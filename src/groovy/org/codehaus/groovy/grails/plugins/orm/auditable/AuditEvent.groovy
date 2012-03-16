package org.codehaus.groovy.grails.plugins.orm.auditable;

import java.util.Map;

/**
 * A simple DTO that carries all the information at the tail of an auditable event.
 * This object is sent back to the registered AuditEventHandlers in order declared.
 * This allows you to perform multiple actions with this information or configure
 * your own audit event handling behaviors.
 */
public class AuditEvent {
    Map newMap
    Map oldMap
    Object parentObject
    Object persistedObjectId
    String eventName
    String className
    String actor
    String uri
}
