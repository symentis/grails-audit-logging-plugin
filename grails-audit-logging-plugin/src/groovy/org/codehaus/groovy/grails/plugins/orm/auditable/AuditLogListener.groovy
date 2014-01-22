package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PreDeleteEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.springframework.context.ApplicationEvent
import org.springframework.web.context.request.RequestContextHolder

import groovy.util.logging.Commons

import static org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListenerUtil.*

/**
 * Grails interceptor for logging saves, updates, deletes and acting on
 * individual properties changes and delegating calls back to the Domain Class
 *
 * 2008-04-17 created initial version 0.1
 * 2008-04-21 changes for version 0.2 include simpler events, config file, removed 'onUpdate' event.
 * 2008-06-04 added ignore fields feature
 * 2009-07-04 fetches its own session from sessionFactory to avoid transaction munging
 * 2009-09-05 getActor as a closure to allow developers to supply their own security plugins
 * 2009-09-25 rewrite.
 * 2009-10-04 preparing beta release
 * 2010-10-13 add a transactional config so transactions can be manually toggled by a user OR automatically disabled for testing
 *
 * @author Shawn Hartsock
 * @author Aaron Long
 */
@Commons
class AuditLogListener extends AbstractPersistenceEventListener {
    def grailsApplication

    /**
     * The verbose flag flips on and off column by column change logging in
     * insert and delete events. If this is true then all columns are logged
     * each as an individual event.
     *
     * If verbose is set to 'true' then you get a log event on
     * each individually changed column/field sent to the database
     * with a record of the old value and the new value.
     *
     * auditLog.verbose = true
     */
    Boolean verbose = true
    Boolean transactional = false

    Long truncateLength
    String sessionAttribute
    String actorKey
    String propertyMask
    Closure actorClosure

    // Global list of attribute changes to ignore, defaults to ['version', 'lastUpdated']
    List<String> defaultIgnoreList
    List<String> defaultMaskList
    Map<String, String> replacementPatterns

