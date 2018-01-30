package grails.plugins.orm.auditable.resolvers

import grails.plugin.springsecurity.SpringSecurityService
import groovy.transform.CompileStatic

@CompileStatic
class SpringSecurityRequestResolver extends DefaultAuditRequestResolver {
    SpringSecurityService springSecurityService

    @Override
    String getCurrentActor() {
        springSecurityService?.currentUser?.toString() ?: super.getCurrentActor()
    }
}
