package org.codehaus.groovy.grails.plugins.orm.auditable

import org.springframework.context.ApplicationEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.*
import org.springframework.context.event.SmartApplicationListener
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentEntity;

class DomainEventListenerService implements SmartApplicationListener {
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
                postInsert(event.entity,event.entityAccess)
                break
        }
    }

    void postInsert(PersistentEntity entity, EntityAccess entityAccess)  {
        def newMap = [:]
        entityAccess.entity.properties.each { key, val ->
            newMap[key] = val
        }
        auditLogListener.logChanges(newMap,null,entityAccess.entity,entityAccess.entity.id,'INSERT',entity.javaClass.getName())
    }
}
