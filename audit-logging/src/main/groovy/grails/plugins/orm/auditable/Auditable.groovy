package grails.plugins.orm.auditable

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

import javax.persistence.Transient

/**
 * Domain classes should implement this trait to provide auditing support
 */
@CompileStatic
trait Auditable extends GormEntity {
    static final Integer MAX_LENGTH = 255

    /**
     * @return by default anything that implements this trait is auditable. This can be overriden to make a
     * runtime decision on whether to audit this entity.
     */
    @Transient
    boolean isAuditLogEnabled(AuditEventType eventType) {
        true
    }

    @Transient
    boolean isLogAssociatedIds() {
        AuditLoggingConfigUtils.auditConfig.getProperty('logIds') as boolean
    }

    /**
     * @return set of event types for which to log verbosely
     */
    @Transient
    Set<AuditEventType> getLogVerbose() {
        if (AuditLoggingConfigUtils.auditConfig.getProperty('verbose')) {
            AuditEventType.values() as Set<AuditEventType>
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
        GrailsNameUtils.getShortName(getClass())
    }

    /**
     * @return returns the id of the object by default, can override to return a natural key
     */
    @Transient
    String getLogEntityId() {
        ident() as String
    }

    /**
     * @return return the URI for audit logging, by default this is null
     */
    @Transient
    String getLogURI() {
        null
    }

    /**
     * @return excluded attributes, this will override anything specifically included
     */
    @Transient
    Set<String> getLogExcluded() {
        ['version', 'lastUpdated', 'dateCreated'] as Set<String>
    }

    /**
     * @return attributes included in logging minus any that are specifically excluded, null for all attributes
     */
    @Transient
    Set<String> getLogIncluded() {
        Collections.EMPTY_SET
    }

    /**
     * @return set of properties to mask when logging
     */
    @Transient
    Set<String> getLogMaskProperties() {
        ['password'] as Set<String>
    }

    /**
     * @return list of event types to ignore, by default all events are logged
     */
    @Transient
    Set<AuditEventType> getLogIgnoreEvents() {
        Collections.EMPTY_SET
    }

    /**
     * @return default maximum length for
     */
    @Transient
    Integer getLogMaxLength() {
        MAX_LENGTH
    }

    /**
     * @return return the current username if available or SYS by default
     */
    @Transient
    String getLogCurrentUserName() {
        'SYS'
    }

    /*
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
    */

    /**
     * Override to control property formatting, by default just invoke toString
     *
     * @param propertyName
     * @param value
     * @return
     */
    String logPropertyToString(String propertyName, Object value) {
        if (value instanceof Enum) {
            return ((Enum)value).name()
        }

        // TODO - Implement for collection
        if (value instanceof Collection) {
            return value = '[...]'
        }

        value ? value.toString() : null
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