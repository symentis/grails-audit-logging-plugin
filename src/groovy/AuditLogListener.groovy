/**
 * @author shawn hartsock
 * 
 * Grails Hibernate Interceptor for logging
 * saves, updates, deletes and acting on
 * individual properties changes... and
 * delegating calls back to the Domain Class
 * 
 * This class is based on the post at
 * http://www.hibernate.org/318.html
 * 
 * 2008-04-17 created initial version 0.1
 * 2008-04-21 changes for version 0.2 include simpler events
 *  ... config file ... removed 'onUpdate' event.
 * 2008-06-04 added ignore fields feature
 */

import org.hibernate.EntityMode;
import org.hibernate.StatelessSession;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.Initializable;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.springframework.web.context.request.RequestContextHolder;

public class AuditLogListener implements PreDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener, Initializable {
	/**
	 * The verbose flag flips on and off column by column change logging in
	 * insert and delete events. If this is true then all columns are logged
	 * each as an individual event.
	 */
	static verbose = false // in Config.groovy auditLog.verbose = true
	/**
	 * Override the actorKey by specifying in Config.groovy
	 * 
	 * auditLog {
	 *     actor = "userPrincipal.name"
	 * }
	 * 
	 * ... or try
	 * 
	 * auditLog {
	 *     actor = "user.id"
	 * }
	 * 
	 */
	static String actorKey = 'userPrincipal.name' // session.user.name
	static String sessionAttribute = null
	
	def getUri() {
		def attr = RequestContextHolder?.getRequestAttributes()
	    return (attr?.currentRequest?.uri?.toString())?:null
	}

	def getActor() {
		def attr = RequestContextHolder?.getRequestAttributes()
		def actor = null
		if(sessionAttribute) {
			actor = attr?.session?.getAttribute(sessionAttribute)
		}
		else {
			actor = resolve(attr,actorKey) 
		}
		return actor
	}

	def resolve(attr, str) {
	    def tokens = str?.split("\\.")
	    def res = attr
	    tokens.each({

	    	try {
	    		if(res) res = res."${it}"
	    	} catch (groovy.lang.MissingPropertyException mpe) {
	            mpe.printStackTrace()
	            res = null
	        }

	    })
	    return (res)?res.toString():null
	}
	
	// returns true for auditable entities.
	def isAuditableEntity(event) {
		if(event && event.getEntity()) {
			def entity = event.getEntity()
			if(entity.metaClass.hasProperty(entity,'auditable') && entity.'auditable') {
				return true
			}
		}
		return false
	}
	
	/**
	 *  we allow user's to specify
	 *       static auditable = [handlersOnly:true]
	 *  if they don't want us to log events for them and instead have their own plan.
	 */
	boolean callHandlersOnly(entity) {
		if(entity && entity.metaClass.hasProperty(entity,'auditable') && entity.'auditable') {
			if(entity.auditable.getClass() == java.util.LinkedHashMap.class && 
					entity.auditable.containsKey('handlersOnly')) {
				return (entity.auditable['handlersOnly'])?true:false
			}
			return false
		}
		return false
	}
	
	boolean ignoreList(entity) {
		if(entity && entity.metaClass.hasProperty(entity,'auditable') && entity.'auditable') {
			if(entity.auditable.getClass() == java.util.LinkedHashMap.class && 
					entity.auditable.containsKey('ignore')) {
				return entity.auditable[ignore]
			}
			return ['version']
		}
		return []
	}
	
	def getEntityId(event) {
		if(event && event.entity) {
			def entity = event.getEntity()
			if(entity.metaClass.hasProperty(entity,'id') && entity.'id') {
				return entity.id.toString()
			}
			else {
				// trying complex entity resolution
				return event.getPersister().hasIdentifierProperty() ? event.getPersister().getIdentifier(event.getEntity(), event.getPersister().guessEntityMode(event.getEntity())) : null;
			}
		}
		return null
	}
	
