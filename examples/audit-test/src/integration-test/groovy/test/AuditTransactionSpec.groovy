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
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.springframework.transaction.TransactionStatus
import spock.lang.Specification

@Slf4j
@Integration
class AuditTransactionSpec extends Specification {

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                Book.where {}.deleteAll()
                Author.where {}.deleteAll()

                def author = new Author(name: "Aaron", age: 37, famous: true)
                author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
                author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
                author.save(flush: true, failOnError: true)
            }
        }
        EntityInSecondDatastore.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                EntityInSecondDatastore.where {}.deleteAll()
                new EntityInSecondDatastore(name: "name", someIntegerProperty: 1).save(flush: true, failOnError: true)
            }
        }
        AuditTrail.withNewTransaction {
            AuditTrail.where {}.deleteAll()
            assert AuditTrail.count() == 0
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
        }.age == 37 // rolled back change -> original value
        Book.withNewTransaction {
            Book.findByTitle("Hunger Games")
        }.pages == 401 // committed change -> new value
        AuditTrail.withNewTransaction {
            AuditTrail.count
        } == 1 // AuditTrail only for committed change
        AuditTrail.withNewTransaction {
            AuditTrail.list()[0]
        }.newValue == "401"
    }

    void "test nested transactions different datastores: rollback outer"() {
        expect:
        AuditTrail.withNewTransaction {
            AuditTrail.list().collect { it.className }.unique()
        } == []

        when:
        Author.withNewTransaction { TransactionStatus transactionStatus ->
            EntityInSecondDatastore.withNewTransaction {
                new Author(name: "name2", age: 12, famous: true).save(flush: true, failOnError: true)
                new EntityInSecondDatastore(name: "name2", someIntegerProperty: 1).save(flush: true, failOnError: true)
                // Commit new EntityInSecondDatastore
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
        } == ["test.EntityInSecondDatastore"]
    }

    void "test nested transactions different datastores: rollback inner"() {
        expect:
        AuditTrail.withNewTransaction {
            AuditTrail.list().collect { it.className }.unique()
        } == []

        when:
        Author.withNewTransaction {
            EntityInSecondDatastore.withNewTransaction { TransactionStatus transactionStatus ->
                new Author(name: "name2", age: 12, famous: true).save(flush: true, failOnError: true)
                new EntityInSecondDatastore(name: "name2", someIntegerProperty: 1).save(flush: true, failOnError: true)
                // Rollback new EntityInSecondDatastore
                transactionStatus.setRollbackOnly()
            }
            // Commit new Author
        }

        then:
        Author.withNewTransaction {
            Author.findByName("name2")
        } != null
        EntityInSecondDatastore.withNewTransaction {
            EntityInSecondDatastore.findByName("name2")
        } == null
        AuditTrail.withNewTransaction {
            AuditTrail.list().collect { it.className }.unique()
        } == ["test.Author"]
    }
}
