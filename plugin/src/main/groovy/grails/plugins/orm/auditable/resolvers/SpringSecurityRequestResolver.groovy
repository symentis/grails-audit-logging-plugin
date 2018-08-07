package grails.plugins.orm.auditable.resolvers

/**
 * Default resolver that uses the SpringSecurityService principal if available
 */
class SpringSecurityRequestResolver extends DefaultAuditRequestResolver {
    def springSecurityService

    @Override
    String getCurrentActor() {
        springSecurityService?.currentUser?.toString() ?: super.getCurrentActor()
    }
}
