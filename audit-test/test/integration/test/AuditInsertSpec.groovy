package test

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import spock.lang.Unroll

class AuditInsertSpec extends IntegrationSpec {
		def grailsApplication

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
        events.size() == Author.gormPersistentEntity.persistentPropertyNames.size()

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == null
        first.newValue == "37"
        first.eventName == 'INSERT'
        first.actor == 'SYS'

        and: "verify that ssn is masked"
        def ssn = events.find { it.propertyName == 'ssn' }
        ssn.oldValue == null
        ssn.newValue == "**********"
        ssn.eventName == 'INSERT'
        ssn.actor == 'SYS'
    }

    void "Test override entity id with recursive domain class property"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)
        def book = new Book(title: 'Hunger Games', description: 'Blah', pages: 400)
        book.addToReviews(new Review(name: 'The Post'))
        author.addToBooks(book)

        when:
        author.save(flush: true, failOnError: true)

        then: "review log is created"
        def events = AuditLogEvent.findAllByClassName('Review')
        events.size() == Review.gormPersistentEntity.persistentPropertyNames.size()

        and: "the object id uses the naem from Review and the title from Book"
        def first = events.first()
        first.oldValue == null
        first.eventName == 'INSERT'
        first.persistedObjectId == 'The Post|Hunger Games'
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
        events.size() == Author.gormPersistentEntity.persistentPropertyNames.size()

        def bookEvents = AuditLogEvent.findAllByClassName('Book')
        bookEvents.size() == Book.gormPersistentEntity.persistentPropertyNames.size()
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
        first.persistedObjectId == 'ABC123|Random House'
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
        events.size() == Author.gormPersistentEntity.persistentPropertyNames.size()

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

    @Unroll
		void "Test auditing disabled in closure"() {

			when:
      println AuditLogEvent.findAllByClassName('Author')
      def author = new Author(name: name, age: 100, famous: true)
      if (enabled){
        author.save(flush: true, failOnError: true)
      } else {
        AuditLogListener.withoutAuditLog {
          author.save(flush: true, failOnError: true)
        }
      }

			then: "author is saved"
			author.id

			and: "check logged"
			def events = AuditLogEvent.findAllByClassName('Author')
			enabled ? events.size() == Author.gormPersistentEntity.persistentPropertyNames.size() : events.size() == 0

			and:
			author.handlerCalled == "onSave"

      where:
      name              | enabled
      'enabledLogging'  | true
      'disabledLogging' | false
      'againEnabled'    | true
      'againDisabled'   | false
		}

  @Unroll
  void "Test verbose auditing disabled in closure"() {

    when:
    println AuditLogEvent.findAllByClassName('Author')
    def author = new Author(name: name, age: 100, famous: true)
    if (enabled){
      author.save(flush: true, failOnError: true)
    } else {
      AuditLogListener.withoutVerboseAuditLog {
        author.save(flush: true, failOnError: true)
      }
    }

    then: "author is saved"
    author.id

    and: "check logged"
    def events = AuditLogEvent.findAllByClassName('Author')
    enabled ? events.size() == Author.gormPersistentEntity.persistentPropertyNames.size() : events.size() == 1

    and:
    author.handlerCalled == "onSave"

    where:
    name              | enabled
    'enabledVerbose'  | true
    'disabledVerbose' | false
    'againV'          | true
    'againDisabledV'  | false
  }
}
