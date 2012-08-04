package com.codehaus.groovy.grails.plugins.orm.auditable

import grails.persistence.Entity

import org.junit.Test
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

@TestFor(ExampleClassicAuditableEntity)
class AuditLogEventTests extends DomainEventListenerServiceTest  {
    @Test
    void testOnSaveEvent() {
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
