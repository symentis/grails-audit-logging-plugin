package test

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

class AuditUpdateSpec extends IntegrationSpec {
    void setup() {
        Author.auditable = true

        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
        author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
        author.save(flush: true, failOnError: true)

        def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
        publisher.save(flush: true, failOnError: true)

        // Remove all logging of the inserts, we are focused on updates here
        AuditLogEvent.where { id != null }.deleteAll()
        assert AuditLogEvent.count() == 0

        author.handlerCalled = ""
    }

    void "Test basic update logging"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.age = 40
        author.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 1

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == "40"
        first.eventName == "UPDATE"
    }

    void "Test update to-one association"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.publisher = Publisher.findByName("Random House")
        author.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 1

        def first = events.find { it.propertyName == 'publisher' }
        first.oldValue == null
        first.newValue ==~ /\[id:ABC123]test\.Publisher : \d+/
        first.eventName == "UPDATE"
    }

    void "Test two saves, one flush"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.age = 40
        author.save(failOnError: true)
        author.age = 50
        author.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 1

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == "50"
        first.eventName == "UPDATE"
    }

    void "Test conditional logging disabled"() {
        given:
        def publisher = Publisher.findByName("Random House")

        when: "is not active"
        publisher.name = "Spring"
        publisher.active = false
        publisher.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('Publisher')
        events.size() == 0
    }

    void "Test conditional logging enabled"() {
        given:
        def publisher = Publisher.findByName("Random House")

        when: "is not active"
        publisher.name = "Spring"
        publisher.active = true
        publisher.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('Publisher')
        events.size() == 1

        def first = events.first()
        first.persistedObjectId == 'ABC123'
        first.oldValue == 'Random House'
        first.newValue == 'Spring'
        first.propertyName == 'name'
        first.eventName == 'UPDATE'
    }

    void "Test globally ignored properties"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.lastUpdatedBy = 'Aaron'
        author.save(flush: true, failOnError: true)

        then: "nothing logged"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 0
    }

    void "Test handler is called"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.famous = false
        author.save(flush: true, failOnError: true)

        then: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 1

        and:
        author.handlerCalled == "onChange"
    }

    void "Test only handler is called"() {
        given:
        def author = Author.findByName("Aaron")
        Author.auditable = [handlersOnly: true]

        when:
        author.famous = false
        author.save(flush: true, failOnError: true)

        then: "nothing logged"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 0

        and:
        author.handlerCalled == "onChange"
    }
}
