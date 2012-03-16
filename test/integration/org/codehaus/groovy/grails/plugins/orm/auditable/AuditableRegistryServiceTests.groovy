package org.codehaus.groovy.grails.plugins.orm.auditable



import grails.test.mixin.*
import org.junit.*
import org.codehaus.groovy.grails.plugins.orm.auditable.testing.AbstractHibernateTests

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(AuditableRegistryService)
class AuditableRegistryServiceTests extends AbstractHibernateTests {
    AuditableRegistryService auditableRegistryService

    void loadClasses() {
        this.gcl.parseClass('''
import grails.persistence.*

''')
    }

    void testInit() {
        assert auditableRegistryService != null
        assert auditableRegistryService.registered() != [].toSet()
    }

    void testIsAuditable() {

    }

    void testRegistered() {

    }

    void testIsRegistered() {

    }

    void testGetAuditableConfig() {

    }

    void testGetConfiguration() {

    }

    void testConfig() {

    }

    void testRegister() {

    }

    void testGetRegistry() {

    }

    void testSetRegistry() {

    }

    void testGetDomainClasses() {

    }

    void testSetDomainClasses() {

    }

}
