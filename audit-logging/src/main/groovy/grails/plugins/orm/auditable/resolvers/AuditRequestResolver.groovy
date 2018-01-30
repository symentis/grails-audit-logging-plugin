package grails.plugins.orm.auditable.resolvers

import groovy.transform.CompileStatic

@CompileStatic
interface AuditRequestResolver {
    /**
     * @return the current actor
     */
    String getCurrentActor()

    /**
     * @return the current request URI or null if no active request
     */
    String getCurrentURI()
}
