package grails.plugins.orm.auditable

import grails.plugins.orm.auditable.resolvers.AuditRequestResolver
import grails.util.GrailsNameUtils
import grails.util.Holders
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException

import javax.persistence.Transient

import static grails.plugins.orm.auditable.AuditLogListenerUtil.makeMap
/**
 * Domain classes should implement this trait to provide auditing support
 */
@CompileStatic
trait Auditable {
    final static Logger log = LoggerFactory.getLogger(Auditable.class)

    /**
     * If false, this entity will not be logged
     */
    @Transient
    boolean isAuditable(AuditEventType eventType) {
        true
    }

    /**
     * Enable logging of associated id changes in the format: "[id:<objId>]objDetails".
     */
    @Transient
    boolean isLogAssociatedIds() {
        AuditLogContext.context.logIds as boolean
    }

    /**
     * @return set of event types for which to log verbosely
     */
    @Transient
    Collection<AuditEventType> getLogVerboseEvents() {
        if (AuditLogContext.context.verbose && !AuditLogContext.context.verboseEvents) {
            AuditEventType.values() as Set<AuditEventType>
        }
        else if (AuditLogContext.context.verboseEvents) {
            AuditLogContext.context.verboseEvents as Set<AuditEventType>
        }
        else {
            Collections.EMPTY_SET
        }
    }

    /**
     * @return the class name to log for this domain object, defaults to the capitilized short name
     */
    @Transient
    String getLogClassName() {
        AuditLogContext.context.logFullClassName ? getClass().name : GrailsNameUtils.getShortName(getClass())
    }

    /**
     * @return blacklist properties that should not be logged including those in context or by default.
     */
    @Transient
    Collection<String> getLogExcluded() {
        (AuditLogContext.context.excluded ?: Collections.EMPTY_SET) as Set<String>
    }

    /**
     * @return whitelist properties that should be logged. If null, all attributes are logged. This overrides any excludes.
     */
    @Transient
    Collection<String> getLogIncluded() {
        AuditLogContext.context.included as Set<String>
    }

    /**
     * @return set of properties to mask when logging
     */
    @Transient
    Collection<String> getLogMaskProperties() {
        (AuditLogContext.context.mask ?: Collections.EMPTY_SET) as Set<String>
    }

    /**
     * @return list of event types to ignore, by default all events are logged
     */
    @Transient
    Collection<AuditEventType> getLogIgnoreEvents() {
        (AuditLogContext.context.ignoreEvents ?: Collections.EMPTY_SET) as Set<AuditEventType>
    }

    /**
     * @return return the URI for audit logging, by default this is null
     */
    @Transient
    String getLogURI() {
        try {
            // Using holders here since we can't inject and need to defer to allow subclasses to override the resolver
            Holders.applicationContext.getBean(AuditRequestResolver)?.currentURI
        } catch (NoSuchBeanDefinitionException ignore){
            // Bean may not be initialized. See #203
            log.debug("No AuditRequestResolver bean found in getLogURI()")
            return null
        }

    }

    /**
     * @return return the current username if available or SYS by default
     */
    @Transient
    String getLogCurrentUserName() {
        try {
            // Using holders here since we can't inject and need to defer to allow subclasses to override the resolver
            Holders.applicationContext.getBean(AuditRequestResolver)?.currentActor ?: 'N/A'
        } catch (NoSuchBeanDefinitionException ignore){
            // Bean may not be initialized. See #203
            log.debug("No AuditRequestResolver bean found in getLogCurrentUserName()")
            return 'N/A'
        }
    }

    /**
     * @return auditable property names for this domain class resolving any includes and excludes
     */
    @Transient
    Collection<String> getAuditablePropertyNames() {
        PersistentEntity entity = getClass().invokeMethod("getGormPersistentEntity", null) as PersistentEntity

        // Start with all persistent properties
        Set<String> persistentProperties = entity.getPersistentProperties()*.name as Set<String>
        Set<String> loggedProperties = persistentProperties

        // If given a whitelist, only log the properties specifically in that list
        if (logIncluded != null) {
            loggedProperties = logIncluded as Set<String>
        }
        else if(logExcluded) {
            loggedProperties -= logExcluded
        }

        // Intersect with the persistent properties to filter to just properties on this domain
        loggedProperties.intersect(persistentProperties as Iterable) as Set<String>
    }

