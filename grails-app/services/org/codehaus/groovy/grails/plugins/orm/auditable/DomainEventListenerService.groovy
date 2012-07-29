package org.codehaus.groovy.grails.plugins.orm.auditable

import org.grails.datastore.mapping.engine.event.PersistenceEventListener
import org.springframework.context.ApplicationEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.*
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext;

class DomainEventListenerService implements PersistenceEventListener {
    AuditLogListener auditLogListener

    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        boolean supports = false
        switch(eventType) {
            case PostInsertEvent:
            case PreDeleteEvent:
            case PostUpdateEvent:
                return true
                break;
            default:
                supports = false
                break;

        }
        return supports
    }

    @Override
    boolean supportsSourceType(Class<?> sourceType) {
        return true
    }

    @Override
    int getOrder() {
        Integer.MAX_VALUE // last thing to run
    }

    @Override
    void onApplicationEvent(ApplicationEvent event) {
        if(supportsEventType(event.class)) {
            onPersistenceEvent(event)
        }
    }

    void onPersistenceEvent(AbstractPersistenceEvent event) {
        switch( event.class ) {
            case PostInsertEvent:
                println event
                break
        }
    }
}
