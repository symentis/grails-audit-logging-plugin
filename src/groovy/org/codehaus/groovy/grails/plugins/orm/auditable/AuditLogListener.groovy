package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.commons.GrailsApplication

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
 * 2009-07-04 fetches its own session from sessionFactory to avoid transaction munging
 * 2009-09-05 getActor as a closure to allow developers to supply their own security plugins
 * 2009-09-25 rewrite.
 * 2009-10-04 preparing beta release
 * 2010-10-13 add a transactional config so transactions can be manually toggled by a user OR automatically disabled for testing
 * 2013-11-11 log collections (Thanks to Ankur Tripathi), log ids, replacement patterns support, cleanup
 * 2013-11-20 ability to disable audit logging by config
 */

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration
import org.hibernate.collection.PersistentCollection
import org.hibernate.engine.CollectionEntry
import org.hibernate.engine.PersistenceContext;
import org.hibernate.event.Initializable;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener
import org.springframework.web.context.request.RequestContextHolder;
import org.hibernate.SessionFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.Session
import grails.util.Environment

public class AuditLogListener implements PreDeleteEventListener, PostInsertEventListener, PostUpdateEventListener, Initializable {
	// set on startup
	GrailsApplication grailsApplication

	public static final Log log = LogFactory.getLog(AuditLogListener.class);
	public static Long TRUNCATE_LENGTH = 255

	/**
	 * The verbose flag flips on and off column by column change logging in
	 * insert and delete events. If this is true then all columns are logged
	 * each as an individual event.
	 */
	boolean verbose = true // in Config.groovy auditLog.verbose = true

	/**
	 * The logIds flag flips on and off object id logging for assotiated objects.
	 * If this is true then object Ids are logged into the newValue column.
	 */
	boolean logIds = false // in Config.groovy auditLog.logIds = false
	boolean transactional = false
	Long truncateLength
	SessionFactory sessionFactory

	String sessionAttribute
	String actorKey
	Closure actorClosure
	String propertyMask = '**********' // string to log for properties' value defined in maskList
	def replacementPatterns // in Config.groovy auditLog.replacementPatterns = ["pattern":"replacement"]

	void setActorClosure(Closure closure) {
		closure.delegate = this
		closure.properties.putAt("log", this.log)
		this.actorClosure = closure
	}

	void init() {
		if (Environment.getCurrent() != Environment.PRODUCTION && grailsApplication?.config.auditLog?.transactional == null) {
			transactional = false
		}

		log.info AuditLogListener.class.getCanonicalName() + " initializing AuditLogListener... "
		if (!truncateLength) {
			truncateLength = new Long(TRUNCATE_LENGTH)
		} else {
			log.debug "truncate length set to ${truncateLength}"
		}
		registerSelf()
	}

	void registerSelf() {
		sessionFactory.eventListeners.with {
			preDeleteEventListeners = addListener(sessionFactory.eventListeners.preDeleteEventListeners)
			postInsertEventListeners = addListener(sessionFactory.eventListeners.postInsertEventListeners)
			postUpdateEventListeners = addListener(sessionFactory.eventListeners.postUpdateEventListeners)
		}
	}

	Object[] addListener(final Object[] array) {
		def size = array?.length ?: 0
		def expanded = new Object[size + 1]
		if (array) {
			System.arraycopy(array, 0, expanded, 0, array.length)
		}
		expanded[-1] = this
		return expanded
	}

	SessionFactory getSessionFactory() {
		this.sessionFactory
	}

