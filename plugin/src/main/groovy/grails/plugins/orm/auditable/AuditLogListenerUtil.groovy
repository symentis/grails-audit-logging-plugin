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

import grails.gorm.validation.ConstrainedProperty
import grails.gorm.validation.PersistentEntityValidator
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.ToMany

/**
 * Provides AuditLogListener utilities that
 * can be used as either templates for your own extensions to
 * the plugin or as default utilities.
 */
@Slf4j
@CompileStatic
class AuditLogListenerUtil {
    /**
     * @param domain the domain instance
     * @return configured AuditLogEvent instance
     */
    static GormEntity createAuditLogDomainInstance(Map params) {
        Class<GormEntity> clazz = getAuditDomainClass()
        log.debug 'clazz: {}', clazz
        log.debug 'params: {}', params
        clazz.newInstance(params)
    }

    /**
     *
     * @param domain the domain instance
     * @return configured AuditLogEvent class
     */
    static Class<GormEntity> getAuditDomainClass() {
        String auditLogClassName = AuditLoggingConfigUtils.auditConfig.getProperty('auditDomainClassName') as String
        if (!auditLogClassName) {
            throw new IllegalArgumentException("grails.plugin.auditLog.auditDomainClassName could not be found in application.groovy. Have you performed 'grails audit-quickstart' after installation?")
        }

        Class domainClass = Class.forName(auditLogClassName)
        if (!GormEntity.isAssignableFrom(domainClass)) {
            throw new IllegalArgumentException("The specified audit domain class $auditLogClassName is not a GORM entity")
        }

        domainClass as Class<GormEntity>
    }

    /**
     * Get the original or persistent value for the given domain.property. This method includes
     * some special case handling for hasMany properties, which don't follow normal rules.
     *
     * Default value of AuditLogConfig.usePersistentDirtyPropertyValue is 'true'. If that does not
     * work (i.e., original value is always null), then use 'false' instead.
     *
     * @see http://gorm.grails.org/6.1.x/api/org/grails/datastore/gorm/GormEntity.html#getPersistentValue(java.lang.String)
     * @see http://gorm.grails.org/6.1.x/api/org/grails/datastore/mapping/dirty/checking/DirtyCheckable.html#getOriginalValue(java.lang.String)
     */
    static Object getOriginalValue(Auditable domain, String propertyName) {
        PersistentEntity entity = getPersistentEntity(domain)
        PersistentProperty property = entity.getPropertyByName(propertyName)
        ConfigObject config = AuditLoggingConfigUtils.getAuditConfig()
        boolean usePersistentDirtyPropertyValues = config.getProperty("usePersistentDirtyPropertyValues")
        if (usePersistentDirtyPropertyValues) {
            property instanceof ToMany ? "N/A" : ((GormEntity)domain).getPersistentValue(propertyName)
        }
        else {
            property instanceof ToMany ? "N/A" : ((GormEntity)domain).getOriginalValue(propertyName)
        }
    }

    /**
     * Helper method to make a map of the current property values
     *
     * @param propertyNames
     * @param domain
     * @return
     */
    static Map<String, Object> makeMap(Collection<String> propertyNames, Auditable domain) {
        propertyNames.collectEntries { [it, domain.metaClass.getProperty(domain, it)] }
    }

    /**
     * Return the grails domain class for the given domain object.
     *
     * @param domain the domain instance
     */
    static PersistentEntity getPersistentEntity(domain) {
        Holders.grailsApplication.mappingContext.getPersistentEntity(domain.getClass().name)
    }

    /**
     * @param domain the auditable domain object
     * @param propertyName property name
     * @param value the value of the property
     * @return
     */
    static String conditionallyMaskAndTruncate(Auditable domain, String propertyName, String value, Integer maxLength) {
        if (value == null) {
            return null
        }

        // Always trim any space
        value = value.trim()

        if (domain.logMaskProperties && domain.logMaskProperties.contains(propertyName)) {
            return AuditLogContext.context.propertyMask as String ?: '********'
        }
        if (maxLength && value.length() > maxLength) {
            return value.substring(0, maxLength)
        }

        value
    }

    /**
     * Determine the truncateLength based on the configured truncateLength and the actual auditDomainClass maxSize constraint for newValue.
     */
    static Integer determineTruncateLength() {
        String confAuditDomainClassName = AuditLoggingConfigUtils.auditConfig.getProperty('auditDomainClassName')
        if (!confAuditDomainClassName) {
            throw new IllegalArgumentException("Please configure auditLog.auditDomainClassName in Config.groovy")
        }

        MappingContext mappingContext = Holders.grailsApplication.mappingContext
        PersistentEntity auditPersistentEntity = mappingContext.getPersistentEntity(confAuditDomainClassName)
        if (!auditPersistentEntity) {
            throw new IllegalArgumentException("The configured audit logging domain class '$confAuditDomainClassName' is not a domain class")
        }

        // Get the property constraints
        PersistentEntityValidator entityValidator = mappingContext.getEntityValidator(auditPersistentEntity) as PersistentEntityValidator
        Map<String, ConstrainedProperty> constrainedProperties = (entityValidator?.constrainedProperties ?: [:]) as Map<String, ConstrainedProperty>

        // The configured length is the min size of oldValue, newValue, or configured truncateLength
        Integer oldValueMaxSize = constrainedProperties['oldValue'].maxSize ?: 255
        Integer newValueMaxSize = constrainedProperties['newValue'].maxSize ?: 255
        Integer maxSize = Math.min(oldValueMaxSize, newValueMaxSize)
        Integer configuredTruncateLength = (AuditLoggingConfigUtils.auditConfig.getProperty('truncateLength') ?: Integer.MAX_VALUE) as Integer

        Math.min(maxSize, configuredTruncateLength)
    }
}
