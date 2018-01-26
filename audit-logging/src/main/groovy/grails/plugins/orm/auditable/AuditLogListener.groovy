/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package grails.plugins.orm.auditable

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.timestamp.DefaultTimestampProvider
import org.grails.datastore.gorm.timestamp.TimestampProvider
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PreDeleteEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.context.ApplicationEvent

import static grails.plugins.orm.auditable.AuditLogListenerUtil.*

/**
 * Grails interceptor for logging saves, updates, deletes and acting on
 * individual properties changes and delegating calls back to the Domain Class
 */
@Slf4j
@CompileStatic
class AuditLogListener extends AbstractPersistenceEventListener {
    GrailsApplication grailsApplication

    // TODO - is this a bean, can it just be loaded at runtime?
    private TimestampProvider timestampProvider = new DefaultTimestampProvider()

    AuditLogListener(Datastore datastore) {
        super(datastore)
    }

    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        if (event.source != datastore) {
            log.trace("Event received for datastore {}, ignoring", event.source)
            return
        }
        if (!(event.entityObject instanceof Auditable)) {
            return
        }

        // Logging is globally disabled
        if (!AuditLoggingConfigUtils.auditConfig.getProperty("enabled")) {
            return
        }

        // Logging is disabled for this transaction (technically this thread)
        if (AuditLogListenerState.getAuditLogDisabled()) {
            return
        }

