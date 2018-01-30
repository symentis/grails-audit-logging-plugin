package grails.plugins.orm.auditable.resolvers

import grails.plugins.orm.auditable.AuditLogContext
import groovy.transform.CompileStatic
import org.grails.web.servlet.mvc.GrailsWebRequest

@CompileStatic
class DefaultAuditRequestResolver implements AuditRequestResolver {
    @Override
    String getCurrentActor() {
        GrailsWebRequest request = GrailsWebRequest.lookup()
        request?.userPrincipal?.name ?: AuditLogContext.context.defaultActor ?: 'N/A'
    }

    @Override
    String getCurrentURI() {
        GrailsWebRequest request = GrailsWebRequest.lookup()
        request?.request?.requestURI
    }
}
