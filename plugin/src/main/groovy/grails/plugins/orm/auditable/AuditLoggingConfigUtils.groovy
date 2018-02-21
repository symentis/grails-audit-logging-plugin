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

import grails.util.Holders
import groovy.util.logging.Slf4j

/**
 * Plugin config Helper methods.
 *
 * @author <a href='mailto:roos@symentis.com'>Robert Oschwald</a>
 */
@Slf4j
class AuditLoggingConfigUtils {
    static @Lazy
    ConfigObject auditConfig = { Holders.config.grails.plugin.auditLog }()

    /** Force a reload of the auditLog configuration. */
    static void reloadAuditConfig() {
        auditConfig = null
        log.trace 'reloaded auditLog config'
    }
}
