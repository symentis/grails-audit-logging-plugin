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

import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil
import org.grails.datastore.mapping.core.Datastore

/**
 * @author Robert Oschwald
 * @author Aaron Long
 * @author Shawn Hartsock
 *
 * Credit is due to the following other projects,
 * first is Kevin Burke's HibernateEventsGrailsPlugin
 * second is the AuditLogging post by Rob Monie at
 * http://www.hibernate.org/318.html
 *
 * Combined the two sources to create a Grails
 * Audit Logging plugin that will track individual
 * changes to columns.
 *
 * See Documentation:
 * http://grails.org/plugin/audit-logging
 *
 * Changes:
 * Release 0.3   actorKey and username features allow for the logging of
 *               user or userPrincipal for most security systems.
 * Release 0.4   custom serializable implementation for AuditLogEvent so events can happen
 *               inside a webflow context.
 *               tweak application.properties for loading in other grails versions
 *               update to views to show URI in an event
 *               fix missing oldState bug in change event
 * Release 0.4.1 repackaged for Grails 1.1.1 see GRAILSPLUGINS-1181
 * Release 0.5_ALPHA see GRAILSPLUGINS-391
 *               changes to AuditLogEvent domain object uses composite id to simplify logging
 *               changes to AuditLogListener uses new domain model with separate transaction
 *               for logging action to avoid invalidating the main hibernate session.
 * Release 0.5_BETA see GRAILSPLUGINS-391
 *               testing version released generally.
 * Release 0.5     GRAILSPLUGINS-391, GRAILSPLUGINS-1496, GRAILSPLUGINS-1181, GRAILSPLUGINS-1515, GRAILSPLUGINS-1811
 * Release 0.5.1   fixes regression in field logging
 * Release 0.5.2   GRAILSPLUGINS-1887 and GRAILSPLUGINS-1354
 * Release 0.5.3   GRAILSPLUGINS-2135 GRAILSPLUGINS-2060 && an issue with extra JAR files that are somehow getting released as part of the plugin
 * Release 0.5.4   compatibility issues with Grails 1.3.x
 * Release 0.5.5   collections logging, log ids, replacement patterns, property value masking, large fields support, fixes and enhancements
 * Release 0.5.5.1 Fixed the title. No changes in the plugin code.
 * Release 0.5.5.2 Added issueManagement to plugin descriptor for the portal. No changes in the plugin code.
 * Release 0.5.5.3 Added ability to disable audit logging by config.
 * Release 1.0.0 Grails >= 2.0 ORM agnostic implementation, major cleanup and new features
 * Release 1.0.1 closures, nonVerboseDelete property, provide domain identifier to onSave() handler
 * Release 1.0.2 GPAUDITLOGGING-66
 * Release 1.0.3 GPAUDITLOGGING-64 workaround for duplicate log entries written per configured dataSource
 *               GPAUDITLOGGING-63 logFullClassName property
 * Release 1.0.4 GPAUDITLOGGING-69 allow to set uri per domain object
 *               GPAUDITLOGGING-62 Add identifier in handler map
 *               GPAUDITLOGGING-29 support configurable id mapping for AuditLogEvent
 *               GPAUDITLOGGING-70 support configurable datasource name for AuditLogEvent
 *               GPAUDITLOGGING-74 Impossible to log values of zero or false
 *               GPAUDITLOGGING-75 Support automatic (audit) stamping support on entities
 *               static auditable = [ignoreEvents:["onChange","onSave"]]
 * Release 1.0.5 Support for ignoring certain Events (#92)
 * Release 1.0.6 Compile fails with mongoDB plugin (#91)
 *               Removed grails-hibernate EventTriggeringInterceptor dependency from Plugin descriptor to be ORM agnostic.
 *               Minimum Grails version raised to 2.1 due to Datastore limitations in applicationContext
 *               fix #93 Use MongoDB as datasource in a multiple-datasource configuration
 *               merge PR #96 Make identifiers available in the maps during onChange event
 *               fix #99 Plugin not working with MongoDB as Only Database
 *               fix #100 Id generation default for AuditLogEvent should align with GORM default
 *               Changed issue management url to GH.
 * Release 1.0.7 fix #106 Enforce text type usage in largeColumValues mode
 * 
 */
