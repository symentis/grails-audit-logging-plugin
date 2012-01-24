package org.codehaus.groovy.grails.plugins.orm.auditable;

import groovy.lang.Closure;

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AuditEventListenerConfig {
    void setVerbose(boolean verbosity);
    void setTransactional(boolean isTransactionalDatabase);
    void setActorClosure(Closure dynamicClosure);
}
