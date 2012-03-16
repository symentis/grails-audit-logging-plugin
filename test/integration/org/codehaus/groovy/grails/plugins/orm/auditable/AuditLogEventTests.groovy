package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.plugins.orm.auditable.testing.AbstractHibernateTests

class AuditLogEventTests extends AbstractHibernateTests {

    void loadClasses() {
        this.gcl.parseClass('''
import grails.persistence.*

''')
    }

    void testSomething() {

    }
}
