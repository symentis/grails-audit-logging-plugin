package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 1/23/12
 * Time: 8:33 PM
 * To change this template use File | Settings | File Templates.
 */
class AuditLogConfig implements AuditEventListenerConfig {
    boolean verbose
    boolean transactional
    Closure actorClosure
}
