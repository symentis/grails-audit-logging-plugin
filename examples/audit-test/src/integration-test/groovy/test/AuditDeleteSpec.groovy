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
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Integration
class AuditDeleteSpec extends Specification {
    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.excluded?.asImmutable() ?: []
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                def author = new Author(name: "Aaron", age: 37, famous: true)
                author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
                author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
                author.save(flush: true, failOnError: true)

                def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
                publisher.save(flush: true, failOnError: true)
            }
        }
    }

    void cleanup() {
        Author.withNewTransaction {
            Publisher.where {}.deleteAll()
            Book.where {}.deleteAll()
            Author.where {}.deleteAll()
        }
        AuditTrail.withNewTransaction {
            AuditTrail.where {}.deleteAll()
        }
    }

    void "Test default delete logging"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            assert author
            author.delete(flush: true, failOnError: true)
        }

        then: "audit logging is created"
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }
        events.size() == Author.NUMBER_OF_AUDITABLE_PROPERTIES

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == null
        first.eventName == 'DELETE'

        and: 'all books are deleted'
        AuditTrail.withNewTransaction {
            def b1Events = AuditTrail.withCriteria {
                eq('className', 'test.Book')
                eq('persistedObjectId', 'Hunger Games')
            }
            b1Events.size()
        } == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size() - 1
        AuditTrail.withNewTransaction {
            def b2Events = AuditTrail.withCriteria {
                eq('className', 'test.Book'); eq('persistedObjectId', 'Catching Fire')
            }
            b2Events.size()
        } == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size() - 1
    }

    @Unroll
    void "Test conditional delete logging active: #activeFlag"() {
        when:
        Publisher.withNewTransaction {
            def publisher = Publisher.findByName("Random House")
            publisher.active = activeFlag
            publisher.delete(flush: true, failOnError: true)
        }

        then:
        Publisher.withNewTransaction {
            Publisher.count == 0
        }

        and:
        AuditTrail.withNewTransaction {
            def events = AuditTrail.withCriteria { eq('className', 'test.Publisher') }
            events.size()
        } == resultCount

        where: "publisher active flag determines logging"
        activeFlag | resultCount
        false      | 0
        true       | 3
    }

    void "Test only delete events are logged"() {
        Resolution.withNewTransaction {
            def resolution = new Resolution()
            resolution.name = "One for all"
            resolution.save(flush: true, failOnError: true)
        }

        when: "updating resolution"
        Resolution.withNewTransaction {
            def resolution = Resolution.find {}
            resolution.name = "One for all and all for one"
            resolution.save(flush: true, failOnError: true)
        }

        and: "deleting resolution"
        Resolution.withNewTransaction {
            def resolution = Resolution.find {}
            resolution.delete(flush: true, failOnError: true)
        }
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Resolution') }
        }

        then:
        events.size() == 1
        events.get(0).eventName == "DELETE"
    }

    void "Test defaultExcluded properties"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.delete(flush: true, failOnError: true)
        }
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }

        then: "ignored properties not logged"
        events.size() == 6
        ['name', 'publisher', 'ssn', 'age', 'famous', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was logged"
        }
        ['version', 'lastUpdated', 'lastUpdatedBy', 'books'].each { name ->
            assert !events.find { it.propertyName == name }, "${name} was not logged"
        }

    }

    void "Test context blacklist"() {
        when:
        AuditLogContext.withConfig(excluded: ['famous', 'age', 'dateCreated']) {
            Author.withNewTransaction {
                def author = Author.findByName("Aaron")
                author.delete(flush: true, failOnError: true)
            }
        }
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }

        then: "ignored properties not logged"
        events.size() == 6
        ['name', 'publisher', 'ssn', 'lastUpdated', 'lastUpdatedBy', 'version'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was logged"
        }
        ['famous', 'age', 'dateCreated', 'books'].each { name ->
            assert !events.find { it.propertyName == name }, "${name} was not logged"
        }
    }

    void "Test context whitelist"() {
        when:
        AuditLogContext.withConfig(included: ['famous', 'age', 'dateCreated']) {
            Author.withNewTransaction {
                def author = Author.findByName("Aaron")
                author.delete(flush: true, failOnError: true)
            }
        }
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }

        then: "ignored properties not logged"
        events.size() == 3
        ['famous', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was logged"
        }
    }

    void "Test whitelist overrides blacklist"() {
        when:
        AuditLogContext.withConfig(included: ['famous', 'age', 'dateCreated'], excluded: ['famous', 'age']) {
            Author.withNewTransaction {
                def author = Author.findByName("Aaron")
                author.delete(flush: true, failOnError: true)
            }
        }
        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }

        then: "only properties in whitelist are logged"
        events.size() == 3
        ['famous', 'age', 'dateCreated'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was logged"
        }
    }
}

