package grails.plugins.orm.auditable

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.model.PersistentEntity

import javax.persistence.Transient

/**
 * Domain classes should implement this trait to provide auditing support
 */
@CompileStatic
trait Auditable<D> extends GormEntity<D> {
    /**
     * If false, this entity will not be logged
     */
    @Transient
    boolean isAuditLogEnabled(AuditEventType eventType) {
        !AuditLogContext.context.disabled
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
    Set<AuditEventType> getLogVerboseEvents() {
        if (AuditLogContext.context.verbose) {
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
     * @return return the URI for audit logging, by default this is null
     */
    @Transient
    String getLogURI() {
        null
    }

    /**
     * @return blacklist properties that should not be logged including those in context or by default.
     */
    @Transient
    Set<String> getLogExcluded() {
        (AuditLogContext.context.excluded ?: Collections.EMPTY_SET) as Set<String>
    }

    /**
     * @return whitelist properties that should be logged. If null, all attributes are logged. This overrides any excludes.
     */
    @Transient
    Set<String> getLogIncluded() {
        AuditLogContext.context.included as Set<String>
    }

    /**
     * @return set of properties to mask when logging
     */
    @Transient
    Set<String> getLogMaskProperties() {
        (AuditLogContext.context.mask ?: Collections.EMPTY_SET) as Set<String>
    }

    /**
     * @return list of event types to ignore, by default all events are logged
     */
    @Transient
    Set<AuditEventType> getLogIgnoreEvents() {
        (AuditLogContext.context.ignoreEvents ?: Collections.EMPTY_SET) as Set<AuditEventType>
    }

    /**
     * @return return the current username if available or SYS by default
     */
    @Transient
    String getLogCurrentUserName() {
        AuditLogContext.context.defaultActor ?: 'SYS'
    }

    /**
     * @return auditable property names for this domain class resolving any includes and excludes
     */
    @Transient
    Set<String> getAuditablePropertyNames() {
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
    @SuppressWarnings("GroovyAssignabilityCheck")
    Set<String> getAuditableDirtyPropertyNames() {
        auditablePropertyNames.intersect(listDirtyPropertyNames() as Set<String>) as Set<String>
    }

    /**
     * @return returns the id of the object by default, can override to return a natural key
     */
    @Transient
    String getLogEntityId() {
        convertLoggedPropertyToString("id", ident())
    }

    /**
     * Domain classes can override to apply special formatting on a per-property basis
     *
     * @param propertyName
     * @param value
     * @return
     */
    String convertLoggedPropertyToString(String propertyName, Object value) {
        if (value instanceof Enum) {
            return ((Enum)value).name()
        }
        if (value instanceof Auditable) {
            return "[id:${((Auditable)value).logEntityId}]$value"
        }
        if (value instanceof GormEntity) {
            return "[id:${((GormEntity)value).ident()}]$value"
        }
        if (logAssociatedIds && value instanceof Collection) {
            return ((Collection)value).collect {
                convertLoggedPropertyToString(propertyName, it)
            }.join(", ")
        }

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