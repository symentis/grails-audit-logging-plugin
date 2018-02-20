package grails.plugins.orm.auditable

import grails.gorm.annotation.Entity
import grails.plugins.orm.auditable.resolvers.DefaultAuditRequestResolver
import grails.testing.gorm.DataTest
import spock.lang.Shared
import spock.lang.Specification

class AuditLogListenerSpec extends Specification implements DataTest {

    Class<?>[] getDomainClassesToMock() {
        [AuditLogEvent, Person, Train].toArray(Class)
    }

    @Shared
    AuditLogListener auditLogListener

    @Override
    Closure doWithConfig() {
        return { c ->
            c.grails.plugin.auditLog.auditDomainClassName = 'grails.plugins.orm.auditable.AuditLogEvent'
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
            AuditLogEvent.count() == 2
        when: 'Retrieve the audit event'
            AuditLogEvent propertyOne = AuditLogEvent.findByPropertyName('propertyOne')
            AuditLogEvent propertyTwo = AuditLogEvent.findByPropertyName('propertyTwo')
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
    void 'should store a single event when verbose=false'() {
        given:
            AuditLogContext.withoutVerboseAuditLog {
                Person person = new Person(
                    propertyOne:'foo',
                    propertyTwo:'bar'
                )
                person.save(flush:true)
            }
        expect: 'An auditevent is created'
            AuditLogEvent.count() == 1
        when: 'Retrieve the audit event'
            AuditLogEvent insertEvent = AuditLogEvent.get(1)
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

    void 'should store an event for each property update'() {
        given:
            Person person = new Person(
                propertyOne:'foo',
                propertyTwo:'bar'
            )
            person.save(flush:true)
        expect: 'An auditevent is created'
            AuditLogEvent.count() == 2
        when: 'Retrieve the audit event'
            person.propertyOne='bar'
            person.propertyTwo='foo'
            person.save(flush:true)
            AuditLogEvent propertyOne = AuditLogEvent.findByPropertyNameAndIdGreaterThan('propertyOne',2)
            AuditLogEvent propertyTwo = AuditLogEvent.findByPropertyNameAndIdGreaterThan('propertyTwo',2)
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
                it.oldValue == null
            }
            propertyOne.propertyName == 'propertyOne'
            propertyOne.newValue == 'bar'
            propertyOne.oldValue == 'foo'
            propertyTwo.propertyName == 'propertyTwo'
            propertyTwo.newValue == 'foo'
            propertyTwo.oldValue == 'bar'
    }

    void 'should only audit configured events'(){
        given:
            Train train = new Train(number:'1')
            train.save(flush:true)
        expect:
            AuditLogEvent.count()==0
        when:
            train.number = "2"
            train.save()
        then:
            AuditLogEvent.count()==0
        when:
            train.delete()
        then:
            AuditLogEvent.count()==1
    }
}

@Entity
class Person implements Auditable{
    String propertyOne
    String propertyTwo
}

@Entity
class Train implements Auditable{
    String number

    @Override
    Collection<AuditEventType> getLogIgnoreEvents() {
        return [AuditEventType.INSERT,AuditEventType.UPDATE]
    }
}
