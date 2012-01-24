package org.codehaus.groovy.grails.plugins.orm.auditable;

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AuditEventHandler {
    void setListener(AuditEventListener listener);
    boolean handle(AuditEvent event);
}
