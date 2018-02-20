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
    @Lazy
    static ConfigObject auditConfig = {
        Holders.config.grails.plugin.auditLog
    }()

    // Constructor. Static methods only
    private AuditLoggingConfigUtils() {
    }


    /** Reset the config for testing or after a dev mode Config.groovy change. */
    static synchronized void resetAuditConfig() {
        auditConfig = null
        log.trace 'reset auditLog config'
    }
}
