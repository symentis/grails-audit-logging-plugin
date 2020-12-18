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
import spock.lang.Specification

@Integration
class AuditUpdateCollectionSpec extends Specification {

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                def author = new Author(name: "Aaron", age: 37, famous: true)
                author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
                author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
                author.addToBooks(new Book(title: 'Mocking Jay', description: 'Blah', pages: 600))
                author.save(flush: true, failOnError: true)
            }
        }
    }

    void cleanup() {
        Author.withNewTransaction {
            Book.where {}.deleteAll()
            Author.where {}.deleteAll()
        }
        AuditTrail.withNewTransaction {
            AuditTrail.where {}.deleteAll()
        }
    }

    void "Test update property on an instance saved via cascade"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            def book = author.books.first()
            book.description = "Woo"
        }

        then: "the author didn't change"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 0

        and: "the book did"
        def bookEvents = AuditTrail.withCriteria { eq('className', 'test.Book') }
        bookEvents.size() == 1
    }

    void "Test remove element from a collection"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            def book = author.books.find { it.title == 'Mocking Jay' }
            author.removeFromBooks(book)
            book.delete()
        }

        then:
        Author.withNewTransaction {
            Author.findByName("Aaron").books.size() == 2
        }

        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        and: "the new value lists the values using the entityId override to show title"
        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue.contains('[id:Hunger Games]')
        e.newValue.contains('[id:Catching Fire]')

        and: "not the one we removed"
        !e.newValue.contains('[id:Mocking Jay]')
    }

    void "Test unsuccessful remove element from collection"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            def book = author.books.find { it.title == 'Mocking Jay' }
            // Doesn't succeed as Book#author isn't nullable
            author.removeFromBooks(book)
        }

        then:
        Author.withNewTransaction {
            Author.findByName("Aaron").books.size() == 3
        }
        AuditTrail.withNewTransaction {
            AuditTrail.count
        } == 0
    }

    void "Test add element to a collection"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.addToBooks(title: 'Something', description: 'Blah', pages: 900)
            author.save()
        }

        then: "another book"
        Author.withNewTransaction {
            Author.findByName("Aaron").books.size() == 4
        }

        def events = AuditTrail.withNewTransaction {
            AuditTrail.withCriteria { eq('className', 'test.Author') }
        }
        events.size() == 1

        and: "the new value lists the values using the entityId override to show title"
        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue.contains('[id:Hunger Games]')
        e.newValue.contains('[id:Catching Fire]')
        e.newValue.contains('[id:Mocking Jay]')
        e.newValue.contains('[id:Something]')

        and: "the book inserted is logged too"
        def bookEvents = AuditTrail.withCriteria { eq('className', 'test.Book') }
        bookEvents.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, ['id', 'version']).size()
        bookEvents.first().eventName == 'INSERT'
    }

    void "Test remove all elements from a collection"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.books*.delete()
            author.books = []
        }

        then:
        Author.withNewTransaction {
            Author.findByName("Aaron").books.size() == 0
        }

        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue == null
    }

    void "Test assign collection to null"() {
        when:
        Author.withNewTransaction {
            def author = Author.findByName("Aaron")
            author.books*.delete()
            author.books = null
        }

        then:
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == 1

        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue == null
    }
}
