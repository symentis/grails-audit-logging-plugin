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
package org.codehaus.groovy.grails.plugins.orm.auditable;

import grails.util.Environment;
import groovy.lang.GroovyClassLoader;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * Helper methods.
 */
public final class AuditLoggingUtils {

	public static final String AUDIT_CONFIG_DOMAIN_CLASS_NAME = "auditDomainClassName";

	private static ConfigObject _auditConfig;
	private static GrailsApplication application;

	private AuditLoggingUtils() {
		// static only
	}

	/**
	 * Set at startup by plugin.
	 * @param app the application
	 */
	public static void setApplication(GrailsApplication app) {
		application = app;
		initializeContext();
	}

	/**
	 * Parse and load the audit configuration.
	 * @return the configuration
	 */
	public static synchronized ConfigObject getAuditConfig() {
		if (_auditConfig == null) {
			reloadAuditConfig();
		}

		return _auditConfig;
	}

	/**
	 * For testing only.
	 * @param config the config
	 */
	public static void setAuditConfig(ConfigObject config) {
		_auditConfig = config;
	}

	/**
	 * Reset the config for testing or after a dev mode Config.groovy change.
	 */
	public static synchronized void resetAuditConfig() {
		_auditConfig = null;
	}

	/**
	 * Allow a secondary plugin to add config attributes.
	 * @param className the name of the config class.
	 */
	public static synchronized void loadSecondaryConfig(final String className) {
		mergeConfig(getAuditConfig(), className);
	}

	/**
	 * Force a reload of the audit configuration.
	 */
	public static void reloadAuditConfig() {
		mergeConfig(ReflectionUtils.getAuditConfig(), "DefaultAuditConfig");
	}

	/**
	 * Merge in a secondary config (provided by a plugin as defaults) into the main config.
	 * @param currentConfig the current configuration
	 * @param className the name of the config class to load
	 */
	private static void mergeConfig(final ConfigObject currentConfig, final String className) {
		GroovyClassLoader classLoader = new GroovyClassLoader(AuditLoggingUtils.class.getClassLoader());
		ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
		ConfigObject secondaryConfig;
		try {
			secondaryConfig = slurper.parse(classLoader.loadClass(className));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		_auditConfig = mergeConfig(currentConfig, (ConfigObject)secondaryConfig.getProperty("auditLog"));
		ReflectionUtils.setAuditConfig(_auditConfig);
	}

	/**
	 * Merge two configs together. The order is important; if <code>secondary</code> is not null then
	 * start with that and merge the main config on top of that. This lets the <code>secondary</code>
	 * config act as default values but let user-supplied values in the main config override them.
	 *
	 * @param currentConfig the main config, starting from Config.groovy
	 * @param secondary new default values
	 * @return the merged configs
	 */
	private static ConfigObject mergeConfig(final ConfigObject currentConfig, final ConfigObject secondary) {
		ConfigObject config = new ConfigObject();
		if (secondary == null) {
			if (currentConfig != null) {
				config.putAll(currentConfig);
			}
		}
		else {
			if (currentConfig == null) {
				config.putAll(secondary);
			}
			else {
				config.putAll(secondary.merge(currentConfig));
			}
		}
		return config;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getBean(final String name) {
		return (T)application.getMainContext().getBean(name);
	}

	/**
	 * Called each time doWithApplicationContext() is invoked, so it's important to reset
	 * to default values when running integration and functional tests together.
	 */
	private static void initializeContext() {
		// nothing to do for now.
	}

}