	/**
	 * We must use the preDelete event if we want to capture
	 * what the old object was like.
	 */
	public boolean onPreDelete(final PreDeleteEvent event) {
		try {
			def audit = isAuditableEntity(event) 
			String[] names = event.getPersister().getPropertyNames()
			Object[] state = event.getDeletedState()
			def map = makeMap(names,state)
			if(audit && ! callHandlersOnly(event.getEntity())) {
				def entity = event.getEntity()
				def entityName = entity.getClass().getName()
				def entityId = getEntityId(event)
				logChanges(null,map,null,entityId,'DELETE',entityName)
			}
			if(audit) {
				executeHandler(event,'onDelete', map, null)
			}
		} catch (HibernateException e) {
			println "Audit Plugin unable to process DELETE event"
			e.printStackTrace()
		}
		return false
	}

	/**
	 * I'm using the post insert event here instead of the pre-insert
	 * event so that I can log the id of the new entity after it
	 * is saved. That does mean the the insert event handlers
	 * can't work the way we want... but really it's the onChange
	 * event handler that does the magic for us.
	 */
	public void onPostInsert(final PostInsertEvent event) {
		try {
			def audit = isAuditableEntity(event) 
			String[] names = event.getPersister().getPropertyNames()
			Object[] state = event.getState()
			def map = makeMap(names,state)
			if(audit && ! callHandlersOnly(event.getEntity())) {
				def entity = event.getEntity()
				def entityName = entity.getClass().getName()
				def entityId = getEntityId(event)
				logChanges(map,null,null,entityId,'INSERT',entityName)
			}
			if(audit) {
				executeHandler(event,'onSave', null, map)
			}
		} catch (HibernateException e) {
			println "Audit Plugin unable to process INSERT event"
			e.printStackTrace()
		}
		return;
	}

	private def makeMap(String[] names, Object[] state) {
		def map = [:]
		for(int ii=0; ii<names.length; ii++){
			map[names[ii]] = state[ii]
		}
		return map
	}
	
	/**
	 * Now we get fancy. Here we want to log changes...
	 * specifically we want to know what property changed,
	 * when it changed. And what the differences were.
	 * 
	 * This works better from the onPreUpdate event handler
	 * but for some reason it won't execute right for me.
	 * Instead I'm doing a rather complex mapping to build
	 * a pair of state HashMaps that represent the old and
	 * new states of the object.
	 * 
	 * The old and new states are passed to the object's
	 * 'onChange' event handler. So that a user can work
	 * with both sets of values.
	 * 
	 * Needs complex type testing BTW.
	 */
	public void onPostUpdate(final PostUpdateEvent event) {
		if(isAuditableEntity(event)) {
			onChange(event)
		}
	}

	/**
	 * Prevent infinate loops of change logging by trapping
	 * non-significant changes. Occasionally you can set up
	 * a change handler that will create a "trivial" object
	 * change that you don't want to trigger another change
	 * event. So this feature uses the ignore parameter
	 * to provide a list of fields for onChange to ignore.
	 */
	private boolean significantChange(entity,oldMap,newMap) {
		def ignore = ignoreList(entity)
		if(ignore?.size()) {
			ignore.each({ key ->
				oldMap.remove(key)
				newMap.remove(key)
			})
		}
		boolean changed = false
		oldMap.each({ k,v ->
			if(v != newMap[k]) {
				changed = true
			}
		})
		return changed
	}
	
