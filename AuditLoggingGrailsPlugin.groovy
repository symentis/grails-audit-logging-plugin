
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil

/**
 * @author Shawn Hartsock
 * @author Robert Oschwald
 *
 * Credit is due to the following other projects,
 * first is Kevin Burke's HibernateEventsGrailsPlugin
 * second is the AuditLogging post by Rob Monie at
 * http://www.hibernate.org/318.html
 * 
 * Combined the two sources to create a Grails
 * Audit Logging plugin that will track individual
 * changes to columns.
 * 
 * See Documentation:
 * http://grails.org/plugin/audit-logging
 * 
 * Changes:
 * Release 0.3
 *      * actorKey and username features allow for the logging of
 *        user or userPrincipal for most security systems.
 * 
 * Release 0.4
 * 		* custom serializable implementation for AuditLogEvent so events can happen
 *        inside a webflow context.
 *      * tweak application.properties for loading in other grails versions
 *      * update to views to show URI in an event
 *      * fix missing oldState bug in change event
 *
 * Release 0.4.1
 *      * repackaged for Grails 1.1.1 see GRAILSPLUGINS-1181
 *
 * Release 0.5_ALPHA see GRAILSPLUGINS-391
 *      * changes to AuditLogEvent domain object uses composite id to simplify logging
 *      * changes to AuditLogListener uses new domain model with separate transaction
 *        for logging action to avoid invalidating the main hibernate session.
 * Release 0.5_BETA see GRAILSPLUGINS-391
 *      * testing version released generally.
 * Release 0.5 see GRAILSPLUGINS-391, GRAILSPLUGINS-1496, GRAILSPLUGINS-1181, GRAILSPLUGINS-1515, GRAILSPLUGINS-1811
 * Release 0.5.1 fixes regression in field logging
 * Release 0.5.2 see GRAILSPLUGINS-1887 and GRAILSPLUGINS-1354
 * Release 0.5.3 GRAILSPLUGINS-2135 GRAILSPLUGINS-2060 && an issue with extra JAR files that are somehow getting released as part of the plugin
 * Release 0.5.4 compatibility issues with Grails 1.3.x
 * Release 0.5.5 collections logging, log ids, replacement patterns, property value masking, large fields support, fixes and enhancements
 * Release 0.5.5.1 Fixed the title. No changes in the plugin code.
 * Release 0.5.5.2 Added issueManagement to plugin descriptor for the portal. No changes in the plugin code.
 * Release 0.5.5.3 Added ability to disable audit logging by config.
 */
class AuditLoggingGrailsPlugin {
  def version = "0.5.5.3"
  def grailsVersion = '1.3 > *'
	def title = "Audit Logging Plugin"
	def author = "Robert Oschwald"
	def authorEmail = "roos@symentis.com"
  def description = """ Automatically log change events for domain objects.
The Audit Logging plugin adds an instance hook to domain objects that allows you to hang
Audit events off of them. The events include onSave, onUpdate, onChange and onDelete.
When called the event handlers have access to oldObj and newObj definitions that
will allow you to take action on what has changed.
Stable Releases:
    0.5.3 (Grails 1.2 or below)
    0.5.4 (Grails 1.3 or above)
    0.5.5.2 (Grails 1.3 or above)
    0.5.5.3 (Grails 1.3 or above)
    """

	def license = "APACHE"
	def organization = [name: "symentis", url: "http://www.symentis.com/"]
	def scm = [url: "https://github.com/robertoschwald/grails-audit-logging-plugin/tree/0.5.5.3"]
  def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPAUDITLOGGING']
  def dependsOn = [:]
  def loadAfter = ['core','hibernate']

    def doWithSpring = {
      if (manager?.hasGrailsPlugin("hibernate")) {
        auditLogListener(AuditLogListener) {
					grailsApplication = ref('grailsApplication')
          sessionFactory   = sessionFactory
          verbose          = application.config?.auditLog?.verbose?:false
          transactional    = application.config?.auditLog?.transactional?:false
          sessionAttribute = application.config?.auditLog?.sessionAttribute?:""
          actorKey         = application.config?.auditLog?.actorKey?:""
					logIds					 = application.config?.auditLog?.logIds?:false
					replacementPatterns = application.config?.auditLog?.replacementPatterns?:null
					propertyMask		 = application.config?.auditLog?.propertyMask?:""
        }
      }
    }

    def doWithApplicationContext = { applicationContext ->
      // pulls in the bean to inject and init
      AuditLogListener listener = applicationContext.getBean("auditLogListener")
      // allows to configure the Actor name Closure in the config
      listener.setActorClosure(application.config?.auditLog?.actorClosure?:AuditLogListenerUtil.actorDefaultGetter )
      listener.init()
			// allows user to over-ride the maximum length the value stored by the audit logger.
      if(application.config?.auditLog?.TRUNCATE_LENGTH) {
        listener.truncateLength = new Long(application.config?.auditLog?.TRUNCATE_LENGTH)
      }
    }
}
