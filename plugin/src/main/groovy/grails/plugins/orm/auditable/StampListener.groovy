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
import grails.plugins.orm.auditable.resolvers.AuditRequestResolver
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.springframework.context.ApplicationEvent

/**
 * Grails interceptor for logging saves, updates, deletes and acting on
 * individual properties changes and delegating calls back to the Domain Class
 */
@Slf4j
@CompileStatic
class StampListener extends AbstractPersistenceEventListener {
    private GrailsApplication grailsApplication

    StampListener(Datastore datastore, GrailsApplication grailsApplication) {
        super(datastore)
        this.grailsApplication = grailsApplication
    }

    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        if (event.source != datastore) {
            log.trace("Event received for datastore {}, ignoring", event.source)
            return
        }
        if (!(event.entityObject instanceof Stampable)) {
            return
        }

        // See if we are disabled for this context
        if (!AuditLogContext.context.stampEnabled) {
            return
        }

        try {
            Stampable domain = event.entityObject as Stampable
            log.trace("Stamping object {}", event.entityObject.class.name)

            // Lookup the request resolver here to ensure that applications have a chance
            // to override this bean to provide different strategies
            AuditRequestResolver requestResolver = grailsApplication.mainContext.getBean(AuditRequestResolver)

            switch(event.eventType) {
                case EventType.PreInsert:
                    handleInsert(domain, requestResolver)
                    break
                case EventType.PreUpdate:
                    handleUpdate(domain, requestResolver)
                    break
            }
        }
        catch (Exception e) {
            if (AuditLogContext.context.failOnError) {
                throw e
            }
            else {
                log.error("Error stamping domain ${event.entityObject}", e)
            }
        }
    }

    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        eventType.isAssignableFrom(PreInsertEvent) || eventType.isAssignableFrom(PreUpdateEvent)
    }

    /**
     * Stamp inserts
     */
    protected void handleInsert(Stampable domain, AuditRequestResolver requestResolver) {
        // Set actors, Grails will take care of setting the dates
        String currentActor = requestResolver.currentActor
        domain.createdBy = currentActor
        domain.lastUpdatedBy = currentActor
    }

    /**
     * Stamp updates
     */
    protected void handleUpdate(Stampable domain, AuditRequestResolver requestResolver) {
        domain.lastUpdatedBy = requestResolver.currentActor
    }
}
