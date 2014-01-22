package test

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

class AuditInsertSpec extends IntegrationSpec {
    void setup() {
        Author.auditable = true
    }

    void "Test basic insert logging"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 7

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == null
        first.newValue == "37"
        first.eventName == 'INSERT'
    }

    void "Test insert logging with collection"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.addToBooks(new Book(title: 'Foo', description: 'Bar', pages: 200))

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 7

        def bookEvents = AuditLogEvent.findAllByClassName('Book')
        bookEvents.size() == 5
    }

    void "Test logging with a different id"() {
        given:
        def publisher = new Publisher(code: 'ABC123', name: "Random House", active: true)

        when:
        publisher.save(flush: true, failOnError: true)

        then:
        publisher.id

        and: "audit logging is created for code"
        def events = AuditLogEvent.findAllByClassName('Publisher')
        events.size() == 3

        def first = events.find { it.propertyName == 'name' }
        first.persistedObjectId == 'ABC123'
        first.newValue == 'Random House'
        first.eventName == 'INSERT'
    }

    void "Test conditional logging"() {
        given:
        def publisher = new Publisher(code: 'ABC123', name: "Random House", active: false)

        when:
        publisher.save(flush: true, failOnError: true)

        then:
        publisher.id

        and: "no auditting"
        def events = AuditLogEvent.findAllByClassName('Publisher')
        events.size() == 0
    }

    void "Test failed insert logging"() {
        given: "an invalid author"
        def author = new Author()

        when:
        author.save(flush: true, failOnError: true)

        then: "author is not saved"
        thrown(Exception)

        and: "no audit is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 0
    }

    void "Test handler is called"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 7

        and:
        author.handlerCalled == "onSave"
    }

    void "Test only handler is called"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)
        Author.auditable = [handlersOnly: true]

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "nothing logged"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 0

        and:
        author.handlerCalled == "onSave"
    }
}
