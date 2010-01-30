import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import org.hibernate.SessionFactory
import org.hibernate.event.EventListeners
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.support.SpringLobHandlerDetectorFactoryBean
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil

/**
 * Most of this code is brazenly lifted from two sources
 * first is Kevin Burke's HibernateEventsGrailsPlugin
 * second is the AuditLogging post by Rob Monie at
 * http://www.hibernate.org/318.html
 * 
 * I've combined the two sources to create a Grails
 * Audit Logging plugin that will track individual
 * changes to columns.
 * 
 * See Documentation:
 * http://grails.codehaus.org/Grails+Audit+Logging+Plugin
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
 */
class AuditLoggingGrailsPlugin {
    def version = "0.5.2"
    def author = "Shawn Hartsock"
    def authorEmail = "hartsock@acm.org"
    def title = "adds auditable to GORM domain classes"
    def description = """ Automatically log change events for domain objects.
The Audit Logging plugin adds an instance hook to domain objects that allows you to hang
Audit events off of them. The events include onSave, onUpdate, onChange, onDelete and
when called the event handlers have access to oldObj and newObj definitions that
will allow you to take action on what has changed.

Stable Releases:
    0.5.2

    """
    def dependsOn = [:]
    def loadAfter = ['hibernate']

    def doWithSpring = {
      if (manager?.hasGrailsPlugin("hibernate")) {
        auditLogListener(AuditLogListener) {
          sessionFactory = sessionFactory
          verbose          = application.config?.auditLog?.verbose ? true : false
          sessionAttribute = application.config?.auditLog?.sessionAttribute?:""
          actorKey         = application.config?.auditLog?.actorKey?:""
        }
      }
    }
   
    def doWithApplicationContext = { applicationContext ->
      // pulls in the bean to inject and init
      AuditLogListener listener = applicationContext.getBean("auditLogListener")
      // allows user to over-ride the maximum length the value stored by the audit logger.
      listener.setActorClosure( application.config?.auditLog?.actorClosure?:AuditLogListenerUtil.actorDefaultGetter )
      listener.init()
      if(application.config?.auditLog?.TRUNCATE_LENGTH) {
        listener.truncateLength = new Long(application.config?.auditLog?.TRUNCATE_LENGTH)
      }
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }
	                                      
    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }
	
    def onChange = { event ->
        // TODO Implement code that is executed when this class plugin class is changed  
        // the event contains: event.application and event.applicationContext objects
    }
                                                                                  
    def onApplicationChange = { event ->
        // TODO Implement code that is executed when any class in a GrailsApplication changes
        // the event contain: event.source, event.application and event.applicationContext objects
    }
}
