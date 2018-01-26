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

import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
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
        PersistentEntity persistentEntity = getPersistentEntity(auditLogClassName)
        if (!persistentEntity) {
            throw new IllegalArgumentException("The specified user domain class '$auditLogClassName' is not a domain class")
        }
        persistentEntity.javaClass
    }

    /**
     * Get the persistent value for the given domain.property. This method includes
     * some special case handling for hasMany properties, which don't follow normal rules.
     */
    static Object getPersistentValue(Auditable domain, String propertyName) {
        PersistentEntity entity = getPersistentEntity(domain.class.name)
        PersistentProperty property = entity.getPropertyByName(propertyName)
        property instanceof ToMany ? "N/A" : domain.getPersistentValue(propertyName)
    }

    /**
     * Helper method to make a map of the current property values
     *
     * @param propertyNames
     * @param domain
     * @return
     */
    static Map<String, Object> makeMap(Set<String> propertyNames, Auditable domain) {
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
    static String conditionallyMaskAndTruncate(Auditable domain, String propertyName, String value) {
        if (!value) {
            return null
        }
        // Always trim any space
        value = value.trim()

        if (domain.logMaskProperties && domain.logMaskProperties.contains(propertyName)) {
            return propertyName
        }
        if (domain.logMaxLength && value.length() > domain.logMaxLength) {
            return value.substring(0, domain.logMaxLength)
        }

        value
    }

    /**
     * The original getActor method transplanted to the utility class as
     * a closure. The closure takes two arguments one a RequestAttributes object
     * the other is an HttpSession object.
     *
     * These are strongly typed here for the purpose of documentation.
     */
    /*
    static Closure actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
        def actor = request?.remoteUser

        if (!actor && request.userPrincipal) {
            actor = request.userPrincipal.getName()
        }

        if (!actor && delegate.sessionAttribute) {
            log.debug "configured with session attribute ${delegate.sessionAttribute} attempting to resolve"
            actor = session?.getAttribute(delegate.sessionAttribute)
            log.trace "session.getAttribute('${delegate.sessionAttribute}') returned '${actor}'"
        }

        if (!actor && delegate.actorKey) {
            log.debug "configured with actorKey ${actorKey} resolve using request attributes "
            actor = resolve(attr, delegate.actorKey, delegate.log)
            log.trace "resolve on ${delegate.actorKey} returned '${actor}'"
        }
        return actor
    }
    */

    /**
     * Attempt to resolve the attribute from the RequestAttributes object passed
     * to it. This did not always work for people since on some designs
     * users are not *always* logged in to the system.
     *
     * It was originally assumed that if you *were* generating events then you
     * were logged in so the problem of null session.user did not come up in testing.
     */
    /*
    static resolve(attr, str, log) {
        def tokens = str?.split("\\.")
        def res = attr
        log.trace "resolving recursively ${str} from request attributes..."
        tokens.each {
            log.trace "\t\t ${it}"
            try {
                if (res) {
                    res = res."${it}"
                }
            }
            catch (MissingPropertyException ignored) {
                log.debug """\
AuditLogListener:

You attempted to configure a request attribute named '${str}' and
${AuditLogListenerUtil.class} attempted to dynamically resolve this from
the servlet context session attributes but failed!

Last attribute resolved class ${res?.getClass()} value ${res}
"""
                res = null
            }
        }
        return res?.toString() ?: null
    }
    */
}
