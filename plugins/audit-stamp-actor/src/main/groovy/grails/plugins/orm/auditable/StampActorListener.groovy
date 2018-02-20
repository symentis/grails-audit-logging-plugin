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
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.event.*
import org.springframework.context.ApplicationEvent

/**
 * Grails interceptor for logging saves, updates, deletes and acting on
 * individual properties changes and delegating calls back to the Domain Class
 */
@Slf4j
@CompileStatic
class StampActorListener extends AbstractPersistenceEventListener {
    private GrailsApplication grailsApplication
    static final String CREATED_BY = 'createdBy'
    static final String LAST_UPDATED_BY = 'lastUpdatedBy'

    @Lazy AuditRequestResolver requestResolver = (AuditRequestResolver){grailsApplication.mainContext.getBean('auditRequestResolver')}()

    StampActorListener(Datastore datastore, GrailsApplication grailsApplication) {
        super(datastore)
        this.grailsApplication = grailsApplication
    }

    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        if (event.source != datastore) {
            log.trace("Event received for datastore {}, ignoring", event.source)
            return
        }
        if (!(event.entityObject instanceof StampActor)) {
            return
        }

        // See if we are disabled for this context
        if (!grailsApplication.config.getProperty('grails.plugin.auditLog.stampActor.enabled',Boolean.TYPE,true)) {
            return
        }

        try {
            log.trace("Stamping object {}", event.entityObject.class.name)

            switch(event.eventType) {
                case EventType.PreInsert:
                    handleInsert(event.entityAccess, requestResolver)
                    break
                case EventType.PreUpdate:
                    handleUpdate(event.entityAccess, requestResolver)
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
    protected static void handleInsert(EntityAccess entityAccess, AuditRequestResolver requestResolver) {
        // Set actors, Grails will take care of setting the dates
        String currentActor = requestResolver.currentActor
        entityAccess.setProperty(CREATED_BY,currentActor)
        entityAccess.setProperty(LAST_UPDATED_BY,currentActor)
    }

    /**
     * Stamp updates
     */
    protected static void handleUpdate(EntityAccess entityAccess, AuditRequestResolver requestResolver) {
        String currentActor = requestResolver.currentActor
        entityAccess.setProperty(LAST_UPDATED_BY,currentActor)
    }
}
