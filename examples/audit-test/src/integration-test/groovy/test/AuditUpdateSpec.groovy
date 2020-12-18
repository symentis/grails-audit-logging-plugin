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
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
@Rollback
class AuditUpdateSpec extends Specification {

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                def author = new Author(name: "Aaron", age: 37, famous: true)
                author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
                author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
                author.save(flush: true, failOnError: true)

                def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
                publisher.save(flush: true, failOnError: true)

                def heliport = new Heliport(code: 'EGLW', name: 'Battersea Heliport')
                heliport.save(flush: true, failOnError: true)
            }
        }
    }

    void cleanup() {
        Author.withNewTransaction {
            Book.where {}.deleteAll()
            Author.where {}.deleteAll()
            Publisher.where {}.deleteAll()
            Heliport.where {}.deleteAll()
        }
        AuditTrail.withNewTransaction {
            AuditTrail.where {}.deleteAll()
            assert AuditTrail.count() == 0
        }
    }

    void "Test persistedObjectVersion in update logging"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.age = 40
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def first = events.find { it.propertyName == 'age' }
        first.persistedObjectVersion == 0
        first.oldValue == "37"
        first.newValue == "40"
        first.eventName == "UPDATE"
    }

    void "Test false/null differentiation"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.famous = false
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def first = events.find { it.propertyName == 'famous' }
        first.oldValue == 'true'
        first.newValue == 'false'
        first.eventName == 'UPDATE'
    }

    void "Test persistedObjectVersion in update logging for domain class without version"() {
        when:
        Heliport.withNewTransaction {
            def heliport = Heliport.findByCode('EGLW')
            heliport.name = "London Heliport"
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Heliport') }
        events.size() == 1

        def first = events.find { it.propertyName == 'name' }
        first.persistedObjectVersion == null
    }

    void "Test update to-one association"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.publisher = Publisher.findByName("Random House")
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def first = events.find { it.propertyName == 'publisher' }
        first.oldValue == null
        first.newValue ==~ /\[id:ABC123\|Random House]test\.Publisher : \d+/
        first.eventName == "UPDATE"
    }

    void "Test two saves, one flush"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.age = 40
            author.save(failOnError: true)
            author.age = 50
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == "50"
        first.eventName == "UPDATE"
    }

    void "Test conditional logging disabled"() {
        when:
        Publisher.withNewTransaction {
            def publisher = Publisher.findByName("Random House")
            publisher.name = "Spring"
            publisher.active = false
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Publisher') }
        events.size() == 0
    }

    void "Test conditional logging enabled"() {
        when:
        Publisher.withNewTransaction {
            def publisher = Publisher.findByName("Random House")
            publisher.name = "Spring"
            publisher.active = true
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Publisher') }
        events.size() == 1

        def first = events.first()
        first.persistedObjectId == 'ABC123|Spring'
        first.oldValue == 'Random House'
        first.newValue == 'Spring'
        first.propertyName == 'name'
        first.eventName == 'UPDATE'
    }

    void "Test globally ignored properties"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.lastUpdatedBy = 'Aaron'
        }

        then: "nothing logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 0
    }

    void "Test excluded properties via context"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            AuditLogContext.withConfig(excluded: ['name', 'famous', 'lastUpdate']) {
                author.age = 50
                author.famous = false
                author.name = 'Bob'
                author.save(flush: true, failOnError: true)
            }
        }

        then: "ignored properties not logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 1

        def first = events.find { it.propertyName == 'age' }
        first.persistedObjectVersion == 0
    }

    void "Test included properties via context"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            AuditLogContext.withConfig(included: ['name', 'famous']) {
                author.age = 50
                author.famous = false
                author.name = 'Bob'
                author.save(flush: true, failOnError: true)
            }
        }

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 2

        ['name', 'famous'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }

    void "Test included properties overrides excluded"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            AuditLogContext.withConfig(included: ['name', 'famous', 'lastUpdated'], excluded: ['name', 'famous']) {
                author.age = 50
                author.famous = false
                author.name = 'Bob'
                author.save(flush: true, failOnError: true)
            }
        }

        then: "only properties in auditableProperties are logged"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }

        events.size() == 2

        ['name', 'famous'].each { name ->
            assert events.find { it.propertyName == name }, "${name} was not logged"
        }
    }
}
