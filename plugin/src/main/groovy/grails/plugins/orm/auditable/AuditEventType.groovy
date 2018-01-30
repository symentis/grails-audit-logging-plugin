package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.engine.event.EventType

/**
 * Simple enum for audit events
 */
@CompileStatic
enum AuditEventType {
    INSERT, UPDATE, DELETE

    @Override
    String toString() {
        name()
    }

    static AuditEventType forEventType(EventType type) {
        switch(type) {
            case EventType.PostInsert:
                return INSERT
            case EventType.PreDelete:
                return DELETE
            case EventType.PreUpdate:
                return UPDATE
            default:
                throw new IllegalArgumentException("Unexpected event type $type")
        }
    }
}