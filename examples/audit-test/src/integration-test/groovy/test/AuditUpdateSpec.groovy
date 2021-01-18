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

import audit.test.HeliportService
import grails.plugins.orm.auditable.AuditLogContext
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.springframework.transaction.TransactionStatus
import spock.lang.Specification

@Slf4j
@Integration
class AuditUpdateSpec extends Specification {

    HeliportService heliportService

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                Book.where {}.deleteAll()
                Author.where {}.deleteAll()
                Publisher.where {}.deleteAll()
                Heliport.where {}.deleteAll()

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
        EntityInSecondDatastore.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                EntityInSecondDatastore.where {}.deleteAll()
                new EntityInSecondDatastore(name:"name", someIntegerProperty:1).save(flush: true, failOnError: true)
            }
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

    void "Test rollback behaviour"() {
        when:
        Author.withNewTransaction { TransactionStatus transactionStatus ->
            def author = Author.findByName("Aaron")
            author.age = 1
            Author.withSession { Session session ->
                session.flush()
            }
            transactionStatus.setRollbackOnly()
        }

        then:
        Author.withNewTransaction { TransactionStatus transactionStatus ->
            Author.findByName("Aaron").age == 37
        }
        AuditTrail.withNewTransaction {
            AuditTrail.list() == []
        }

        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.age = 3
        }

        then:
        Author.currentGormStaticApi().datastore
        Author.withNewTransaction {
            Author.findByName("Aaron").age == 3
        }
        AuditTrail.withNewTransaction {
            AuditTrail.count
        } == 1
    }

    void "Test nested transactions"() {
        when:
        Author.withNewTransaction { TransactionStatus transactionStatus ->
            Author.findByName("Aaron").age = 1
            Author.withSession { Session session ->
                session.flush()
            }
            Book.withNewTransaction { TransactionStatus transactionStatus2 ->
                Book.findByTitle("Hunger Games").pages = 401
            }
            transactionStatus.setRollbackOnly()
        }

        then:
        Author.withNewTransaction {
            Author.findByName("Aaron")
        }.age == 37
        Book.withNewTransaction {
            Book.findByTitle("Hunger Games")
        }.pages == 401
        AuditTrail.withNewTransaction {
            AuditTrail.count
        } == 1
        AuditTrail.withNewTransaction {
            AuditTrail.list()[0]
        }.newValue == "401"
    }

    void "test nested transactions different datastores" () {
        when:
        // Domain Foo has two datastores DEFAULT, second
        Author.withNewTransaction { TransactionStatus transactionStatus ->
            EntityInSecondDatastore.withNewTransaction {
                 new Author(name: "name2", age: 12, famous: true).save(flush: true, failOnError: true)
                 new EntityInSecondDatastore(name:"name2", someIntegerProperty:1).save(flush: true, failOnError: true)
                // Commit new EntityInSecondDatastore

                // Problem, because we have flush: true the AuditTrails are both immediately queued to the active synchronization
                // When the EntityInSecondDatastore commits *all* queued AuditTrails are saved
                // But we would only want to save those from EntityInSecondDatastore
                // => hibernate-envers solves this by having a queue for each transaction
                //    so even if we have multiple transactions we could queue e.g. the AuditTrails for Author independent from the AuditTrails for  EntityInSecondDatastore
                //    and commit them independently
                // Problem: We couldn't find a GORM api that would allow us to do that
            }
            // Rollback new Author
            transactionStatus.setRollbackOnly()
        }

        then:
        Author.withNewTransaction {
            Author.findByName("name2")
        } == null
        EntityInSecondDatastore.withNewTransaction {
            EntityInSecondDatastore.findByName("name2")
        } != null
        AuditTrail.withNewTransaction {
            AuditTrail.list().collect { it.className }.unique()
        }.size() == 1
    }
}
