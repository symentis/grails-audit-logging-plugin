/* Copyright 2006-2015 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.orm.auditable

import grails.util.Holders
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Helper methods in Groovy.
 *
 * Taken from Spring-Security-Core 2.x
 */
class ReflectionUtils {

	// Configured Entity class
	private static Class<?> AuditLogEventClazz

	// set at startup
	static GrailsApplication application

	private ReflectionUtils() {
		// static only
	}

	static getConfigProperty(String name) {
		def value = AuditLoggingUtils.getAuditConfig()
		for (String part in name.split('\\.')) {
			value = value."$part"
		}
		value
	}

	static void setConfigProperty(String name, value) {
		def config = AuditLoggingUtils.getAuditConfig()
		def parts = name.split('\\.') as List
		name = parts.remove(parts.size() - 1)

		for (String part in parts) {
			config = config."$part"
		}

		config."$name" = value
	}

	static List asList(o) { o ? o as List : [] }

	private static GrailsApplication getApplication() {
		if (!application) {
			application = Holders.grailsApplication
		}
		application
	}

	static ConfigObject getAuditConfig() {
		def grailsConfig = getApplication().config
		grailsConfig.auditLog
	}

	static void setAuditConfig(ConfigObject c) { getApplication().config.auditLog = c }

	static Class<?> getAuditClass() {
		String domainClassName = (String)getAuditConfig()."${AuditLoggingUtils.AUDIT_CONFIG_DOMAIN_CLASS_NAME}"
		if (domainClassName == null) {
			throw new IllegalArgumentException("You must configure auditLog.${AuditLoggingUtils.AUDIT_CONFIG_DOMAIN_CLASS_NAME}. Have you performed grails audit-quickstart?")
		}
		if (domainClassName && !AuditLogEventClazz) {
			AuditLogEventClazz = Holders.grailsApplication.getDomainClass(domainClassName)?.clazz
		}
		if (!AuditLogEventClazz) {
			throw new IllegalStateException("Can't find configured AuditLog domain: $domainClassName")
		}
		AuditLogEventClazz
	}
}