        try {
            AuditEventType auditEventType = AuditEventType.forEventType(event.eventType)
            Auditable domain = event.entityObject as Auditable

            if (domain.isAuditLogEnabled(auditEventType)) {
                log.trace("Audit logging: Event {} for object {}", auditEventType, event.entityObject.class.name)

                switch (event.eventType) {
                    case EventType.PostInsert:
                    case EventType.PreDelete:
                        handleInsertAndDelete(domain, auditEventType)
                        break
                    case EventType.PreUpdate:
                        handleUpdate(domain, auditEventType)
                        break
                }
            }
        }
        catch (Exception e) {
            if (AuditLoggingConfigUtils.auditConfig.getProperty('failOnError')) {
                throw e
            }
            else {
                log.error("Error creating audit log for event ${event.eventType} and domain ${event.entityObject}", e)
            }
        }
    }

    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return eventType.isAssignableFrom(PostInsertEvent) ||
            eventType.isAssignableFrom(PreUpdateEvent) ||
            eventType.isAssignableFrom(PreDeleteEvent)
    }

    /**
     * @param domain the domain instance
     * @return set of propert names eligible for logging
     */
    protected Set<String> resolveLoggedProperties(Auditable domain) {
        PersistentEntity entity = grailsApplication.mappingContext.getPersistentEntity(domain.class.name)

        // Start with all persistent properties
        Set<String> loggedProperties = entity.getPersistentProperties()*.name as Set<String>

        // Intersect with any that are specifically whitelisted
        if (domain.logIncluded) {
            loggedProperties = loggedProperties.intersect(domain.logIncluded as Iterable) as Set<String>
        }

        // Finally exclude anything specifically excluded
        if (domain.logExcluded) {
            loggedProperties -= domain.logExcluded
        }

        loggedProperties
    }

    /**
     * We must use the preDelete event if we want to capture what the old object was like.
     */
    protected void handleInsertAndDelete(Auditable domain, AuditEventType auditEventType) {
        if (domain.logIgnoreEvents?.contains(auditEventType)) {
            return
        }

        Map<String, Object> map = [:]

        // If verbose logging, resolve properties
        if (!AuditLogListenerState.getAuditLogNonVerbose() && domain.logVerbose?.contains(auditEventType)) {
            Set<String> loggedProperties = resolveLoggedProperties(domain)
            if (loggedProperties) {
                map = makeMap(loggedProperties, domain)
            }
        }

        logChanges(domain, map, null, auditEventType)
    }

    /**
     * Look at persistent and transient state for the object and log differences
     */
    protected void handleUpdate(Auditable domain, AuditEventType auditEventType) {
        if (domain.logIgnoreEvents.contains("onChange")) {
            return
        }

        // By default, we don't log verbose properties
        Map newMap = [:]
        Map oldMap = [:]

        // If verbose, resolve properties
        if (!AuditLogListenerState.getAuditLogNonVerbose() && domain.logVerbose?.contains(auditEventType)) {
            Set<String> dirtyProperties = domain.getDirtyPropertyNames() as Set<String>
            Set<String> loggedProperties = resolveLoggedProperties(domain)

            // Needs to be dirty properties and properties to log, otherwise it won't be verbose
            if (dirtyProperties && loggedProperties) {

                // Intersect the two to get the final set of properties that we care about
                loggedProperties = loggedProperties.intersect(dirtyProperties as Iterable)
                if (loggedProperties) {

                    // Get the prior values for everything that is dirty
                    oldMap = loggedProperties.collectEntries { String property -> [property, getPersistentValue(domain, property)] }

                    // Get the current values for everything that is dirty
                    newMap = makeMap(loggedProperties, domain)
                }
            }
        }

        logChanges(domain, newMap, oldMap, auditEventType)
    }

    /**
     * Do the actual logging of changes
     *
     * @param domain the thing triggering the audit
     * @param newMap for insert and delete, holds the current values filtered to what we care about
     * @param oldMap null for insert and delete, for updates holds the original persisted values filtered to what we care about
     * @param eventType the type of event we are logging
     */
    protected void logChanges(Auditable domain, Map<String, Object> newMap, Map<String, Object> oldMap, AuditEventType eventType) {
        log.debug("Audit logging event {} and domain {}", eventType, domain.getClass().name)

        // Wrap all of the logging in a single session to prevent flushing for each insert
        getAuditDomainClass().invokeMethod("withNewSession") {
            Long persistedObjectVersion = getPersistedObjectVersion(domain, newMap, oldMap)

            // This handles insert, delete, and update with any property level logging enabled
            if (newMap) {
                newMap.each { String propertyName, Object newVal ->
                    String newValueAsString = conditionallyMaskAndTruncate(domain, propertyName, domain.logPropertyToString(propertyName, newVal))
                    String oldValueAsString = null

                    // This indicates a change
                    if (oldMap) {
                        Object oldVal = oldMap[propertyName]
                        if (newVal != oldVal) {
                            oldValueAsString = conditionallyMaskAndTruncate(domain, propertyName, domain.logPropertyToString(propertyName, oldVal))
                        }
                    }

                    // Create a new entity for each property
                    GormEntity audit = createAuditLogDomainInstance(
                        actor: domain.logCurrentUserName, uri: domain.logURI, className: domain.logClassName, eventName: eventType.name(),
                        persistedObjectId: domain.logEntityId, persistedObjectVersion: persistedObjectVersion,
                        propertyName: propertyName, oldValue: oldValueAsString, newValue: newValueAsString,
                    )
                    if (domain.beforeSaveLog(audit)) {
                        audit.save(failOnError: true)
                    }
                }
            }
            else {
                // Create a single entity for this event
                GormEntity audit = createAuditLogDomainInstance(actor: domain.logCurrentUserName, uri: domain.logURI, className: domain.logClassName, eventName: eventType.name(), persistedObjectId: domain.logEntityId, persistedObjectVersion: persistedObjectVersion)
                if (domain.beforeSaveLog(audit)) {
                    audit.save(failOnError: true)
                }
            }
        }
    }

    protected Long getPersistedObjectVersion(Auditable domain, Map<String, Object> newMap, Map<String, Object> oldMap) {
        def persistedObjectVersion = (newMap?.version ?: oldMap?.version)
        if (!persistedObjectVersion && domain.hasProperty("version")) {
            persistedObjectVersion = domain.metaClass.getProperty(domain, "version")
        }
        persistedObjectVersion as Long
    }

    /**
     * Disable verbose audit logging for anything within this block
     */
    static withoutVerboseAuditLog(Closure c) {
        AuditLogListenerState.auditLogNonVerbose = true
        try {
            c.call()
        }
        finally {
            AuditLogListenerState.clearAuditLogNonVerbose()
        }
    }

    /**
     * Disable audit logging for this block
     */
    static withoutAuditLog(Closure c) {
        AuditLogListenerState.auditLogDisabled = true
        try {
            c.call()
        }
        finally {
            AuditLogListenerState.clearAuditLogDisabled()
        }
    }
}
