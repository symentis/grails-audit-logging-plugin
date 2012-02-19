package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.commons.GrailsApplication
import grails.test.GrailsUnitTestCase

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 2/2/12
 * Time: 8:38 PM
 * To change this template use File | Settings | File Templates.
 */
class AuditableRegistryTests extends AbstractHibernateTests {
   AuditableRegistry auditableRegistry = new AuditableRegistry();

    void loadClasses() {
        this.gcl.parseClass('''
import grails.persistence.*

@Entity
class ExampleClassicAuditableEntity {
    static auditable = true
    String value
}
''')
    }

    void testAuditableRegistryDetection() {
        println ga.getDomainClasses()
        def dClass = ga.getDomainClass("ExampleClassicAuditableEntity")
        println dClass
        def exampleClass = dClass.clazz
        println exampleClass
        assert exampleClass != null
        assert auditableRegistry.isAuditable(dClass) == true
    }
}

