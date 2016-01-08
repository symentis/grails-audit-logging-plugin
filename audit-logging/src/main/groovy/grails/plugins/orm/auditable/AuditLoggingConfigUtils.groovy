/* Copyright 2006-2015 the original author or authors.
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
package grails.plugins.orm.auditable

import grails.core.GrailsApplication
import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Plugin config Helper methods.
 *
 * @author <a href='mailto:roos@symentis.com'>Robert Oschwald</a>
 */

@CompileStatic
@Slf4j
class AuditLoggingConfigUtils {

  private static ConfigObject _auditConfig
  private static GrailsApplication application

  // Constructor. Static methods only
  private AuditLoggingConfigUtils() {}

  /**
   * Set at startup by plugin.
   * @param app the application
   */
  static void setApplication(GrailsApplication app) {
    application = app
  }

  /**
   * Parse and load the auditLog configuration.
   * @return the configuration
   */
  static synchronized ConfigObject getAuditConfig() {
    if (_auditConfig == null) {
      log.trace 'Building auditLog config since there is no cached config'
      reloadAuditConfig()
    }
    _auditConfig
  }

  /**
   * For testing only.
   * @param config the config
   */
  static void setAuditConfig(ConfigObject config) {
    _auditConfig = config
  }

  /** Reset the config for testing or after a dev mode Config.groovy change. */
  static synchronized void resetAuditConfig() {
    _auditConfig = null
    log.trace 'reset auditLog config'
  }

  /**
   * Allow a secondary plugin to add config attributes.
   * @param className the name of the config class.
   */
  static synchronized void loadSecondaryConfig(String className) {
    mergeConfig auditConfig, className
    log.trace 'loaded secondary config {}', className
  }

  /** Force a reload of the auditLog configuration. */
  static void reloadAuditConfig() {
    mergeConfig ReflectionUtils.auditConfig, 'DefaultAuditLogConfig'
    log.trace 'reloaded auditLog config'
  }

  /**
   * Merge in a secondary config (provided by plugin as defaults) into the main config.
   * @param currentConfig the current configuration
   * @param className the name of the config class to load
   */
  private static void mergeConfig(ConfigObject currentConfig, String className) {
    log.trace("Merging currentConfig with $className")
    GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
    ConfigObject secondary = new ConfigSlurper(Environment.current.name).parse(classLoader.loadClass(className))
    secondary = secondary.defaultAuditLog as ConfigObject

    Collection<String> keysToDefaultEmpty = []
    findKeysToDefaultEmpty secondary, '', keysToDefaultEmpty

    ConfigObject merged = mergeConfig(currentConfig, secondary)

    // having discovered the keys that have map values (since they initially point to empty maps),
    // check them again and remove the damage done when Map values are 'flattened'
    for (String key in keysToDefaultEmpty) {
      Map value = (Map)ReflectionUtils.getConfigProperty(key, merged)
      for (Iterator<Map.Entry> iter = value.entrySet().iterator(); iter.hasNext();) {
        def entry = iter.next()
        if (entry.value instanceof Map) {
          iter.remove()
        }
      }
    }

    _auditConfig = ReflectionUtils.auditConfig = merged
  }

  /**
   * Merge two configs together. The order is important if <code>secondary</code> is not null then
   * start with that and merge the main config on top of that. This lets the <code>secondary</code>
   * config act as default values but let user-supplied values in the main config override them.
   *
   * @param currentConfig the main config, starting from Config.groovy
   * @param secondary new default values
   * @return the merged configs
   */
  private static ConfigObject mergeConfig(ConfigObject currentConfig, ConfigObject secondary) {
    log.trace("Merging secondary config on top of currentConfig")
    (secondary ?: new ConfigObject()).merge(currentConfig ?: new ConfigObject()) as ConfigObject
  }

  /**
   * Given an unmodified config map with defaults, loop through the keys looking for values that are initially
   * empty maps. This will be used after merging to remove map values that cause problems by being included both as
   * the result from the ConfigSlurper (which is correct) and as a "flattened" maps which confuse Audit Logging.
   * @param m the config map
   * @param fullPath the path to this config map, e.g. 'grails.plugin.auditLog
   * @param keysToDefaultEmpty a collection of key names to add to
   */
  private static void findKeysToDefaultEmpty(Map m, String fullPath, Collection keysToDefaultEmpty) {
    m.each { k, v ->
      if (v instanceof Map) {
        if (v) {
          // recurse
          findKeysToDefaultEmpty((Map)v, fullPath + '.' + k, keysToDefaultEmpty)
        }
        else {
          // since it's an empty map, capture its path for the cleanup pass
          keysToDefaultEmpty << (fullPath + '.' + k).substring(1)
        }
      }
    }
  }
}