class AuditLoggingGrailsPlugin {
    def version = "1.0.7"
    def grailsVersion = '2.1 > *'
    def title = "Audit Logging Plugin"
    def authorEmail = "roos@symentis.com"
    def description = """ Automatically log change events for domain objects.
The Audit Logging plugin additionally adds an instance hook to domain objects that allows you to hang Audit events off of them.
The events include onSave, onChange, and onDelete.
When called, the event handlers have access to oldObj and newObj definitions that will allow you to take action on what has changed.
    """

    def documentation = 'http://grails.org/plugin/audit-logging'
    def license = 'APACHE'
    def organization = [name: "symentis GmbH", url: "http://www.symentis.com/"]
    def developers = [
        [ name: 'Robert Oschwald', email: 'roos@symentis.com' ],
        [ name: 'Elmar Kretzer', email: 'elkr@symentis.com' ],
        [ name: 'Aaron Long', email: 'longwa@gmail.com' ]
    ]
    def issueManagement = [system: 'GitHub', url: 'https://github.com/robertoschwald/grails-audit-logging-plugin/issues']
    def scm = [url: 'https://github.com/robertoschwald/grails-audit-logging-plugin']
    def dependsOn = [:]
    def loadAfter = ['core', 'dataSource']

    // Register generic GORM listener
    def doWithApplicationContext = { applicationContext ->
        // due to next line, Grails 2.0 is not supported anymore.
        // We need to obtain all datastores in ORM agnostic way, but in Grails 2.0.x, the DataStore is not obtainable from ctx.
        applicationContext.getBeansOfType(Datastore).values().each { Datastore datastore ->
            // Don't register the listener if we are disabled
            log.debug("Registering AuditLogListeners to datastores")
            // Note: Some datastores do not hold config property (e.g. mongodb)
            boolean dataStoreDisabled = datastore.hasProperty("config") ? datastore.config.auditLog.disabled : false
            if (!application.config.auditLog.disabled && !dataStoreDisabled) {
                log.debug("Registering AuditLogListeners to datastore $datastore")
                def listener = new AuditLogListener(datastore)
                listener.with {
                    grailsApplication = application
                    stampEnabled = application.config.auditLog.stampEnabled ?: true
                    stampAlways = application.config.auditLog.stampAlways ?: false
                    stampCreatedBy = application.config.auditLog.stampCreatedBy ?: 'createdBy'
                    stampLastUpdatedBy = application.config.auditLog.stampLastUpdatedBy ?: 'lastUpdatedBy'
                    verbose = application.config.auditLog.verbose ?: false
                    nonVerboseDelete = application.config.auditLog.nonVerboseDelete ?: false
                    logFullClassName = application.config.auditLog.logFullClassName ?: false
                    transactional = application.config.auditLog.transactional ?: false
                    sessionAttribute = application.config.auditLog.sessionAttribute ?: ""
                    actorKey = application.config.auditLog.actorKey ?: ""
                    truncateLength = application.config.auditLog.truncateLength ?: determineDefaultTruncateLength()
                    actorKey = application.config.auditLog.actorKey ?: ""
                    actorClosure = application.config.auditLog.actorClosure ?: AuditLogListenerUtil.actorDefaultGetter
                    defaultIgnoreList = application.config.auditLog.defaultIgnore?.asImmutable() ?: ['version', 'lastUpdated'].asImmutable()
                    defaultMaskList = application.config.auditLog.defaultMask?.asImmutable() ?: ['password'].asImmutable()
                    propertyMask = application.config.auditLog.propertyMask ?: "**********"
                    replacementPatterns = application.config.auditLog.replacementPatterns
                    logIds = application.config.auditLog.logIds ?: false
                }
                applicationContext.addApplicationListener(listener)
            }
        }
    }

    /**
     * The default truncate length is 255 unless we are using the largeValueColumnTypes, then we allow up to the column size
     */
    private Integer determineDefaultTruncateLength() {
        AuditLogEvent.constraints.oldValue?.maxSize ?: 255
    }
}
