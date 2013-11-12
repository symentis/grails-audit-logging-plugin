import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener

auditLogListener(AuditLogListener) {
	grailsApplication = ref('grailsApplication')
	sessionFactory    = ref('sessionFactory')
	verbose           = application.config?.auditLog?.verbose?:false
	logIds            = application.config?.auditLog?.logIds?:false
	transactional     = application.config?.auditLog?.transactional?:false
	sessionAttribute  = application.config?.auditLog?.sessionAttribute?:""
	actorKey          = application.config?.auditLog?.actorKey?:""
}
