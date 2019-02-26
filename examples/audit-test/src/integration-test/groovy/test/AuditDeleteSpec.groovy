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

import grails.gorm.transactions.Rollback
import grails.plugins.orm.auditable.AuditLogContext
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.testing.mixin.integration.Integration
import spock.lang.Shared
import spock.lang.Specification

@Integration
@Rollback
class AuditDeleteSpec extends Specification {
    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.excluded?.asImmutable() ?: []
    }

    void setupData() {
        AuditLogContext.withoutAuditLog {
            def author = new Author(name: "Aaron", age: 37, famous: true)
            author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
            author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
            author.save(flush: true, failOnError: true)

            def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
            publisher.save(flush: true, failOnError: true)
        }

        // Remove all logging of the inserts, we are focused on deletes here
        AuditTrail.withNewTransaction {
            AuditTrail.where { id != null }.deleteAll()
            assert AuditTrail.count() == 0
        }
    }

    void "Test default delete logging"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")
        assert author

        when:
        author.delete(flush: true, failOnError: true)

        then: "audit logging is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == author.getAuditablePropertyNames().size()

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == null
        first.eventName == 'DELETE'

        and: 'all books are deleted'
        def b1Events = AuditTrail.withCriteria { eq('className', 'test.Book'); eq('persistedObjectId', 'Hunger Games') }
        b1Events.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size()

        def b2Events = AuditTrail.withCriteria {
            eq('className', 'test.Book'); eq('persistedObjectId', 'Catching Fire')
        }
        b2Events.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size()
    }

    void "Test conditional delete logging"() {
        given:
        setupData()
        def publisher = Publisher.findByName("Random House")

        when:
        publisher.active = activeFlag
        publisher.delete(flush: true, failOnError: true)

        then:
        !Publisher.get(publisher.id)

        and:
        def events = AuditTrail.withCriteria { eq('className', 'test.Publisher') }
        events.size() == resultCount

        where: "publisher active flag determines logging"
        activeFlag << [false, true]
        resultCount << [0, 3]
    }

    void "Test only delete events are logged"() {
        def resolution = new Resolution()
        resolution.name = "One for all"
        resolution.save(flush: true, failOnError: true)

        when: "updating resolution"
        resolution.name = "One for all and all for one"
        resolution.save(flush: true, failOnError: true)

        then: "delete resolution"
        resolution.delete(flush: true, failOnError: true)

        def events = AuditTrail.withCriteria { eq('className', 'test.Resolution') }
        events.size() == 1
        and:
        events.get(0).eventName == "DELETE"
    }

    void "Test defaultExcluded properties"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")

        when:
        author.delete(flush: true, failOnError: true)

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

    void "Test context blacklist"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")

        when:
        AuditLogContext.withConfig(excluded: ['famous', 'age', 'dateCreated']) {
            author.delete(flush: true, failOnError: true)
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

    void "Test context whitelist"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")

        when:
        AuditLogContext.withConfig(included: ['famous', 'age', 'dateCreated']) {
            author.delete(flush: true, failOnError: true)
        }

        then: "only properties in given are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 3

        ['famous', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }

    void "Test whitelist overrides blacklist"() {
        given:
        setupData()

        def author = Author.findByName("Aaron")

        when:
        AuditLogContext.withConfig(included: ['famous', 'age', 'dateCreated'], excluded: ['famous', 'age']) {
            author.delete(flush: true, failOnError: true)
        }

        then: "only properties in whitelist are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 3
        ['famous', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }
}

