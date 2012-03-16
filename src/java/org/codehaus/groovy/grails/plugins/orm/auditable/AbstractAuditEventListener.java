package org.codehaus.groovy.grails.plugins.orm.auditable;

import groovy.lang.Closure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 11:00 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractAuditEventListener implements AuditEventListener{
    public static final Log log = LogFactory.getLog(AuditEventListener.class);

    AuditableRegistry auditableRegistry;
    public void setAuditableRegistry(AuditableRegistry auditableReg) {
        this.auditableRegistry = auditableReg;
    }

    AuditLogConfig config;
    public void setConfig(AuditLogConfig config) {
        this.config = config;
    }

    List<AuditEventHandler> auditEventHandlers = new LinkedList<AuditEventHandler>();

    public void setAuditEventHandlers(AuditEventHandler handler) {
        handler.setListener(this);
        auditEventHandlers.add(handler);
    }

    public void setAuditEventHandlers(Collection list) {
        for(Object handler : list) {
                setAuditEventHandlers((AuditEventHandler) handler);
        }
    }

    Closure actorClosure;
    void setActorClosure(Closure closure) {
        closure.setDelegate(this);
        closure.setProperty("log",LogFactory.getLog(closure.getThisObject().getClass()));
        this.actorClosure = closure;
    }

}
