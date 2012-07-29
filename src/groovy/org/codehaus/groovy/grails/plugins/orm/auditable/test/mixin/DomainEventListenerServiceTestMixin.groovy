package org.codehaus.groovy.grails.plugins.orm.auditable.test.mixin

import grails.test.mixin.domain.DomainClassUnitTestMixin
import org.junit.BeforeClass

/**
 * @author Shawn Hartsock
 */
class DomainEventListenerServiceTestMixin extends DomainClassUnitTestMixin {
    @BeforeClass
    static void initializeDatastoreImplementation() {
        if (applicationContext == null) {
            super.initGrailsApplication()
        }
        def domainEventListenerService = applicationContext.getBean('domainEventListenerService')
        applicationContext.addApplicationListener domainEventListenerService
    }
}