package org.codehaus.groovy.grails.plugins.orm.auditable;

import groovy.lang.Closure;
import org.hibernate.SessionFactory;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 7:49 PM
 *
 * To provide your own
 *                       sessionFactory = sessionFactory
 verbose = application.config?.auditLog?.verbose ?: false
 transactional = application.config?.auditLog?.transactional ?: false
 sessionAttribute = application.config?.auditLog?.sessionAttribute ?: ""
 actorKey = application.config?.auditLog?.actorKey ?: ""

 * */
public interface AuditEventListener extends PreDeleteEventListener, PostInsertEventListener, PostUpdateEventListener, Initializable {
    void setSessionFactory(Object sessionFactory);
    void setConfig(AuditEventListenerConfig config);
}
