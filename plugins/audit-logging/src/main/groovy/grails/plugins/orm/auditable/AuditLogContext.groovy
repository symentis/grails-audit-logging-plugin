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

import org.springframework.core.NamedThreadLocal

/**
 * Allows runtime overriding of any audit logging configurable property
 */
class AuditLogContext {
    static final ThreadLocal<Map> auditLogConfig = new NamedThreadLocal<Map>("auditLog.context")

    /**
     * Overrides most 'grails.plugin.auditLog' configuration properties for execution of the given block
     *
     * For example:
     *
     * withConfig(logIncludes: ['foo', 'bar']) { ... }* withConfig(logFullClassName: false) { ... }* withConfig(verbose: false) { ... }*
     */
    static withConfig(Map config, Closure block) {
        try {
            auditLogConfig.set(mergeConfig(config))
            block.call()
        }
        finally {
            auditLogConfig.remove()
        }
    }

    /**
     * Disable verbose audit logging for anything within this block
     */
    static withoutVerboseAuditLog(Closure block) {
        withConfig([verbose: false], block)
    }

    /**
     * Disable audit logging for this block
     */
    static withoutAuditLog(Closure block) {
        withConfig([disabled: true], block)
    }

    /**
     * @return the current context
     */
    static Map getContext() {
        mergeConfig(auditLogConfig.get())
    }

    private static Map mergeConfig(Map localConfig) {
        localConfig ? AuditLoggingConfigUtils.auditConfig + localConfig : AuditLoggingConfigUtils.auditConfig
    }
}