	/**
	 * only if this is an auditable entity
	 */
	private void onChange(final PostUpdateEvent event) {
		def entity = event.getEntity()
		def entityName = entity.getClass().getName()
		def entityId = event.getId()
		
		// object arrays representing the old and new state
		def oldState = event.getOldState()
		def newState = event.getState()
		
		def nameMap = event.getPersister().getPropertyNames()
		def oldMap = [:]
		def newMap = [:]
		
		if(nameMap) {
			for(int ii=0; ii<newState.length; ii++) {
				if(nameMap[ii]) {
					if(oldState) oldMap[nameMap[ii]] = oldState[ii]
					if(newState) newMap[nameMap[ii]] = newState[ii]
				}
			}
		}
		
		if(!significantChange(entity,oldMap,newMap)) {
			return
		}
		
		// allow user's to over-ride whether you do auditing for them.
		if(! callHandlersOnly(event.getEntity())) {
			logChanges(newMap,oldMap,event,entityId,'UPDATE',entityName)
		}
		/**
		 * Hibernate only saves changes when there are changes.
		 * That means the onUpdate event was the same as the
		 * onChange with no parameters. I've eliminated onUpdate.
		 */
		executeHandler(event,'onChange',oldMap,newMap)
		return
	}
	
	public void initialize(final Configuration config) {
		// TODO Auto-generated method stub
		return
	}

	/**
	 * Leans heavily on the "toString()" of a property
	 * ... this feels crufty... should be tighter...
	 */
	def logChanges(newMap,oldMap,parentObject,persistedObjectId,eventName,className) {
		def persistedObjectVersion = (newMap?.version)?:oldMap?.version
		if(newMap) newMap.remove('version')
		if(oldMap) oldMap.remove('version')
		if(newMap && oldMap) {
			newMap.each({key,val->
					if(val != oldMap[key]) {
						def audit = new AuditLogEvent(
							actor:this.getActor(),
							uri: this.getUri(),
							className:className,
							eventName:eventName,
							persistedObjectId:persistedObjectId.toString(),
							persistedObjectVersion:persistedObjectVersion.toString(),
							propertyName:key,
							oldValue:truncate(oldMap[key]),
							newValue:truncate(newMap[key]),
						)
						if(!audit.save()) {
							println audit.errors
						}
					}
			})
		}
		else if(newMap && verbose) {
			newMap.each({key,val->
					def audit = new AuditLogEvent(
							actor:this.getActor(),
							uri: this.getUri(),
							className:className,
							eventName:eventName,
							persistedObjectId:persistedObjectId.toString(),
							persistedObjectVersion:persistedObjectVersion.toString(),
							propertyName:key,
							oldValue:null,
							newValue:truncate(val),
						)
					if(!audit.save()) {
						println audit.errors
					}
			})
		}
		else if(oldMap && verbose) {
			oldMap.each({key,val->
					def audit = new AuditLogEvent(
							actor:this.getActor(),
							uri: this.getUri(),
							className:className,
							eventName:eventName,
							persistedObjectId:persistedObjectId.toString(),
							persistedObjectVersion:persistedObjectVersion.toString(),
							propertyName:key,
							oldValue:truncate(val),
							newValue:null
						)
					if(!audit.save()) {
						println audit.errors
					}
			})
		}
		else {
			def audit = new AuditLogEvent(
					actor:this.getActor(),
					uri: this.getUri(),
					className:className,
					eventName:eventName,
					persistedObjectId:persistedObjectId.toString(),
					persistedObjectVersion:persistedObjectVersion.toString()
				)
			if(!audit.save()) {
				// TODO don't use println...
				println audit.errors
			}
		}
		return
	}
	
	String truncate(final obj,max=255) {
		def str = obj.toString().trim()
		return (str.length()>max)?str.substring(0,max):str
	}
	
	/**
	 * This calls the handlers based on what was passed in to it.
	 */	
	def executeHandler(event,handler,oldState,newState) {
		def entity = event.getEntity()
        if(isAuditableEntity(event) && entity.metaClass.hasProperty(entity,handler)) {
        		if(oldState && newState) {
            		if(entity."${handler}".maximumNumberOfParameters == 2) {
            			entity."${handler}"(oldState,newState)
            		} 
            		else {
            			entity."${handler}"([oldMap:oldState,newMap:newState])
            		}
        		}
        		else if(oldState) {
        			entity."${handler}"(oldState)        			
        		}
        		else if(newState) {        			
        			entity."${handler}"(newState)        			
        		}
        }			
	}
}