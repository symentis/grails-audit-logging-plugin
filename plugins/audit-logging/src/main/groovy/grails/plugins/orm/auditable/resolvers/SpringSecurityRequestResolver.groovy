package grails.plugins.orm.auditable.resolvers

import groovy.transform.CompileStatic
import org.springframework.security.core.context.SecurityContextHolder

@CompileStatic
class SpringSecurityRequestResolver extends DefaultAuditRequestResolver {

    @Override
    String getCurrentActor() {
        SecurityContextHolder.context?.authentication?.name
    }
}
