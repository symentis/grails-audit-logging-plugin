import org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil

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
 *      * custom serializable implementation for AuditLogEvent so events can happen
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
 * Release 0.5.5.2 collections logging, log ids, replacement patterns, property value masking, large fields support, fixes and enhancements
 * <p/>
 * Release 1.0.0  T.B.D.
 * <p/>
 */
class AuditLoggingGrailsPlugin {
    def version = "1.0.0-SNAPSHOT"
    def grailsVersion = '2.0 > *'
    def title = "Adds auditable to GORM domain classes"
    def description = """ Automatically log change events for domain objects.
The Audit Logging plugin additionally adds an instance hook to domain objects that allows you to hang
Audit events off of them. The events include onSave, onUpdate, onChange and onDelete.
When called, the event handlers have access to oldObj and newObj definitions that
will allow you to take action on what has changed.

Stable Releases:
    0.5.3 (Grails 1.2 or below)
    0.5.4 (Grails 1.3 or above)
    0.5.5.2 (Grails 1.3 or above)
    1.0.0 (Grails 2.0 or above)
    """
    def loadAfter = ['core', 'hibernate']

    def documentation = 'http://grails.org/plugin/audit-logging'
    def license = 'APACHE'
    def organization = [name: "symentis GmbH", url: "http://www.symentis.com/"]
    def developers = [
        [name: 'Robert Oschwald', email: 'roos@symentis.com']
    ]
    def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPAUDITLOGGING']
    def scm = [url: 'https://github.com/robertoschwald/grails-audit-logging-plugin']

    def doWithSpring = {
        if (manager?.hasGrailsPlugin("hibernate")) {
            auditLogListener(AuditLogListener) {
                sessionFactory = sessionFactory
                verbose = application.config?.auditLog?.verbose ?: false
                transactional = application.config?.auditLog?.transactional ?: false
                sessionAttribute = application.config?.auditLog?.sessionAttribute ?: ""
                actorKey = application.config?.auditLog?.actorKey ?: ""
            }
            //PreDeleteEventListener, PostInsertEventListener, PostUpdateEventListener
            hibernateEventListeners(HibernateEventListeners) {
                listenerMap = [
                        'post-insert': auditLogListener,
                        'post-update': auditLogListener,
                        'pre-delete': auditLogListener,
                        'pre-collection-update': auditLogListener,
                        'pre-collection-remove': auditLogListener,
                        'post-collection-recreate': auditLogListener]
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // pulls in the bean to inject and init
        AuditLogListener listener = applicationContext.auditLogListener
        // allows to configure the Actor name Closure in the config
        listener.setActorClosure(application.config?.auditLog?.actorClosure ?: AuditLogListenerUtil.actorDefaultGetter)
        listener.init()
        // allows user to over-ride the maximum length the value stored by the audit logger.
        if (application.config?.auditLog?.TRUNCATE_LENGTH) {
            listener.truncateLength = Long.valueOf(application.config?.auditLog?.TRUNCATE_LENGTH)
        }
    }
}
