package grails.plugins.orm.auditable

import grails.gorm.annotation.Entity
import grails.plugins.orm.auditable.resolvers.DefaultAuditRequestResolver
import grails.testing.gorm.DataTest
import spock.lang.Shared
import spock.lang.Specification

class AuditLogListenerSpec extends Specification implements DataTest {

    Class<?>[] getDomainClassesToMock() {
        [AuditEvent,Person].toArray(Class)
    }

    @Shared
    AuditLogListener auditLogListener

    @Override
    Closure doWithConfig() {
        return { c ->
            c.grails.plugin.auditLog.auditDomainClassName = 'grails.plugins.orm.auditable.AuditEvent'
            c.grails.plugin.auditLog.defaultActor = 'SYS'
            c.grails.plugin.auditLog.verbose = true
            c.grails.plugin.auditLog.excluded = ['version', 'lastUpdated', 'lastUpdatedBy']
        }
    }

    @Override
    Closure doWithSpring() {
        return {
            auditRequestResolver(DefaultAuditRequestResolver)
        }
    }

    void setupSpec() {
        auditLogListener = new AuditLogListener(dataStore, grailsApplication)
        applicationContext.addApplicationListener(auditLogListener)
    }

    void 'should store audit events for inserts'() {
        given:
            Person person = new Person(
                propertyOne:'foo',
                propertyTwo:'bar'
            )
            person.save(flush:true)
        expect: 'An auditevent for each property is created'
            AuditEvent.count() == 2
        when: 'Retrieve the audit event'
            AuditEvent propertyOne = AuditEvent.findByPropertyName('propertyOne')
            AuditEvent propertyTwo = AuditEvent.findByPropertyName('propertyTwo')
        then: 'Check the values set'
            propertyOne
            propertyTwo
            [propertyOne,propertyTwo].each{
                it.actor == 'SYS'
                it.uri == null
                it.className == 'Person'
                it.eventName == AuditEventType.INSERT.name()
                it.persistedObjectId == '1'
                it.persistedObjectVersion == 0
                it.dateCreated == it.lastUpdated
                it.oldValue == null
            }
            propertyOne.propertyName == 'propertyOne'
            propertyOne.newValue == 'foo'
            propertyTwo.propertyName == 'propertyTwo'
            propertyTwo.newValue == 'bar'
    }
    void 'when insert verbose store events for each property'() {
        given:
            AuditLogContext.withoutVerboseAuditLog {
                Person person = new Person(
                    propertyOne:'foo',
                    propertyTwo:'bar'
                )
                person.save(flush:true)
            }
        expect: 'An auditevent is created'
            AuditEvent.count() == 3
        when: 'Retrieve the audit event'
            AuditEvent insertEvent = AuditEvent.get(1)
        then: 'Check the values set'
            insertEvent.actor == 'SYS'
            insertEvent.uri == null
            insertEvent.className == 'Person'
            insertEvent.eventName == AuditEventType.INSERT.name()
            insertEvent.persistedObjectId == '1'
            insertEvent.persistedObjectVersion == 0
            insertEvent.dateCreated == insertEvent.lastUpdated
            insertEvent.propertyName == null
            insertEvent.oldValue == null
            insertEvent.newValue == null
    }
}

@Entity
class Person implements Auditable{
    String propertyOne
    String propertyTwo
}
