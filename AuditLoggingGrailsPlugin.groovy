import org.codehaus.groovy.grails.commons.ApplicationHolder
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
 */
class AuditLoggingGrailsPlugin {
    def version = "0.4-SNAPSHOT"
    def author = "Shawn Hartsock"
    def authorEmail = "hartsock@acm.org"
    def title = "adds hibernate audit logging and onChange event handlers to GORM domain classes"
    def description = """
The Audit Logging plugin adds an instance hook to domain objects that allows you to hang
Audit events off of them. The events include onSave, onUpdate, onChange, onDelete and
when called the event handlers have access to oldObj and newObj definitions that
will allow you to take action on what has changed.
    """
    def dependsOn = [:]
	
    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }
   
    def doWithApplicationContext = { applicationContext ->
    	def listeners = applicationContext.sessionFactory.eventListeners

    	def listener = new AuditLogListener()
    	def config = ApplicationHolder.application.config.auditLog
    	if(config?.verbose) {
    		listener.verbose = (config?.verbose)?true:false
    	}
    	if(config?.actor) {
    		listener.actorKey = (config?.actor)?:config?.user
    	}
    	if(config?.username) {
    		listener.sessionAttribute = (config?.username)?:null
    	}
    	
    	// use preDelete so we can see what is going to be destroyed
    	// hook to the postInsert to grab the ID of the object
    	// hook to postUpdate to use the old and new state hooks
    	['preDelete','postInsert', 'postUpdate',].each({
    		addEventTypeListener(listeners, listener, it)
    	})
    }
 
    // Brazenly lifted from HibernateEventsGrailsPlugin by Kevin Burke
    def addEventTypeListener(listeners, listener, type) {
        def typeProperty = "${type}EventListeners"
        def typeListeners = listeners."${typeProperty}"
        def expandedTypeListeners = new Object[typeListeners.length + 1]
        System.arraycopy(typeListeners, 0, expandedTypeListeners, 0, typeListeners.length)
        expandedTypeListeners[-1] = listener
        listeners."${typeProperty}" = expandedTypeListeners
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
