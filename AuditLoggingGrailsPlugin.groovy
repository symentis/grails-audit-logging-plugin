import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil
import org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import javax.servlet.http.HttpSession
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogConfig
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditEventHandler
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditableRegistry

/**
 * @author Shawn Hartsock
 * <p/>
 * Credit is due to the following other projects,
 * first is Kevin Burke's HibernateEventsGrailsPlugin
 * second is the AuditLogging post by Rob Monie at
 * http://www.hibernate.org/318.html
 * <p/>
 * I've combined the two sources to create a Grails
 * Audit Logging plugin that will track individual
 * changes to columns.
 * <p/>
 * See Documentation:
 * http://grails.org/plugin/audit-logging
 * <p/>
 * Changes:
 * <p/>
 * Release 0.3
 * <p/>
 *      * actorKey and username features allow for the logging of
 *        user or userPrincipal for most security systems.
 * <p/>
 *
 * Release 0.4
 * <p/>
 * 		* custom serializable implementation for AuditLogEvent so events can happen
 *        inside a webflow context.
 * <p/>
 *      * tweak application.properties for loading in other grails versions
 * <p/>
 *      * update to views to show URI in an event
 * <p/>
 *      * fix missing oldState bug in change event
 * <p/>
 *
 * Release 0.4.1
 * <p/>
 *      * repackaged for Grails 1.1.1 see GRAILSPLUGINS-1181
 * <p/>
 *
 * Release 0.5_ALPHA see GRAILSPLUGINS-391
 * <p/>
 *      * changes to AuditLogEvent domain object uses composite id to simplify logging
 * <p/>
 *      * changes to AuditLogListener uses new domain model with separate transaction
 *        for logging action to avoid invalidating the main hibernate session.
 * <p/>
 * Release 0.5_BETA see GRAILSPLUGINS-391
 * <p/>
 *      * testing version released generally.
 * <p/>
 * Release 0.5 see GRAILSPLUGINS-391, GRAILSPLUGINS-1496, GRAILSPLUGINS-1181, GRAILSPLUGINS-1515, GRAILSPLUGINS-1811
 * <p/>
 * Release 0.5.1 fixes regression in field logging
 * <p/>
 * Release 0.5.2 see GRAILSPLUGINS-1887 and GRAILSPLUGINS-1354
 * <p/>
 * Release 0.5.3 GRAILSPLUGINS-2135 GRAILSPLUGINS-2060 &amp; an issue with extra JAR files that are somehow getting released as part of the plugin
 * <p/>
 * Release 0.5.4 compatibility issues with Grails 1.3.x
 * <p/>
 * Release 1.0.0
 * <p/>
 */
class AuditLoggingGrailsPlugin {
    def version = "1.0.0-SNAPSHOT"
    def grailsVersion = '2.0 > *'
    def author = "Shawn Hartsock"
    def authorEmail = "hartsock@acm.org"
    def title = "adds auditable to GORM domain classes"
    def description = """ Automatically log change events for domain objects.
The Audit Logging plugin adds an instance hook to domain objects that allows you to hang
Audit events off of them. The events include onSave, onUpdate, onChange, onDelete and
when called the event handlers have access to oldObj and newObj definitions that
will allow you to take action on what has changed.

Stable Releases:
    0.5.3 (Grails 1.2 or below)
    0.5.4 (Grails 1.3 or above)
    1.0.0 (Grails 2.0 or above)
    """
    def dependsOn = [:]
    def loadAfter = ['core', 'hibernate']

    def doWithSpring = {

        auditableRegistry(AuditableRegistry)

        if (manager?.hasGrailsPlugin("hibernate")) {
            auditEventListenerConfig(AuditLogConfig) {
                sessionFactory = sessionFactory
                auditableRegistry = auditableRegistry
                verbose = application.config?.auditLog?.verbose ?: false
                transactional = application.config?.auditLog?.transactional ?: false
                // users may specify their own closure for this job
                actorClosure = {->
                    def actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
                        def actor = null
                        if(request.remoteUser) {
                            actor = request.remoteUser
                        }

                        if(!actor && request.userPrincipal) {
                            actor = request.userPrincipal.getName()
                        }
                        return actor
                    }

                    if (application.config?.auditLog?.sessionAttribute) {
                        actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
                            log.debug "configured with session attribute ${delegate.sessionAttribute} attempting to resolve"
                            session.getAttribute(application.config?.auditLog?.sessionAttribute)
                        }
                    }
                    else if (application.config?.auditLog?.actorKey) {
                        actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
                            log.debug "configured with actorKey ${actorKey} resolve using request attributes "
                            AuditLogListenerUtil.resolve(request.getAttributes(),application.config?.auditLog?.actorKey,delegate.log)
                        }
                    }
                    else if (application.config?.auditLog?.actorClosure) {
                        actorDefaultGetter = application.config?.auditLog?.actorClosure
                    }
                    actorDefaultGetter
                }
            }


            def auditEventHandlers = new LinkedList<AuditEventHandler>()
            // TODO stuff... and things...

            Class listenerClass = application.config?.auditLog?.listenerClass ?: AuditLogListener
            auditLogListener(listenerClass) {
                configuration = auditEventListenerConfig
                auditEventHandlers = auditEventHandlers
            }

            //PreDeleteEventListener, PostInsertEventListener, PostUpdateEventListener
            hibernateEventListeners(HibernateEventListeners) {
                listenerMap = [
                        'post-insert': auditLogListener,
                        'post-update': auditLogListener,
                        'pre-delete': auditLogListener
                ]
            }
        }
        else {
            log.error("Audit Logging Plugin only implements Hibernate!")
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // pulls in the bean to inject and init
        AuditLogListener listener = applicationContext.getBean("auditLogListener")
        listener.init()
        // allows user to over-ride the maximum length the value stored by the audit logger.
        if (application.config?.auditLog?.TRUNCATE_LENGTH) {
            listener.truncateLength = new Long(application.config?.auditLog?.TRUNCATE_LENGTH)
        }

        AuditableRegistry auditableRegistry = applicationContext.getBean("auditableRegsitry")
        for (Class dc in application.domainClasses) {
            if( dc.properties.containsKey('auditable') ) {
                auditableRegistry.register(dc,dc.properties['auditable'])
            }
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
