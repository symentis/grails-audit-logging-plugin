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


import grails.plugins.orm.auditable.AuditLogContext
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.testing.mixin.integration.Integration
import org.springframework.util.StringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Integration
class AuditInsertSpec extends Specification {
    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.excluded?.asImmutable() ?: []
        AuditTrail.withNewTransaction { AuditTrail.executeUpdate('delete from AuditTrail') }
    }

    void cleanup() {
        Author.withNewTransaction {
            Review.where {}.deleteAll()
            Book.where {}.deleteAll()
            Author.where {}.deleteAll()
            Publisher.where {}.deleteAll()
        }
    }

    void "Test basic insert logging"() {
        given:
        def author = new Author(name: "Aaron", age: 37, famous: true)

        when:
        Author.withNewTransaction {
            author.save(flush: true, failOnError: true)
        }

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == null
        first.newValue == "37"
        first.eventName == 'INSERT'
        first.actor == 'SYS'
        first.id instanceof String // must be a String - see Config.groovy gorm mapping
        StringUtils.countOccurrencesOf((String) first.id, "-") == 4 // must be UUID format - see Config.groovy

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
        Author.withNewTransaction {
            author.save(flush: true, failOnError: true)
        }

        then: "review log is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Review') }
        events.size() == (Review.gormPersistentEntity.persistentPropertyNames - defaultIgnoreList).size()

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
        Author.withNewTransaction {
            author.save(flush: true, failOnError: true)
        }

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()

        def bookEvents = AuditTrail.withCriteria { eq('className', 'test.Book') }
        bookEvents.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size()
    }

    void "Test logging with a different id"() {
        given:
        def publisher = new Publisher(code: 'ABC123', name: "Random House", active: true)

        when:
        Author.withNewTransaction {
            publisher.save(flush: true, failOnError: true)
        }

        then:
        publisher.id

        and: "audit logging is created for code"
        AuditTrail.withNewSession {
            def events = AuditTrail.findAllByClassName('test.Publisher')
            events.size() == 3

            def first = events.find { it.propertyName == 'name' }
            first.persistedObjectId == 'ABC123|Random House'
            first.newValue == 'Random House'
            first.eventName == 'INSERT'
        }
    }

    void "Test conditional logging"() {
        given:
        def publisher = new Publisher(code: 'ABC123', name: "Random House", active: false)

        when:
        Author.withNewTransaction {
            publisher.save(flush: true, failOnError: true)
        }

        then:
        publisher.id

        and: "no auditting"
        AuditTrail.withNewSession {
            def events = AuditTrail.findAllByClassName('test.Publisher')
            events.size() == 0
        }
    }

    void "Test failed insert logging"() {
        given: "an invalid author"
        def author = new Author()

        when:
        Author.withNewTransaction {
            author.save(flush: true, failOnError: true)
        }

        then: "author is not saved"
        thrown(Exception)

        and: "no audit is created"
        AuditTrail.withNewSession {
            def events = AuditTrail.findAllByClassName('test.Author')
            events.size() == 0
        }
    }

    @Unroll
    void "Test auditing disabled in closure #name"() {
        when:
        println AuditTrail.withCriteria { eq('className', 'test.Author') }
        def author = new Author(name: name, age: 100, famous: true)

        Author.withNewTransaction {
            if (enabled) {
                author.save(flush: true, failOnError: true)
            }
            else {
                AuditLogContext.withoutAuditLog {
                    author.save(flush: true, failOnError: true)
                }
            }
        }

        then: "author is saved"
        author.id

        and: "check logged"
        AuditTrail.withNewSession {
            def events = AuditTrail.findAllByClassName('test.Author')
            enabled ? events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size() : events.size() == 0
        }

        where:
        name              | enabled
        'enabledLogging'  | true
        'disabledLogging' | false
        'againEnabled'    | true
        'againDisabled'   | false
    }

    @Unroll
    void "Test verbose auditing disabled in closure #name"() {
        when:
        println AuditTrail.withCriteria { eq('className', 'test.Author') }
        def author = new Author(name: name, age: 100, famous: true)

        Author.withNewTransaction {
            if (enabled) {
                author.save(flush: true, failOnError: true)
            }
            else {
                AuditLogContext.withoutVerboseAuditLog {
                    author.save(flush: true, failOnError: true)
                }
            }
        }

        then: "author is saved"
        author.id

        and: "check logged"
        AuditTrail.withNewSession {
            def events = AuditTrail.findAllByClassName('test.Author')
            enabled ? events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size() : events.size() == 1
        }

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
        Author.withNewTransaction {
            author.save(flush: true, failOnError: true)
        }

        then: "ignored properties not logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 7
        ['name', 'publisher', 'books', 'ssn', 'age', 'famous', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
        ['version', 'lastUpdated', 'lastUpdatedBy'].each { name ->
            assert !events.find { it.propertyName == name }, "${name} was logged"
        }
    }

    void "Test context excluded properties"() {
        when:
        Author.withNewTransaction {
            AuditLogContext.withConfig(excluded: ['famous', 'age', 'dateCreated']) {
                def author = new Author(name: "Aaron", age: 50)
                author.save()
            }
        }

        then: "ignored properties not logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 7
        ['name', 'publisher', 'books', 'ssn', 'lastUpdated', 'lastUpdatedBy', 'version'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
        ['famous', 'age', 'dateCreated'].each { name ->
            assert !events.find { it.propertyName == name }, "${name} was logged"
        }
    }

    void "Test context included properties"() {
        given:
        def author = new Author(name: "Aaron", age: 50, famous: true, ssn: '123-981-0001')

        when:
        Author.withNewTransaction {
            AuditLogContext.withConfig(included: ['name', 'age', 'dateCreated']) {
                author.save(flush: true, failOnError: true)
            }
        }

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 3
        ['name', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }

    void "Test included overrides excluded list"() {
        given:
        def author = new Author(name: "Aaron", age: 50, famous: true, ssn: '123-981-0001')

        when:
        Author.withNewTransaction {
            AuditLogContext.withConfig(included: ['name', 'age', 'dateCreated'], excluded: ['name', 'age']) {
                author.save(flush: true, failOnError: true)
            }
        }

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 3
        ['name', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }
}
