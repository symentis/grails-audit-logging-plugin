package com.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent
import org.codehaus.groovy.grails.plugins.orm.auditable.DomainEventListenerService
import grails.test.mixin.TestFor
import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext

@TestFor(AuditLogEvent)
class AuditLogEventTests  {
    DomainEventListenerService domainEventListenerService
    AuditLogListener auditLogListener

    void testAuditableDetection() {
        assert domainEventListenerService != null
        assert auditLogListener != null

        mockDomain(ExampleClassicAuditableEntity);

        // domainEventListenerService.register(simpleDatastore)
        applicationContext.applicationEventMulticaster.addApplicationListener(domainEventListenerService)

        def count = AuditLogEvent.count()
        assert ExampleClassicAuditableEntity.count() == 0
        new ExampleClassicAuditableEntity(value:'foo').save()
        assert ExampleClassicAuditableEntity.count() == 1
        assert count + 1 == AuditLogEvent.count()
    }
}

@Entity
class ExampleClassicAuditableEntity {
    static auditable = true
    String value
    void afterInsert() {
        println "called"
    }
}
