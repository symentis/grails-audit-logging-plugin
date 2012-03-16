package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 2/2/12
 * Time: 8:38 PM
 * To change this template use File | Settings | File Templates.
 */
class AuditableRegistryTests extends GroovyTestCase {

   AuditableRegistry auditableRegistry = new AuditableRegistry();

    void testAuditableRegistryDetection() {
        def example = ga.getDomainClass("ExampleClassicAuditableEntity")
        assert example != null
        assert auditableRegistry.isAuditable(example) == true
    }
}
