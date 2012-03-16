package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 10:32 PM
 * To change this template use File | Settings | File Templates.
 */
class AuditEventLogHandler implements AuditEventHandler {
    AuditEventListener listener

    void setListener(AuditEventListener listener) {
        this.listener = listener;
    }

    @Override
    boolean handle(AuditEvent event) {
        return false  //To change body of implemented methods use File | Settings | File Templates.
    }
}