    /**
     * @return return any dirty properties that are flagged as auditable
     */
    @Transient
    Collection<String> getAuditableDirtyPropertyNames() {
        Collection<String> dirtyProperties = Collections.EMPTY_LIST
        if (this instanceof GormEntity) {
            dirtyProperties = ((DirtyCheckable)this).listDirtyPropertyNames()
        }
        auditablePropertyNames.intersect(dirtyProperties) as Set<String>
    }

    /**
     * @return returns the id of the object by default, can override to return a natural key
     */
    @Transient
    String getLogEntityId() {
        log.debug("getLogEntityId()")
        if (this instanceof GormEntity) {
            PersistentEntity persistentEntity = (PersistentEntity) getClass().invokeMethod("getGormPersistentEntity", null)
            log.debug("    this instanceof GormEntity")
            if (persistentEntity.identity != null) {
                return convertLoggedPropertyToString("id", ((GormEntity)this).ident())
            }
            else {
                // Fetch composite ID values
                PersistentProperty[] idProperties = persistentEntity.compositeIdentity
                Map<String, Object> map = makeMap(idProperties*.name, this)

                // Build a string representation of this class that looks like: [<persistent id property>:<value or id>]
                StringBuilder stringBuilder = new StringBuilder()
                stringBuilder.append("[")
                map.eachWithIndex { Map.Entry<String, Object> entry, int i ->
                    stringBuilder.append(entry.key)
                    stringBuilder.append(":")
                    switch (entry.value) {
                        case Auditable:
                            stringBuilder.append(((Auditable) entry.value).logEntityId)
                            break
                        case GormEntity:
                            stringBuilder.append(((GormEntity) entry.value).ident().toString())
                            break
                        default:
                            stringBuilder.append(convertLoggedPropertyToString(entry.key, entry.value))
                            break
                    }
                    if (i != (map.size() - 1)) {
                        stringBuilder.append(", ")
                    }
                }
                stringBuilder.append("]")
                return stringBuilder.toString()
            }
        }
        if (this.respondsTo("getId")) {
            log.debug("    this respondsTo getId")
            return convertLoggedPropertyToString("id", this.invokeMethod("getId", null))
        }

        throw new IllegalStateException("Could not determine the Id for ${getClass().name}, override getLogEntityId() to specify")
    }

    /**
     * Domain classes can override to apply special formatting on a per-property basis
     *
     * @param propertyName
     * @param value
     * @return
     */
    String convertLoggedPropertyToString(String propertyName, Object value) {
        log.debug("convertLoggedPropertyToString(propertyName: ${propertyName}, value: ${value})")
        if (value instanceof Enum) {
            log.debug("    value instanceof Enum")
            return ((Enum)value).name()
        }
        if (value instanceof Auditable) {
            log.debug("    value instanceof Auditable")
            return "[id:${((Auditable)value).logEntityId}]$value"
        }
        if (value instanceof GormEntity) {
            log.debug("    value instanceof GormEntity")
            return "[id:${((GormEntity)value).ident()}]$value"
        }
        if (value instanceof Collection) {
            log.debug("    value instanceof Collection")
            if (logAssociatedIds) {
                return ((Collection)value).collect {
                    convertLoggedPropertyToString(propertyName, it)
                }.join(", ")
            }
            else {
                return "N/A"
            }
        }

        log.debug("    value.toString(): ${value?.toString()}")
        value?.toString()
    }

    /**
     * Stub method to modify or override properties prior to save. This is called after all values are set but prior
     * to actually saving.
     *
     * @param auditEntity this will be an instance of grails.plugin.auditLog.auditDomainClassName
     * @return true to continue; false will veto the save of the audit entry
     */
    boolean beforeSaveLog(Object auditEntity) {
        true
    }
}
