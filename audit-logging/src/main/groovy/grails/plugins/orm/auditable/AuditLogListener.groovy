/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package grails.plugins.orm.auditable

import com.fasterxml.jackson.annotation.JsonIgnore
import grails.compiler.GrailsCompileStatic
import grails.core.GrailsDomainClass
import grails.util.GrailsClassUtils
import groovy.util.logging.Commons
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang.reflect.FieldUtils
import org.grails.datastore.gorm.timestamp.DefaultTimestampProvider
import org.grails.datastore.gorm.timestamp.TimestampProvider
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.*
import org.springframework.context.ApplicationEvent
import org.springframework.web.context.request.RequestContextHolder

import static grails.plugins.orm.auditable.AuditLogListenerUtil.*
/**
 * Grails interceptor for logging saves, updates, deletes and acting on
 * individual properties changes and delegating calls back to the Domain Class
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
     *
     * If nonVerboseDelete is set to "true" then delete events are never verbosely logged,
     * even when "verbose=true".
     */
    Boolean verbose = true
    Boolean nonVerboseDelete = false
    Boolean logIds = false
    Boolean transactional = false
    Boolean logFullClassName = false
    Integer truncateLength
    String sessionAttribute
    String actorKey
    String propertyMask

    @JsonIgnore
    Closure actorClosure

    boolean usingHibernate = false
    
    boolean stampEnabled = true
    boolean stampAlways = false
    boolean stampTimestamp = false
    
    // Global list of attribute changes to ignore, defaults to ['version', 'lastUpdated']
    List<String> defaultIgnoreList
    List<String> defaultMaskList
    Map<String, String> replacementPatterns

    private TimestampProvider timestampProvider = new DefaultTimestampProvider();

    AuditLogListener(Datastore datastore) {
        super(datastore)
    }

    @Override
    @GrailsCompileStatic
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        // GPAUDITLOGGING-64: Even we register AuditLogListeners per datasource, at least up to Grails 2.4.2 events for other datasources
        // get triggered in all other listeners.
        if (event.source != this.datastore) {
            if(log.traceEnabled) log.trace("Event received for other datastore. Ignoring event")
            return
        }
        if (stampEnabled && (stampAlways || isStampable(event))) {
            stamp(event);
        }
        if (isAuditableEntity(event.entityObject, getEventName(event))) {
            if(log.traceEnabled) log.trace "Audit logging: ${event.eventType.name()} for ${event.entityObject.class.name}"

            switch (event.eventType) {
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

    @GrailsCompileStatic
    void stamp(AbstractPersistenceEvent event) {
        def entity = event.entityObject
        def actor = getActor()
        
        if (EventType.PreInsert == event.eventType) {
            stampTimestamp(event,entity)
            
            stampCreatedBy(entity,actor,event)
            stampLastUpdatedBy(entity,actor,event)
        } else {
            stampTimestampUpdate(event,entity)
            stampLastUpdatedBy(entity,actor,event)
        }
    }

    void stampTimestamp(AbstractPersistenceEvent event, def entity){
        String dateCreatedProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_dateCreatedStampableProperty')
        Class<?> dateCreatedType = GrailsClassUtils.getPropertyType(entity.class,dateCreatedProperty)
        def timestamp = timestampProvider.createTimestamp(dateCreatedType)
        
        entity.setProperty(dateCreatedProperty,timestamp)
        if(usingHibernate) syncHibernateState(event,dateCreatedProperty,timestamp)
        
        String lastUpdatedProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_lastUpdatedStampableProperty')
        Class<?> lastUpdateType = GrailsClassUtils.getPropertyType(entity.class,lastUpdatedProperty)
        if(!lastUpdateType.isAssignableFrom(dateCreatedType)){
            timestamp = timestampProvider.createTimestamp(lastUpdateType)
        }
        entity.setProperty(lastUpdatedProperty,timestamp)
        if(usingHibernate) syncHibernateState(event,lastUpdatedProperty,timestamp)
    }
    
    void stampTimestampUpdate(AbstractPersistenceEvent event,def entity){
        String lastUpdatedProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_lastUpdatedStampableProperty')
        Class<?> lastUpdateType = GrailsClassUtils.getPropertyType(entity.class,lastUpdatedProperty)
        def timestamp = timestampProvider.createTimestamp(lastUpdateType)
        
        entity.setProperty(lastUpdatedProperty,timestamp)
        
        if(usingHibernate) syncHibernateState(event,lastUpdatedProperty,timestamp)
    }
    
    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return eventType.isAssignableFrom(PostInsertEvent) ||
            eventType.isAssignableFrom(PreInsertEvent) ||
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
        if (actorClosure) {
            def attr = RequestContextHolder.getRequestAttributes()
            //def session = attr?.session
            if (attr) {
                def session
                try {
                    // When requesting the session via the getSession() method a
                    // new session will be created even if it doesn't exists.
                    // This is a problem with rest requests where it's not desirable
                    // that a session is created for each request
                    session = FieldUtils.readDeclaredField(attr,'session',true)
                } catch(Exception ignore){
                  log.debug("Cannot get session. " + ignore)
                }
                try {
                    actor = actorClosure.call(attr, session /* session is possibly null */)
                } catch (ex) {
                    log.error "The auditLog.actorClosure threw this exception: ", ex
                    log.error "The auditLog.actorClosure will be disabled now."
                    actorClosure = null
                }
            }
        }
        // If we couldn't find an actor, use the configured default or just 'system'
        if (!actor) {
            actor = AuditLoggingConfigUtils.auditConfig.defaultActor ?: 'system'
        }
        if(log.traceEnabled) log.trace("Actor: $actor")
        return actor?.toString()
    }

    String getUri(domain = null) {
        if (domain?.metaClass?.hasProperty(domain, "getAuditLogUri")) {
            return domain."getAuditLogUri"()?.toString()
        }
        return this.requestUri
    }

    private String getRequestUri() {
        def attr = RequestContextHolder?.getRequestAttributes()
        return (attr?.currentRequest?.uri?.toString()) ?: null
    }

    /**
     * We allow users to specify static auditable = [handlersOnly: true]
     * if they don't want us to log events for them and instead have their own plan.
     */
    boolean callHandlersOnly(domain) {
        // Allow global configuration of handlers only
        if (AuditLoggingConfigUtils.auditConfig.handlersOnly) {
            return true
        }

        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('handlersOnly')) {
            return (auditableMap['handlersOnly']) ? true : false
        }
        return false
    }

    /**
     * We allow users to specify static auditable = [ignoreEvents: ["onSave","onDelete","onChange"]]
     * First check global variable auditLog.events
     * @param domain
     * @param event
     */
    boolean ignoreEvent(domain, event) {
        List<String> ignoreEvents = Collections.EMPTY_LIST
        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('ignoreEvents')) {
            ignoreEvents = auditableMap['ignoreEvents'] as List<String>
        }
        return ignoreEvents.contains(event)
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
     * Get the list of auditable properties.  This is used to override
     * the default behaviour of auditing all properties except those in the
     * ignore list.
     *
     *   static auditable = [auditableProperties: ['dateCreated','lastUpdated','myField']]
     *
     *
     */
    List auditableProperties(domain) {

        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('auditableProperties')) {
            log.debug "Found auditableProperty list on this entity ${domain.class.name}"
            def list = auditableMap['auditableProperties']
            if (list instanceof List) {
                return list
            }
        }

        null
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

    protected stampCreatedBy(entity,actor,event) {
        String dateCreatedByProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_createdByStampableProperty')
        entity.setProperty(dateCreatedByProperty,actor)
        if(usingHibernate){
            syncHibernateState(event,dateCreatedByProperty,actor)
        }
    }

    protected stampLastUpdatedBy(entity,actor,event) {
        String dateCreatedByProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_createdByStampableProperty')
        String lastUpdatedByProperty = GrailsClassUtils.getStaticPropertyValue(entity.class,'_lastUpdatedByStampableProperty')
        entity.setProperty(lastUpdatedByProperty,actor)
        
        if(usingHibernate){
            syncHibernateState(event,dateCreatedByProperty,entity.getProperty(dateCreatedByProperty))
            syncHibernateState(event,lastUpdatedByProperty,actor)
        }
    }

    private void syncHibernateState(AbstractPersistenceEvent event,String propertyName,Object value){
        //event.nativeEvent.getPersister().getEntityMetamodel().getPropertyNames();
        String[] propertyNames = event.nativeEvent.persister.entityMetamodel.properties*.name
        Object[] state = event.nativeEvent.state

        int index = ArrayUtils.indexOf(propertyNames, propertyName);
        if(index>=0){
            state[index] = value
        }
    }
    
    /**
     * We must use the preDelete event if we want to capture
     * what the old object was like.
     */
    protected void onPreDelete(PreDeleteEvent event) {
        def domain = event.entityObject
        try {
            def entity = getDomainClass(domain)

            if (ignoreEvent(domain, "onDelete")) {
                return
            }

            def map = makeMap(entity.persistentProperties*.name as Set, domain)
            if (!callHandlersOnly(domain)) {
                if (nonVerboseDelete) {
                    log.debug("Forced Non-Verbose logging by config onPreDelete.")
                    withoutVerboseAuditLog {
                        logChanges(domain, null, map, getEntityId(domain), getEventName(event), getClassName(entity))
                    }
                } else {
                    logChanges(domain, null, map, getEntityId(domain), getEventName(event), getClassName(entity))
                }
            } else {
                def identifier = entity.identifier.name
                if (!map.containsKey(identifier) && domain."$identifier") {
                    map << [(identifier): domain."$identifier"]
                }
            }
            executeHandler(domain, 'onDelete', map, null)
        }
        catch (e) {
            log.error "Audit plugin unable to process delete event for ${domain.class.name}: ", e
        }
    }

    /**
     * I'm using the post insert event here instead of the pre-insert
     * event so that I can log the id of the new entity after it
     * is saved. That does mean the the insert event handlers
     * can't work the way we want... but really it's the onChange
     * event handler that does the magic for us.
     */
    protected void onPostInsert(PostInsertEvent event) {
        def domain = event.entityObject
        try {
            def entity = getDomainClass(domain)

            if (ignoreEvent(domain, "onSave")) {
                return
            }

            def map = makeMap(entity.persistentProperties*.name as Set, domain)
            if (!callHandlersOnly(domain)) {
                logChanges(domain, map, null, getEntityId(domain), getEventName(event), getClassName(entity))
            } else {
                def identifier = entity.identifier.name
                if (!map.containsKey(identifier) && domain."$identifier") {
                    map << [(identifier): domain."$identifier"]
                }
            }

            executeHandler(domain, 'onSave', null, map)
        }
        catch (e) {
            log.error "Audit plugin unable to process insert event for ${domain.class.name}", e
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
    protected void onPreUpdate(PreUpdateEvent event) {
        def domain = event.entityObject
        try {
            def entity = getDomainClass(domain)

            if (ignoreEvent(domain, "onChange")) {
                return
            }

            // Get all the dirty properties
            Set<String> dirtyProperties = getDirtyPropertyNames(domain, entity)
            if (dirtyProperties) {
                // Get the prior values for everything that is dirty
                Map oldMap = dirtyProperties.collectEntries { String property ->
                    [property, getPersistentValue(domain, property, entity)]
                }

                // Get the current values for everything that is dirty
                Map newMap = makeMap(dirtyProperties, domain)

                if (!significantChange(domain, oldMap, newMap)) {
                    return
                }

                // Allow user to override whether you do auditing for them
                if (!callHandlersOnly(domain)) {
                    logChanges(domain, newMap, oldMap, getEntityId(domain), getEventName(event), getClassName(entity))
                } else {
                    def identifier = entity.identifier.name
                    if (!(oldMap.containsKey(identifier) || newMap.containsKey(identifier)) && domain."$identifier") {
                        [oldMap, newMap]*.leftShift([(identifier): domain."$identifier"])
                    }
                }

                executeHandler(domain, 'onChange', oldMap, newMap)
            }
        }
        catch (e) {
            log.error "Audit plugin unable to process update event for ${domain.class.name}", e
        }
    }

    /**
     * Get the class name to log.
     *
     * @param entity A {@link GrailsDomainClass} instance.
     * @return The {@link GrailsDomainClass#getFullName()}, if {@link #logFullClassName} is true,
     * {@link GrailsDomainClass#getName()}, otherwise.
     */
    protected String getClassName(GrailsDomainClass entity) {
        logFullClassName ? entity.fullName : entity.name
    }

    /**
     * Get the persistent value for the given domain.property. This method includes
     * some special case handling for hasMany properties, which don't follow normal rules.
     *
     * TODO - Need a way to load the old value generically for a collection
     */
    protected getPersistentValue(domain, String property, GrailsDomainClass entity) {
        if (entity.associationMap.containsKey(property)) {
            "N/A"
        } else {
            domain.getPersistentValue(property)
        }
    }

    /**
     * Return dirty property names for the given domain class. This method includes some
     * special case logic for hasMany properties, which don't follow normal isDirty rules.
     */
    protected Set<String> getDirtyPropertyNames(domain, GrailsDomainClass entity) {
        Set<String> dirtyProperties = domain.dirtyPropertyNames ?: []

        // In some cases, collections aren't listed as being dirty in the dirty property names.
        // We need to check them individually.
        entity.associationMap.each { String associationName, value ->
            def collection = domain."${associationName}"
            if (collection?.respondsTo('isDirty') && collection?.isDirty()) {
                dirtyProperties << associationName
            }
        }

        dirtyProperties
    }

    /**
     * Prevent infinate loops of change logging by trapping
     * non-significant changes. Occasionally you can set up
     * a change handler that will create a "trivial" object
     * change that you don't want to trigger another change
     * event. So this feature uses the ignore parameter
     * to provide a list of fields for onChange to ignore.
     */
    protected boolean significantChange(domain, Map oldMap, Map newMap) {
        def auditableProperties = auditableProperties(domain)
        def ignore = ignoreList(domain)
        boolean changed = false
        oldMap.each { String k, Object v ->
            if (isPropertyAuditable(k, auditableProperties, ignore) && v != newMap[k]) {
                changed = true
            }
        }
        return changed
    }

    private makeMap(Set<String> propertyNames, domain) {
        propertyNames.collectEntries { [it, domain."${it}"] }
    }

    private String getEventName(AbstractPersistenceEvent event) {
        switch (event.eventType) {
            case EventType.PreInsert:
                return "PREINSERT"
            case EventType.PostInsert:
                return "INSERT"
            case EventType.PreUpdate:
                return "UPDATE"
            case EventType.PreDelete:
                return "DELETE"
            default:
                throw new IllegalStateException("Unknown event type: ${event.eventType}")
        }
    }

    /**
     * Leans heavily on the "toString()" of a property
     * ... this feels crufty... should be tighter...
     */
    def logChanges(domain, Map newMap, Map oldMap, persistedObjectId, eventName, className) {
        // ability to enable / disable logging by runtime
        if (AuditLogListenerThreadLocal.auditLogDisabled) {
            log.debug("Auditing disabled by runtime.")
            return
        }

        Object persistedObjectVersion = getPersistedObjectVersion(domain, newMap, oldMap)
        newMap?.remove('version')
        oldMap?.remove('version')

        List auditableProperties = auditableProperties(domain)
        List ignoreList = ignoreList(domain)

        if (newMap && oldMap) {
            log.trace "There are new and old values to log"
            newMap.each { String key, val ->
                if (!isPropertyAuditable(key, auditableProperties, ignoreList)) {
                    return
                }
                if (val != oldMap[key]) {
                    GrailsDomainClass audit = getAuditLogDomainInstance(
                        actor: getActor(),
                        uri: getUri(domain),
                        className: className,
                        eventName: eventName,
                        persistedObjectId: persistedObjectId?.toString(),
                        persistedObjectVersion: persistedObjectVersion,
                        propertyName: key,
                        oldValue: conditionallyMaskAndTruncate(domain, key, oldMap[key]),
                        newValue: conditionallyMaskAndTruncate(domain, key, newMap[key]))
                    saveAuditLog(audit)
                }
            }
            return
        }
        if (newMap && verbose && !AuditLogListenerThreadLocal.auditLogNonVerbose) {
            log.trace "there are new values and logging is verbose ... "
            newMap.each { String key, val ->
                if (!isPropertyAuditable(key, auditableProperties, ignoreList)) {
                    return
                }
                def audit = getAuditLogDomainInstance(
                    actor: getActor(),
                    uri: getUri(domain),
                    className: className,
                    eventName: eventName,
                    persistedObjectId: persistedObjectId?.toString(),
                    persistedObjectVersion: persistedObjectVersion,
                    propertyName: key,
                    oldValue: null,
                    newValue: conditionallyMaskAndTruncate(domain, key, val))
                saveAuditLog(audit)
            }
            return
        }
        if (oldMap && verbose && !AuditLogListenerThreadLocal.auditLogNonVerbose) {
            log.trace "there is only an old map of values available and logging is set to verbose... "
            oldMap.each { String key, val ->
                if (!isPropertyAuditable(key, auditableProperties, ignoreList)) {
                    return
                }
                def audit = getAuditLogDomainInstance(
                    actor: getActor(),
                    uri: getUri(domain),
                    className: className,
                    eventName: eventName,
                    persistedObjectId: persistedObjectId?.toString(),
                    persistedObjectVersion: persistedObjectVersion,
                    propertyName: key,
                    oldValue: conditionallyMaskAndTruncate(domain, key, val),
                    newValue: null)
                saveAuditLog(audit)
            }
            return
        }
        // default
        log.trace "creating a basic audit logging event object."
        def audit = getAuditLogDomainInstance(
            actor: getActor(),
            uri: getUri(domain),
            className: className,
            eventName: eventName,
            persistedObjectId: persistedObjectId?.toString(),
            persistedObjectVersion: persistedObjectVersion)
        saveAuditLog(audit)

    }

    Long getPersistedObjectVersion(domain, Map newMap, Map oldMap) {
        def persistedObjectVersion = (newMap?.version) ?: oldMap?.version
        if (null == persistedObjectVersion && domain.hasProperty("version")) {
            persistedObjectVersion = domain.version
        }
        persistedObjectVersion as Long
    }

    /**
     * @param domain the auditable domain object
     * @param key property name
     * @param value the value of the property
     * @return
     */
    String conditionallyMaskAndTruncate(domain, String key, value) {
        try {
            if (value == null) {
                return null
            }
        } catch (Exception e) {
            // ignore (GPAUDITLOGGING-61)
            return
        }

        if (maskList(domain)?.contains(key)) {
            log.trace("Masking property ${key} with ${propertyMask}")
            propertyMask
        } else if (truncateLength) {
            truncate(value, truncateLength)
        } else {
            truncate(value, Integer.MAX_VALUE)
        }
    }

    String truncate(value, int max) {
        if (value == null) {
            return null
        }

        log.trace "trimming object's string representation based on ${max} characters."

        // GPAUDITLOGGING-43
        def str = null
        if (logIds) {
            if (value instanceof Collection || value instanceof Map) {
                value.each {
                    str = appendWithId(it, str)
                }
            } else {
                str = appendWithId(value, str)
            }
        } else {
            str = "$value".trim() // GPAUDITLOGGING-40
        }

        str = replaceByReplacementPatterns(str)
        (str?.length() > max) ? str?.substring(0, max) : str
    }

    private String appendWithId(obj, str) {
        // If this is a domain object, use the standard entity id which
        // allows the domain class to determine what property to use
        def objId = null
        if (obj && grailsApplication.isDomainClass(obj.class)) {
            objId = getEntityId(obj)
        } else if (obj?.respondsTo("getId")) {
            objId = obj.id
        }

        // If we have an object id, use it otherwise just fallback to toString()
        if (objId) {
            str ? "$str, [id:${objId}]$obj" : "[id:${objId}]$obj"
        } else {
            str ? "$str,$obj" : "$obj"
        }
    }

    private String replaceByReplacementPatterns(String str) {
        if (str == null) {
            return null
        }
        replacementPatterns?.each { String from, String to ->
            str = str.replace(from, to)
        }
        str
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
                } else {
                    log.trace "only one parameter on the closure I'm sending oldMap and newMap as part of a Map parameter"
                    domain."${handler}"([oldMap: oldState, newMap: newState])
                }
            } else if (oldState) {
                log.trace "sending old state into ${handler}"
                domain."${handler}"(oldState)
            } else if (newState) {
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
     * you may over-ride the definition of this closure using ... TODO allow override via config
     *
     * To debug in Config.groovy set:
     *    log4j.debug 'grails.plugins.orm.auditable.AuditLogListener'
     * or
     *    log4j.trace 'grails.plugins.orm.auditable.AuditLogListener'
     *
     * SEE: GRAILSPLUGINS-391
     */
    def saveAuditLog = { GrailsDomainClass audit ->
        audit.with {
            dateCreated = lastUpdated = new Date()
        }
        try {
            getAuditDomainClass().withNewSession {
                if (transactional) {
                    getAuditDomainClass().withTransaction {
                        audit.merge(flush: true, failOnError: true)
                    }
                } else {
                    audit.merge(flush: true, failOnError: true)
                }
            }
        }
        catch (e) {
            log.error "Failed to create ${getAuditDomainClass().name} for ${audit}: ${e.message}"
        }
    }

    /*
    Execute given Closure without verbose logging.
    You MUST wrap your CRUD by this closure if you explicitely set flush:true and
    use verbose=true (the default), otherwise you might receive Hibernate exceptions.
    */
    static withoutVerboseAuditLog = { Closure c ->
        AuditLogListenerThreadLocal.auditLogNonVerbose = true
        try {
            c.call()
        } finally {
            AuditLogListenerThreadLocal.clearAuditLogNonVerbose()
        }
    }

    /**
     * Execute given Closure without any audit logging.
     */
    static withoutAuditLog = { Closure c ->
        AuditLogListenerThreadLocal.auditLogDisabled = true
        try {
            c.call()
        } finally {
            AuditLogListenerThreadLocal.clearAuditLogDisabled()
        }
    }

    /**
     * Returns a boolean indicating if the given property should be audited or ignored.
     * If there is an auditableProperties list the property is auditable if it is in that
     * list.  Otherwise the property is auditable if is is not in the ignoreList.
     * @param fieldName The field name
     * @param auditableProperties List of auditable Properties
     * @param ignoreList The ignoreList
     * @return true if property is auditable
     */
    boolean isPropertyAuditable(def fieldName, List auditableProperties, List ignoreList) {
        if (auditableProperties) {
            return fieldName in auditableProperties
        } else if (ignoreList) {
            return !(fieldName in ignoreList)
        }

        true
    }

}
