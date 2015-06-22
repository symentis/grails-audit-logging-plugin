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
package org.codehaus.groovy.grails.plugins.orm.auditable

import grails.util.Holders

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.datastore.mapping.engine.event.EventType;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

import javax.servlet.http.HttpSession

/**
 * Provides simple AuditLogListener utilities that
 * can be used as either templates for your own extensions to
 * the plugin or as default utilities.
 */
class AuditLogListenerUtil {
    /**
     * Returns true for auditable entities, false otherwise.
     *
     * Domain classes can use the 'isAuditable' attribute to provide a closure
     * that will be called in order to determine instance level auditability. For example,
     * a domain class may only be audited after it becomes Final and not while Pending.
     */
    static boolean isAuditableEntity(domain, String eventName) {
        // Null or false is not auditable
        def auditable = getAuditable(domain)
        if (!auditable) {
            return false
        }

        // If we have a map, see if we have an instance-level closure to check
        if (auditable instanceof Map) {
            def map = auditable as Map
            if (map?.containsKey('isAuditable')) {
                return map.isAuditable.call(eventName, domain)
            }
        }

        // Anything that get's this far is auditable
        return true
    }
	
	static isStampable(domain,EventType eventType) {
		boolean stampable =  isStampable(eventType)
		if(stampable){
			def cpf = ClassPropertyFetcher.forClass(domain.class)
			stampable = cpf.getPropertyValue('stampable')
		}
		stampable
	}
	
	static isStampable(EventType eventType){
		eventType == EventType.PreDelete || eventType == EventType.PreUpdate || eventType == EventType.PreInsert
	}
	
	
    /**
     * The static auditable attribute for the given domain class or null if none exists
     */
    static getAuditable(domain) {
        def cpf = ClassPropertyFetcher.forClass(domain.class)
        cpf.getPropertyValue('auditable')
    }

    /**
     * If auditable is defined as a Map, return it otherwise return null
     */
    static Map getAuditableMap(domain) {
        def auditable = getAuditable(domain)
        auditable && auditable instanceof Map ? auditable as Map : null
    }

    /**
     * Get the Id to display for this entity when logging. Domain classes can override the property
     * used by supplying a [entityId] attribute in the auditable Map.
     *
     * @param event
     * @return String key
     */
    static String getEntityId(domain) {
        // If we have a display key, allow override of what shows as the entity id
        Map auditableMap = getAuditableMap(domain)
        if (auditableMap?.containsKey('entityId')) {
            def entityId = auditableMap.entityId

            if (entityId instanceof Closure) {
                return entityId.call(domain) as String
            }
            else if (entityId instanceof Collection) {
                return entityId.collect { getIdProperty(domain, it) }.join("|")
            }
            else if (entityId instanceof String) {
                return getIdProperty(domain, entityId)
            }
        }

        // Use the identifier property if this is a domain class or just 'id' if not
        def identifier = getDomainClass(domain)?.identifier?.name ?: 'id'
        domain."${identifier}" as String
    }

    private static String getIdProperty(domain, property) {
        def val = domain."${property}"
        if (val instanceof Enum) {
            return val.name()
        }
        if (getDomainClass(val)) {
            return getEntityId(val)
        }
        val as String
    }

    /**
     * Return the grails domain class for the given domain object.
     *
     * @param domain the domain instance
     */
    static GrailsDomainClass getDomainClass(domain) {
        if (domain && Holders.grailsApplication.isDomainClass(domain.class)) {
            Holders.grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, domain.class.name) as GrailsDomainClass
        }
        else {
            null
        }
    }

    /**
     * The original getActor method transplanted to the utility class as
     * a closure. The closure takes two arguments one a RequestAttributes object
     * the other is an HttpSession object.
     *
     * These are strongly typed here for the purpose of documentation.
     */
    static Closure actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
        def actor = request?.remoteUser

        if(!actor && request.userPrincipal) {
            actor = request.userPrincipal.getName()
        }

        if(!actor && delegate.sessionAttribute) {
            log.debug "configured with session attribute ${delegate.sessionAttribute} attempting to resolve"
            actor = session?.getAttribute(delegate.sessionAttribute)
            log.trace "session.getAttribute('${delegate.sessionAttribute}') returned '${actor}'"
        }

        if(!actor && delegate.actorKey) {
            log.debug "configured with actorKey ${actorKey} resolve using request attributes "
            actor = resolve(attr, delegate.actorKey, delegate.log)
            log.trace "resolve on ${delegate.actorKey} returned '${actor}'"
        }
        return actor
    }

    /**
     * Attempt to resolve the attribute from the RequestAttributes object passed
     * to it. This did not always work for people since on some designs
     * users are not *always* logged in to the system.
     *
     * It was originally assumed that if you *were* generating events then you
     * were logged in so the problem of null session.user did not come up in testing.
     */
    static resolve(attr, str, log) {
        def tokens = str?.split("\\.")
        def res = attr
        log.trace "resolving recursively ${str} from request attributes..."
        tokens.each {
            log.trace "\t\t ${it}"
            try {
                if(res) {
                    res = res."${it}"
                }
            } catch(MissingPropertyException ignored) {
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
}