	void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory
	}

	void setTruncateLength(Long length) {
		// GPAUDITLOGGING-42
		// Note: We are not setting the column types in the Domain mapping, but constraints for now.

		// Mapping mapping = new GrailsDomainBinder().getMapping(AuditLogEvent) // since Grails 2.3, GrailsDomainBinder is not static anymore.
		// def oldValueColumnType = mapping?.columns?.oldValue?.type
		// def newValueColumnType = mapping?.columns?.newValue?.type

		def oldValueMaxSize = AuditLogEvent.constraints.oldValue?.maxSize
		def newValueMaxSize = AuditLogEvent.constraints.newValue?.maxSize
		if (!oldValueMaxSize && length > 255) {
			log.error("auditLog.TRUNCATE_LENGTH $length exceeds oldValue column size 255. Ignoring TRUNCATE_LENGTH setting.")
			return
		}
		if (oldValueMaxSize && length > oldValueMaxSize) {
			log.error("auditLog.TRUNCATE_LENGTH $length exceeds oldValue column size ${oldValueMaxSize}. Ignoring TRUNCATE_LENGTH setting.")
			return
		}
		if (!newValueMaxSize && length > 255) {
			log.error("auditLog.TRUNCATE_LENGTH $length exceeds newValue column size 255. Ignoring TRUNCATE_LENGTH setting.")
			return
		}
		if (newValueMaxSize && length > newValueMaxSize) {
			log.error("auditLog.TRUNCATE_LENGTH $length exceeds newValue column size ${newValueMaxSize}. Ignoring TRUNCATE_LENGTH setting.")
			return
		}
		this.truncateLength = length
	}

	/**
	 * if verbose is set to 'true' then you get a log event on
	 * each individually changed column/field sent to the database
	 * with a record of the old value and the new value.
	 */
	void setVerbose(final boolean verbose) {
		this.verbose = verbose
	}

	/**
	 * if propertyMask is set, mask the values of all configured properties
	 * in the "mask" map of DomainClasses.
	 */
	void setPropertyMask(final String configuredPropertyMask) {
		if (configuredPropertyMask) this.propertyMask = configuredPropertyMask
	}

	/**
	 * if logIds is set to 'true', log the id of changed associated objects
	 * TODO logIds currently does not work for collection entries.
	 */
	void setLogIds(final boolean doLogIds) {
		this.logIds = doLogIds
	}

	/**
	 * if replacementPatterns map is defined in the config,
	 * replace the old / new value strings with the entry value.
	 * Key = replacement regex pattern
	 * Value = replacement String to use.
	 */
	void setReplacementPatterns(final def patternMap) {
		this.replacementPatterns = patternMap
	}

	String getActor() {
		def actor = null
		try {
			if (actorClosure) {
				def attr = RequestContextHolder?.getRequestAttributes()
				def session = attr?.session
				if (attr && session) {
					actor = actorClosure.call(attr, session)
				} else { // no session or attributes mean this is invoked from a Service, Quartz Job, or other headless-operation
					actor = 'system'
				}
			}
		} catch (Exception ex) {
			log.error "The auditLog.actorClosure threw this exception: ${ex.message}"
			ex.printStackTrace()
			log.error "The auditLog.actorClosure will be disabled now."
			actorClosure = null
		}
		return actor?.toString()
	}

	def getUri() {
		def attr = RequestContextHolder?.getRequestAttributes()
		return (attr?.currentRequest?.uri?.toString()) ?: null
	}

	// returns true for auditable entities.
	def isAuditableEntity(event) {
		if (grailsApplication?.config.auditLog?.disabled == true) {
			log.debug("Auditing disabled in config.")
			return false
		}
		if (event && event.getEntity()) {
			def entity = event.getEntity()
			if (entity.metaClass.hasProperty(entity, 'auditable') && entity.'auditable') {
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
		if (entity?.metaClass?.hasProperty(entity, 'auditable') && entity?.'auditable') {
			if (entity.auditable instanceof java.util.Map &&
					entity.auditable.containsKey('handlersOnly')) {
				return (entity.auditable['handlersOnly']) ? true : false
			}
			return false
		}
		return false
	}

	/**
	 * The default ignore field list is:  ['version','lastUpdated']
	 * If you want to provide your own ignore list, specify in the DomainClass:
	 *
	 *   static auditable = [ignore:['version','lastUpdated','myField']]
	 *
	 * If you really want to trigger on version and lastUpdated changes:
	 *
	 *   static auditable = [ignore:[]]
	 *
	 */
	List ignoreList(entity) {
		def ignore = ['version', 'lastUpdated']
		if (entity?.metaClass?.hasProperty(entity, 'auditable')) {
			if (entity.auditable instanceof java.util.Map && entity.auditable.containsKey('ignore')) {
				log.debug "found an ignore list one this entity ${entity.getClass()}"
				def list = entity.auditable['ignore']
				if (list instanceof java.util.List) {
					ignore = list
				}
			}
		}
		return ignore
	}

	/**
	 * The default properties to mask list is:  ['password']
	 * if you want to provide your own mask list, specify in the DomainClass:
	 *
	 *   static auditable = [mask:['password','myField']]
	 *
	 * If you really want to log password property change values
	 * specify an empty mask list:
	 *
	 *   static auditable = [mask:[]]
	 *
	 */
	List maskList(entity) {
		def mask = ['password']
		if (entity?.metaClass?.hasProperty(entity, 'auditable')) {
			if (entity.auditable instanceof java.util.Map && entity.auditable.containsKey('mask')) {
				log.debug "found a mask list one this entity ${entity.getClass()}"
				def list = entity.auditable['mask']
				if (list instanceof java.util.List) {
					mask = list
				}
			}
		}
		return mask
	}

	def getEntityId(event) {
		if (event && event.entity) {
			def entity = event.getEntity()
			if (entity.metaClass.hasProperty(entity, 'id') && entity.'id') {
				return entity.id.toString()
			} else {
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
			def map = makeMap(names, state)
			if (audit && !callHandlersOnly(event.getEntity())) {
				def entity = event.getEntity()
				def entityName = entity.getClass().getName()
				def entityId = getEntityId(event)
				logChanges(null, map, null, entityId, 'DELETE', entityName)
			}
			if (audit) {
				executeHandler(event, 'onDelete', map, null)
			}
		} catch (HibernateException e) {
			log.error "Audit Plugin unable to process DELETE event"
			e.printStackTrace()
		}
		return false
	}

	/**
	 * Using the post insert event here instead of the pre-insert
	 * event so that I can log the id of the new entity after it
	 * is saved. That does mean the the insert event handlers
	 * can't work the way we want... but really it's the onChange
	 * event handler that does the magic for us.
	 */
	public void onPostInsert(final PostInsertEvent event) {
		try {
			def audit = isAuditableEntity(event)
			String[] propertyNames = event.getPersister().getPropertyNames()
			// TODO log assotiations as well. Currently they are omitted.
			Object[] state = event.getState()
			def map = makeMap(propertyNames, state)
			if (audit && !callHandlersOnly(event.getEntity())) {
				log.debug "logging insert for auditable entity"
				def entity = event.getEntity()
				def entityName = entity.getClass().getName()
				def entityId = getEntityId(event)
				logChanges(map, null, null, entityId, 'INSERT', entityName)
			}
			if (audit) {
				log.debug "calling event handlers for ${event.getEntity().getClass().getCanonicalName()}"
				executeHandler(event, 'onSave', null, map)
			}
		} catch (HibernateException e) {
			log.error "Audit Plugin unable to process INSERT event"
			e.printStackTrace()
		}
		log.trace "exit onPostInsert"
		return;
	}

	private def makeMap(String[] propertyNames, Object[] state) {
		def map = [:]
		for (int index = 0; index < propertyNames.length; index++) {
			map[propertyNames[index]] = state[index]
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
		if (isAuditableEntity(event)) {
			log.trace "${event.getClass()} onChange handler will be called"
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
	private boolean significantChange(entity, oldMap, newMap) {
		def ignore = ignoreList(entity)
		ignore?.each({ key ->
			oldMap.remove(key)
			newMap.remove(key)
		})
		boolean changed = false
		oldMap.each({ k, v ->
			if (v != newMap[k]) {
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

		def propertyNames = event.getPersister().getPropertyNames()
		def oldMap = [:]
		def newMap = [:]

		if (propertyNames) {
			for (int index = 0; index < newState.length; index++) {
				if (propertyNames[index]) {
					if (oldState) {
						populateOldStateMap(oldState, oldMap, propertyNames[index], index)
					}
					if (newState) {
						newMap[propertyNames[index]] = newState[index]
					}
				}
			}
		}

		if (!significantChange(entity, oldMap, newMap)) {
			return
		}

		// allow user's to over-ride whether you do auditing for them.
		if (!callHandlersOnly(event.getEntity())) {
			logChanges(newMap, oldMap, event, entityId, 'UPDATE', entityName)
		}
		/**
		 * Hibernate only saves changes when there are changes.
		 * That means the onUpdate event was the same as the
		 * onChange with no parameters. I've eliminated onUpdate.
		 */
		executeHandler(event, 'onChange', oldMap, newMap)
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
	def logChanges(newMap, oldMap, parentObject, persistedObjectId, eventName, className) {
		AuditLogEvent audit = null
		def persistedObjectVersion = (newMap?.version) ?: oldMap?.version
		if (newMap) newMap.remove('version')
		if (oldMap) oldMap.remove('version')
		// new+old
		if (newMap && oldMap) {
			log.trace "there are new and old values to log"
			newMap.each { key, val ->
				if (val != oldMap[key]) {
					audit = new AuditLogEvent(
							actor:this.getActor(),
							uri:this.getUri(),
							className:className,
							eventName:eventName,
							persistedObjectId:persistedObjectId?.toString(),
							persistedObjectVersion:persistedObjectVersion?.toLong(),
							propertyName:key,
							oldValue:conditionallyMaskAndTruncate(key, oldMap[key]),
							newValue:conditionallyMaskAndTruncate(key, newMap[key]),
					)
					saveAuditLog(audit)
				}
			}
			return
		}
		//
		if (newMap && verbose) {
			log.trace "there are only new values and logging is verbose."
			newMap.each { key, val ->
				audit = new AuditLogEvent(
						actor:this.getActor(),
						uri:this.getUri(),
						className:className,
						eventName:eventName,
						persistedObjectId:persistedObjectId?.toString(),
						persistedObjectVersion:persistedObjectVersion?.toLong(),
						propertyName:key,
						oldValue:null,
						newValue:conditionallyMaskAndTruncate(key, val),
				)
				saveAuditLog(audit)
			}
			return
		}
		//
		if (oldMap && verbose) {
			log.trace "there is only an old map of values available and logging is verbose... "
			oldMap.each { key, val ->
				audit = new AuditLogEvent(
						actor:this.getActor(),
						uri:this.getUri(),
						className:className,
						eventName:eventName,
						persistedObjectId:persistedObjectId?.toString(),
						persistedObjectVersion:persistedObjectVersion?.toLong(),
						propertyName:key,
						oldValue:conditionallyMaskAndTruncate(key, val),
						newValue:null
				)
				saveAuditLog(audit)
			}
			return
		}
		// default
		log.trace "creating a basic audit logging event object."
		audit = new AuditLogEvent(
				actor:this.getActor(),
				uri:this.getUri(),
				className:className,
				eventName:eventName,
				persistedObjectId:persistedObjectId?.toString(),
				persistedObjectVersion:persistedObjectVersion?.toLong()
		)
		saveAuditLog(audit)
		return
	}

	String conditionallyMaskAndTruncate(final String key, final obj) {
		if (maskList().contains(key)) {
			String maskedValue = propertyMask
			log.trace("Masking property ${key} with ${propertyMask}")
			return truncate(maskedValue)
		}
		truncate(obj)
	}

	String truncate(final obj) {
		truncate(obj, truncateLength.toInteger())
	}

	String truncate(final obj, int max) {
		log.trace "trimming object's string representation based on ${max} characters."
		def str
		// GPAUDITLOGGING-43
		if (logIds) {
			if (obj instanceof Collection || obj instanceof Map) {
				obj?.each {
					str = appendWithId(it, str)
				}
			} else {
				str = appendWithId(obj, str)
			}
		} else {
			str = "$obj".trim() // GPAUDITLOGGING-40
		}
		str = replaceByReplacementPatterns(str, obj)
		return (str?.length() > max) ? str?.substring(0, max) : str
	}

	private String appendWithId(obj, str) {
		if (obj?.metaClass?.respondsTo(obj, "getId")) {
			// add id of obj
			str = str ? "$str, [id:${obj.id}]$obj" : "[id:${obj.id}]$obj"
		} else {
			str = str ? "$str,$obj" : "$obj"
		}
		str
	}

	/**
	 * This calls defined entity handlers based on what was passed in to it.
	 */
	def executeHandler(event, handler, oldState, newState) {
		log.trace "start executeHandler()"
		def entity = event.getEntity()
		if (isAuditableEntity(event) && entity.metaClass.hasProperty(entity, handler)) {
			log.trace "entity was auditable and had a handler property ${handler}"
			if (oldState && newState) {
				log.trace "there was both an old state and a new state"
				if (entity."${handler}".maximumNumberOfParameters == 2) {
					log.trace "there are two parameters to the handler so I'm sending old and new value maps"
					entity."${handler}"(oldState, newState)
				} else {
					log.trace "only one parameter on the closure I'm sending oldMap and newMap as part of a Map parameter"
					entity."${handler}"([oldMap:oldState, newMap:newState])
				}
			} else if (oldState) {
				log.trace "sending old state into ${handler}"
				entity."${handler}"(oldState)
			} else if (newState) {
				log.trace "sending new state into ${handler}"
				entity."${handler}"(newState)
			}
		}
		log.trace "finish executeHandler()"
	}

	/**
	 * Performs special logging and save actions for AuditLogEvent class. You cannot use
	 * the GORM save features to persist this class on transactional databases due to timing
	 * conflicts with the session. Due to problems in this routine on some databases
	 * this closure has comprehensive trace logging in it for your diagnostics.
	 *
	 * It has also been written as a closure for your sake so that you may over-ride the
	 * save closure with your own code (should your particular database not work with this code)
	 * you may over-ride the definition of this closure using ... TODO allow over-ride via config
	 *
	 * To debug in Config.groovy set:
	 *    log4j.debug 'org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener'
	 * or
	 *    log4j.trace 'org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener'
	 *
	 * SEE: GRAILSPLUGINS-391
	 */
	def saveAuditLog = { AuditLogEvent audit ->
		audit.with {
			dateCreated = new Date()
			lastUpdated = new Date()
		}
		log.info audit
		try {
			// NOTE: you simply cannot use the session that GORM is using for some
			// audit log events. That's because some audit events occur *after* a
			// transaction has committed. Late session save actions can invalidate
			// sessions or you can essentially roll-back your audit log saves. This is
			// why you have to open your own session and transaction on some
			// transactional databases and not others.
			Session session = sessionFactory.openSession()
			log.trace "opened new session for audit log persistence"
			def trans = null
			if (transactional) {
				trans = session.beginTransaction()
				log.trace " + began transaction "
			}
			def saved = session.merge(audit)
			log.debug " + saved log entry id:'${saved.id}'."
			session.flush()
			log.trace " + flushed session"
			if (transactional) {
				trans?.commit()
				log.trace " + committed transaction"
			}
			session.close()
			log.trace "session closed"
		} catch (org.hibernate.HibernateException ex) {
			log.error "AuditLogEvent save has failed!"
			log.error ex.message
			log.error audit.errors
		}
		return
	}

	private populateOldStateMap(def oldState, Map oldMap, String keyName, int index) {
		def oldPropertyState = oldState[index]
		if (oldPropertyState instanceof PersistentCollection) {
			PersistentCollection pc = (PersistentCollection) oldPropertyState;
			PersistenceContext context = sessionFactory.getCurrentSession().getPersistenceContext();
			CollectionEntry entry = context.getCollectionEntry(pc);
			Object snapshot = entry.getSnapshot();
			if (pc instanceof List) {
				oldMap[keyName] = Collections.unmodifiableList((List) snapshot);
			} else if (pc instanceof Map) {
				oldMap[keyName] = Collections.unmodifiableMap((Map) snapshot);
			} else if (pc instanceof Set) {
				//Set snapshot is actually stored as a Map
				if (snapshot) {
					Map snapshotMap = (Map) snapshot;
					oldMap[keyName] = Collections.unmodifiableSet(new HashSet(snapshotMap?.values()));
					log.trace(oldMap[keyName].class);
				} else {
					log.trace("Cannot get snapshot of $pc Entry: $entry for keyName: $keyName");
					oldMap[keyName] = null
				}
			} else {
				oldMap[keyName] = pc;
			}
		} else {
			oldMap[keyName] = oldPropertyState
		}
	}

	// Rework String based on configured replacementPatterns
	private String replaceByReplacementPatterns(String str, final obj) {
		replacementPatterns?.each { k, v ->
			str = str?.replace(k, v)
		}
		return str
	}
}
