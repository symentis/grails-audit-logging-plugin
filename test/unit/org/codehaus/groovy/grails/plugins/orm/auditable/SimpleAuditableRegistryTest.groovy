package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.plugins.orm.auditable.testing.AbstractHibernateTests

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 2/2/12
 * Time: 8:38 PM
 * To change this template use File | Settings | File Templates.
 */
class SimpleAuditableRegistryTest extends AbstractHibernateTests {
   AuditableRegistry auditableRegistry = new AuditableRegistry();

    void loadClasses() {
        this.gcl.parseClass('''
import grails.persistence.*

@Entity
class ExampleClassicAuditableEntity {
    static auditable = true
    String value
}

@Entity
class ExampleMapConfiguredEntity {
    static auditable = [handlersOnly:true]
    String value
}

@Entity
class ExampleMapConfiguredEntityToo {
    static auditable = [ignoreList:['value']]
    String value
}

@Entity
class ExampleMapConfiguredEntityTransactionalTrue {
    static auditable = [transactional:true]
    String value
}
''')
    }

    void testIsAuditableRegistryDetection() {
        println ga.getDomainClasses()
        def dClass = ga.getDomainClass("ExampleClassicAuditableEntity")
        println dClass
        def exampleClass = dClass.clazz
        println exampleClass
        assert exampleClass != null
        assert auditableRegistry.isAuditable(dClass) == true
    }

    void testRegistrationOfDomainClass() {
        def dClass = ga.getDomainClass("ExampleClassicAuditableEntity")
        assert auditableRegistry.register(dClass) == true
        def config = auditableRegistry.getAuditableConfig(dClass)
        assert config instanceof AuditableConfig
        assert config.handlersOnly == false
        assert config.ignoreList == ['version','lastUpdated']
    }

    void testGetAuditableConfigHandlersOnly() {
        def dClass = ga.getDomainClass("ExampleMapConfiguredEntity")
        assert auditableRegistry.register(dClass) == true
        def config = auditableRegistry.getAuditableConfig(dClass)
        assert config instanceof AuditableConfig
        assert config.handlersOnly == true
        assert config.ignoreList == ['version','lastUpdated']
    }

    void testGetAuditableConfigHandlersOnlyTooTransactionalTrue() {
        def dClass = ga.getDomainClass("ExampleMapConfiguredEntityTransactionalTrue")
        assert auditableRegistry.register(dClass) == true
        def config = auditableRegistry.getAuditableConfig(dClass)
        assert config instanceof AuditableConfig
        assert config.handlersOnly == false
        assert config.ignoreList == ['version','lastUpdated']
        assert config.transactional == true
    }
}

