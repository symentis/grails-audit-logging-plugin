package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.commons.ApplicationHolder
import javax.annotation.PostConstruct

/**
 * This bean looks through all domain classes on its creation
 * and holds on to references to the 'auditable' domain classes
 * it finds in the application
 */
class AuditableRegistryService extends AuditableRegistry {
    static transactional = false

    @PostConstruct
    void init() {
        def application = ApplicationHolder.application
        for (dc in application.domainClasses) {
            isAuditable(dc) ?
                this.register(dc) : null
        }
    }
}