    AuditLogListener(Datastore datastore) {
        super(datastore)
    }

    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        if (isAuditableEntity(event.entityObject, event.eventType)) {
            log.trace "Audit logging: ${event.eventType.name()} for ${event.entityObject.class.name}"

            switch(event.eventType) {
                case EventType.PostInsert:
                    onPostInsert(event as PostInsertEvent)
                    break
                case EventType.PreUpdate:
                    onPreUpdate(event as PreUpdateEvent)
                    break
                case EventType.PreDelete:
                    onPreDelete(event as PreDeleteEvent)
                    break
            }
        }
    }

    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return eventType.isAssignableFrom(PostInsertEvent) ||
               eventType.isAssignableFrom(PreUpdateEvent) ||
               eventType.isAssignableFrom(PreDeleteEvent)
    }

    void setActorClosure(Closure closure) {
        closure.delegate = this
        closure.properties['log'] = log
        actorClosure = closure
    }

    String getActor() {
        def actor = null
        try {
            if (actorClosure) {
                def attr = RequestContextHolder?.getRequestAttributes()
                def session = attr?.session
                if (attr && session) {
                    actor = actorClosure.call(attr, session)
                }
                else {
                    // No session or attributes mean this is invoked from a Service, Quartz Job, or other headless-operation
                    actor = 'system'
                }
            }
        }
        catch (ex) {
            log.error "The auditLog.actorClosure threw this exception", ex
            log.error "The auditLog.actorClosure will be disabled now."
            actorClosure = null
        }
        return actor?.toString()
    }

    String getUri() {
        def attr = RequestContextHolder?.getRequestAttributes()
        return (attr?.currentRequest?.uri?.toString()) ?: null
    }

    /**
     * We allow users to specify static auditable = [handlersOnly: true]
     * if they don't want us to log events for them and instead have their own plan.
     */
    boolean callHandlersOnly(domain) {
        // Allow global configuration of handlers only
        if (grailsApplication.config.auditLog.handlersOnly) {
            return true
        }

        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('handlersOnly')) {
            return (auditableMap['handlersOnly']) ? true : false
        }
        return false
    }

    /**
     * The default ignore field list is: ['version','lastUpdated'] if you want
     * to provide your own ignore list do so by specifying the ignore list like so:
     *
     *   static auditable = [ignore:['version','lastUpdated','myField']]
     *
     * You may change the default ignore list by setting:
     *
     *   auditLog.defaultIgnore = ['version', 'lastUpdated', 'myOtherField']
     *
     * If instead you really want to trigger on version and lastUpdated changes you
     * may specify an empty ignore list:
     *
     *   static auditable = [ignore:[]]
     *
     * Or globally:
     *
     *   auditLog.defaultIgnore = []
     *
     */
    List<String> ignoreList(domain) {
        def ignore = defaultIgnoreList

        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('ignore')) {
            log.debug "Found an ignore list on this entity ${domain.class.name}"
            def list = auditableMap['ignore']
            if (list instanceof List) {
                ignore = list
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
     * Or globally:
     *
     *   auditLog.defaultMask = ['password']
     *
     */
    List maskList(domain) {
        def mask = defaultMaskList

        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('mask')) {
            log.debug "Found a mask list one this entity ${domain.class.name}"
            def list = domain.auditable['mask']
            if (list instanceof List) {
                mask = list
            }
        }

        return mask
    }

    /**
     * Get the Id to display for this entity when logging. Domain classes can override the property
     * used by supplying a [entityId] attribute in the auditable Map.
     *
     * @param event
     * @return String key
     */
    String getEntityId(AbstractPersistenceEvent event) {
        def domain = event.entityObject
        def entity = getDomainClass(domain)

        // If we have a display key, allow override of what shows as the entity id
        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('entityId')) {
            def entityId = auditableMap.entityId
            if (entityId instanceof Closure) {
                return entityId.call(domain) as String
            }
            else if (entityId instanceof Collection) {
                return entityId.inject { id, prop -> id + ("|"+domain."${prop}")?.toString() }
            }
            else if (entityId instanceof String) {
                return domain."${entityId}" as String
            }
        }

        return domain."${entity.identifier.name}" as String
    }

    /**
     * We must use the preDelete event if we want to capture
     * what the old object was like.
     */
    private void onPreDelete(PreDeleteEvent event) {
        def domain = event.entityObject
        try {
            def entity = getDomainClass(domain)

            def map = makeMap(entity.persistentProperties*.name, domain)
            if (!callHandlersOnly(domain)) {
                logChanges(domain, null, map, getEntityId(event), 'DELETE', entity.name)
            }

            executeHandler(domain, 'onDelete', map, null)
        }
        catch (e) {
            log.error "Audit plugin unable to process DELETE event for ${domain.class.name}", e
        }
    }

    /**
     * I'm using the post insert event here instead of the pre-insert
     * event so that I can log the id of the new entity after it
     * is saved. That does mean the the insert event handlers
     * can't work the way we want... but really it's the onChange
     * event handler that does the magic for us.
     */
    private void onPostInsert(PostInsertEvent event) {
        def domain = event.entityObject
        try {
            def entity = getDomainClass(domain)

            def map = makeMap(entity.persistentProperties*.name, domain)
            if (!callHandlersOnly(domain)) {
                logChanges(domain, map, null, getEntityId(event), 'INSERT', entity.name)
            }

            executeHandler(domain, 'onSave', null, map)
        }
        catch (e) {
            log.error "Audit plugin unable to process INSERT event for ${domain.class.name}", e
        }
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
    private void onPreUpdate(PreUpdateEvent event) {
        def domain = event.entityObject
        try {
            // Get all the dirty properties
            List<String> dirtyProperties = domain.dirtyPropertyNames
            if (dirtyProperties) {

                // Get the prior values for everything that is dirty
                Map oldMap = dirtyProperties.collectEntries { String property ->
                    [property, domain.getPersistentValue(property)]
                }

                // Get the current values for everything that is dirty
                Map newMap = makeMap(dirtyProperties, domain)

                if (!significantChange(domain, oldMap, newMap)) {
                    return
                }

                // Allow user to override whether you do auditing for them
                if (!callHandlersOnly(domain)) {
                    def entity = getDomainClass(domain)
                    logChanges(domain, newMap, oldMap, getEntityId(event), 'UPDATE', entity.name)
                }

                executeHandler(domain, 'onChange', oldMap, newMap)
            }
        }
        catch (e) {
            log.error "Audit plugin unable to process UPDATE event for ${domain.class.name}", e
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
    private boolean significantChange(domain, Map oldMap, Map newMap) {
        def ignore = ignoreList(domain)
        ignore?.each { String key ->
            oldMap.remove(key)
            newMap.remove(key)
        }
        boolean changed = false
        oldMap.each { String k, Object v ->
            if (v != newMap[k]) {
                changed = true
            }
        }
        return changed
    }

    private makeMap(List<String> propertyNames, domain) {
        propertyNames.collectEntries { [it, domain."${it}"] }
    }

    private GrailsDomainClass getDomainClass(domain) {
        grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, domain.class.name) as GrailsDomainClass
    }

    /**
     * Leans heavily on the "toString()" of a property
     * ... this feels crufty... should be tighter...
     */
    def logChanges(domain, Map newMap, Map oldMap, persistedObjectId, eventName, className) {
        List<String> maskList = maskList(domain)
        maskList?.each {

        }

        def persistedObjectVersion = (newMap?.version) ?: oldMap?.version
        newMap?.remove('version')
        oldMap?.remove('version')

        if (newMap && oldMap) {
            log.trace "There are new and old values to log"
            newMap.each { String key, val ->
                if (val != oldMap[key]) {
                    def audit = new AuditLogEvent(
                        actor: getActor(),
                        uri: getUri(),
                        className: className,
                        eventName: eventName,
                        persistedObjectId: persistedObjectId?.toString(),
                        persistedObjectVersion: persistedObjectVersion as Long,
                        propertyName: key,
                        oldValue: conditionallyMaskAndTruncate(domain, key, oldMap[key]),
                        newValue: conditionallyMaskAndTruncate(domain, key, newMap[key]))
                    saveAuditLog(audit)
                }
            }
        }
        else if (newMap && verbose) {
            log.trace "there are new values and logging is verbose ... "
            newMap.each { String key, val ->
                def audit = new AuditLogEvent(
                    actor: getActor(),
                    uri: getUri(),
                    className: className,
                    eventName: eventName,
                    persistedObjectId: persistedObjectId?.toString(),
                    persistedObjectVersion: persistedObjectVersion as Long,
                    propertyName: key,
                    oldValue: null,
                    newValue: conditionallyMaskAndTruncate(domain, key, val))
                saveAuditLog(audit)
            }
        }
        else if (oldMap && verbose) {
            log.trace "there is only an old map of values available and logging is set to verbose... "
            oldMap.each { String key, val ->
                def audit = new AuditLogEvent(
                    actor: getActor(),
                    uri: getUri(),
                    className: className,
                    eventName: eventName,
                    persistedObjectId: persistedObjectId?.toString(),
                    persistedObjectVersion: persistedObjectVersion as Long,
                    propertyName: key,
                    oldValue: conditionallyMaskAndTruncate(domain, key, val),
                    newValue: null)
                saveAuditLog(audit)
            }
        }
        else {
            log.trace "creating a basic audit logging event object."
            def audit = new AuditLogEvent(
                actor: getActor(),
                uri: getUri(),
                className: className,
                eventName: eventName,
                persistedObjectId: persistedObjectId?.toString(),
                persistedObjectVersion: persistedObjectVersion as Long)
            saveAuditLog(audit)
        }
    }

    /**
     * @param domain the auditable domain object
     * @param key property name
     * @param value the value of the property
     * @return
     */
    String conditionallyMaskAndTruncate(domain, String key, value){
        if (maskList(domain)?.contains(key)){
            log.trace("Masking property ${key} with ${propertyMask}")
            propertyMask
        }
        else {
            truncate(value)
        }
    }

    String truncate(value) {
        truncate(value, truncateLength.toInteger())
    }

    String truncate(value, int max) {
        log.trace "trimming object's string representation based on ${max} characters."

        // GPAUDITLOGGING-40
        def str = replaceByReplacementPatterns("$value".trim())
        (str?.length() > max) ? str?.substring(0, max) : str
    }

    String replaceByReplacementPatterns(String str) {
        if (str == null) {
            return null
        }
        replacementPatterns?.each { String from, String to ->
            str = str.replace(from, to)
        }
        return str
    }

    /**
     * This calls the handlers based on what was passed in to it.
     */
    def executeHandler(domain, handler, oldState, newState) {
        log.trace "calling execute handler ... "

        if (domain.metaClass.hasProperty(domain, handler)) {
            log.trace "entity was auditable and had a handler property ${handler}"
            if (oldState && newState) {
                log.trace "there was both an old state and a new state"
                if (domain."${handler}".maximumNumberOfParameters == 2) {
                    log.trace "there are two parameters to the handler so I'm sending old and new value maps"
                    domain."${handler}"(oldState, newState)
                }
                else {
                    log.trace "only one parameter on the closure I'm sending oldMap and newMap as part of a Map parameter"
                    domain."${handler}"([oldMap: oldState, newMap: newState])
                }
            }
            else if (oldState) {
                log.trace "sending old state into ${handler}"
                domain."${handler}"(oldState)
            }
            else if (newState) {
                log.trace "sending new state into ${handler}"
                domain."${handler}"(newState)
            }
        }
        log.trace "... execute handler is finished."
    }

    /**
     * Save the audit log in a new session and optionally, in a transaction
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
            dateCreated = lastUpdated = new Date()
        }
        log.info audit
        try {
            AuditLogEvent.withNewSession {
                if (transactional) {
                    AuditLogEvent.withTransaction {
                        audit.merge(flush: true, failOnError: true)
                    }
                }
                else {
                    audit.merge(flush: true, failOnError: true)
                }
            }
        }
        catch (e) {
            log.error "Failed to create AuditLogEvent for ${audit}", e
        }
    }
}
