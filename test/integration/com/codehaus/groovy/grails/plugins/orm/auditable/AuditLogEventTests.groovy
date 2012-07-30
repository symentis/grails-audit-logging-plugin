package com.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent
import org.codehaus.groovy.grails.plugins.orm.auditable.DomainEventListenerService
import grails.test.mixin.TestFor

@TestFor(ExampleClassicAuditableEntity)
class AuditLogEventTests  {
    DomainEventListenerService domainEventListenerService
    AuditLogListener auditLogListener

    void testAuditableDetection() {
        def logCount = 0
        assert domainEventListenerService != null
        assert auditLogListener != null
        auditLogListener.saveAuditLog = { AuditLogEvent audit ->
            logCount++
        }

        mockDomain(AuditLogEvent);

        applicationContext.applicationEventMulticaster.addApplicationListener(domainEventListenerService)

        assert 0 == logCount
        new ExampleClassicAuditableEntity(value:'foo').save()
        assert ExampleClassicAuditableEntity.count() == 1
        assert 1 == logCount
    }
}

@Entity
class ExampleClassicAuditableEntity {
    static auditable = true
    String value
}
