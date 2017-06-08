/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package test

import grails.plugins.orm.auditable.AuditLogListener
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.util.StringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Integration
@Rollback
class AuditInsertSpec extends Specification {

    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.defaultIgnore?.asImmutable() ?: []
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
        def events = AuditTrail.findAllByClassName('test.Author')
        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == null
        first.newValue == "37"
        first.eventName == 'INSERT'
        first.actor == 'SYS'
        first.id instanceof String // must be a String - see Config.groovy gorm mapping
        StringUtils.countOccurrencesOf((String)first.id, "-") == 4 // must be UUID format - see Config.groovy

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
        def events = AuditTrail.findAllByClassName('test.Review')
        events.size() == (Review.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()

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
        def events = AuditTrail.findAllByClassName('test.Author')
        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()

        def bookEvents = AuditTrail.findAllByClassName('test.Book')
        bookEvents.size() == (Book.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()
    }

    void "Test logging with a different id"() {
        given:
        def publisher = new Publisher(code: 'ABC123', name: "Random House", active: true)

        when:
        publisher.save(flush: true, failOnError: true)

        then:
        publisher.id

        and: "audit logging is created for code"
        def events = AuditTrail.findAllByClassName('test.Publisher')
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
        def events = AuditTrail.findAllByClassName('test.Publisher')
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
        def events = AuditTrail.findAllByClassName('test.Author')
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
        def events = AuditTrail.findAllByClassName('test.Author')
        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()

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
        def events = AuditTrail.findAllByClassName('test.Author')
        events.size() == 0

        and:
        author.handlerCalled == "onSave"
    }

    @Unroll
    void "Test auditing disabled in closure"() {

        when:
        println AuditTrail.findAllByClassName('test.Author')
        def author = new Author(name: name, age: 100, famous: true)
        if (enabled) {
            author.save(flush: true, failOnError: true)
        } else {
            AuditLogListener.withoutAuditLog {
                author.save(flush: true, failOnError: true)
            }
        }

        then: "author is saved"
        author.id

        and: "check logged"
        def events = AuditTrail.findAllByClassName('test.Author')
        enabled ? events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size() : events.size() == 0

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
        println AuditTrail.findAllByClassName('test.Author')
        def author = new Author(name: name, age: 100, famous: true)
        if (enabled) {
            author.save(flush: true, failOnError: true)
        } else {
            AuditLogListener.withoutVerboseAuditLog {
                author.save(flush: true, failOnError: true)
            }
        }

        then: "author is saved"
        author.id

        and: "check logged"
        def events = AuditTrail.findAllByClassName('test.Author')
        println events*.eventName
        enabled ? events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size() : events.size() == 1

        and:
        author.handlerCalled == "onSave"

        where:
        name              | enabled
        'enabledVerbose'  | true
        'disabledVerbose' | false
        'againV'          | true
        'againDisabledV'  | false
    }

    void "Test globally ignored properties"() {
        given:
        def author = new Author(name: "Aaron", age: 50)

        when:
        author.save(flush: true, failOnError: true)

        then: "ignored properties not logged"
        def events = AuditTrail.findAllByClassName('test.Author')
        println("Events: ${events}")
        events.size() == 8
        ['name', 'publisher', 'books', 'ssn', 'age', 'famous', 'dateCreated'].each { name ->
            assert events.find {it.propertyName == name}, "${name} was not logged"
        }
        ['version', 'lastUpdated', 'lastUpdatedBy'].each { name ->
            assert !events.find {it.propertyName == name}, "${name} was logged"
        }
    }

    void "Test locally ignored properties"() {
        given:
        Author.auditable = [ignore: ['famous', 'age', 'dateCreated']]
        def author = new Author(name: "Aaron", age: 50)

        when:
        author.save(flush: true, failOnError: true)

        then: "ignored properties not logged"
        def events = AuditTrail.findAllByClassName('test.Author')
        println("Events: ${events}")
        events.size() == 7
        ['name', 'publisher', 'books', 'ssn', 'lastUpdated', 'lastUpdatedBy'].each { name ->
            assert events.find {it.propertyName == name}, "${name} was not logged"
        }
        ['famous', 'age', 'dateCreated'].each { name ->
            assert !events.find {it.propertyName == name}, "${name} was logged"
        }
    }

    void "Test auditableProperties"() {
        given:
        Author.auditable = [auditableProperties: ['name', 'age', 'dateCreated']]
        def author = new Author(name: "Aaron", age: 50, famous: true, ssn: '123-981-0001')

        when:
        author.save(flush: true, failOnError: true)

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.findAllByClassName('test.Author')

        events.size() == 3
        ['name', 'age', 'dateCreated'].each { name ->
            assert events.find {it.propertyName == name}, "${name} was not logged"
        }
    }

    void "Test auditableProperties overrides ignore list"() {
        given:
        Author.auditable = [
          auditableProperties: ['name', 'age', 'dateCreated'],
          ignore: ['name', 'age']
        ]
        def author = new Author(name: "Aaron", age: 50, famous: true, ssn: '123-981-0001')

        when:
        author.save(flush: true, failOnError: true)

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.findAllByClassName('test.Author')

        events.size() == 3
        ['name', 'age', 'dateCreated'].each { name ->
            assert events.find {it.propertyName == name}, "${name} was not logged"
        }
    }

    void "Test insert logging with embedded object"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.address = new Address(city:'test', street:'teststr', zip:'testZip')
        author.addToBooks(new Book(title: 'Foo', description: 'Bar', pages: 200))

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id
        author.address.zip == 'testZip'

        and: "verbose audit logging is created"
        def events = AuditTrail.findAllByClassName('test.Author')
        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()

        def bookEvents = AuditTrail.findAllByClassName('test.Book')
        bookEvents.size() == (Book.gormPersistentEntity.persistentPropertyNames  - defaultIgnoreList).size()
    }

}
