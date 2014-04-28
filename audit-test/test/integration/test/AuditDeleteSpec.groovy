package test

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

class AuditDeleteSpec extends IntegrationSpec {
    void setup() {
        Author.auditable = true

        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
        author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
        author.save(flush: true, failOnError: true)

        def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
        publisher.save(flush: true, failOnError: true)

        // Remove all logging of the inserts, we are focused on deletes here
        AuditLogEvent.where { id != null }.deleteAll()
        assert AuditLogEvent.count() == 0

        author.handlerCalled = ""
    }

    void "Test delete logging"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.delete(flush: true, failOnError: true)

        then: "audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        // events.size() == Author.gormPersistentEntity.persistentPropertyNames.size()
        // GPAUDITLOGGING-61: for now, all deletes are non-verbose
        events.size() == 1

        // GPAUDITLOGGING-61: for now, all deletes are non-verbose, therefore we cannot get properties
        // def first = events.find { it.propertyName == 'age' }
        // first.oldValue == "37"
        // first.newValue == null
        // first.eventName == 'DELETE'

        and: 'all books are deleted'
        def b1Events = AuditLogEvent.findAllByClassNameAndPersistedObjectId('Book', 'Hunger Games')
        // b1Events.size() == Book.gormPersistentEntity.persistentPropertyNames.size()
        // GPAUDITLOGGING-61: for now, all deletes are non-verbose
        b1Events.size() == 1

        def b2Events = AuditLogEvent.findAllByClassNameAndPersistedObjectId('Book', 'Catching Fire')
        // b2Events.size() == Book.gormPersistentEntity.persistentPropertyNames.size()
        // GPAUDITLOGGING-61: for now, all deletes are non-verbose
        b2Events.size() == 1
    }

    void "Test conditional delete logging"() {
        given:
        def publisher = Publisher.findByName("Random House")

        when:
        publisher.active = activeFlag
        publisher.delete(flush: true, failOnError: true)

        then:
        !Publisher.get(publisher.id)

        and:
        def events = AuditLogEvent.findAllByClassName('Publisher')
        events.size() == resultCount

        where: "publisher active flag determines logging"
        activeFlag << [false, true]
        // resultCount << [0, 3]
        // GPAUDITLOGGING-61: for now, all deletes are non-verbose
        resultCount << [0, 1]
    }

    void "Test handler is called"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.delete(flush: true, failOnError: true)

        then: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('Author')
        // events.size() == Author.gormPersistentEntity.persistentPropertyNames.size()
        // GPAUDITLOGGING-61: for now, all deletes are non-verbose
        events.size() == 1

      and:
        author.handlerCalled == "onDelete"
    }

    void "Test only handler is called"() {
        given:
        def author = Author.findByName("Aaron")
        Author.auditable = [handlersOnly: true]

        when:
        author.delete(flush: true, failOnError: true)

        then: "nothing logged"
        def events = AuditLogEvent.findAllByClassName('Author')
        events.size() == 0

        and:
        author.handlerCalled == "onDelete"
    }
}

