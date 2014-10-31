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

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

class AuditUpdateCollectionSpec extends IntegrationSpec {
    void setup() {
        Author.auditable = true

        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
        author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
        author.addToBooks(new Book(title: 'Mocking Jay', description: 'Blah', pages: 600))
        author.save(flush: true, failOnError: true)

        // Remove all logging of the inserts, we are focused on updates here
        AuditLogEvent.where { id != null }.deleteAll()
        assert AuditLogEvent.count() == 0

        author.handlerCalled = ""
    }

    void "Test update property on an instance saved via cascade"() {
        given:
        def author = Author.findByName("Aaron")
        def book = author.books.asList().first() // do not use first() directly on books, as this tests needs to run on Grails 2.x, too

        when:
        book.description = "Woo"
        author.save(flush: true, failOnError: true)

        then: "the author didn't change"
        def events = AuditLogEvent.findAllByClassName('test.Author')
        events.size() == 0

        and: "the book did"
        def bookEvents = AuditLogEvent.findAllByClassName('test.Book')
        bookEvents.size() == 1
    }

    void "Test remove element from a collection"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.removeFromBooks(author.books.find { it.title == 'Mocking Jay' })
        author.save(flush: true, failOnError: true)

        then:
        author.books.size() == 2

        def events = AuditLogEvent.findAllByClassName('test.Author')
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

    void "Test add element to a collection"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.addToBooks(new Book(title: 'Something', description: 'Blah', pages: 900))
        author.save(flush: true, failOnError: true)

        then: "another book"
        author.books.size() == 4

        def events = AuditLogEvent.findAllByClassName('test.Author')
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
        def bookEvents = AuditLogEvent.findAllByClassName('test.Book')
        bookEvents.size() == Book.gormPersistentEntity.persistentPropertyNames.size()
        bookEvents.first().eventName == 'INSERT'
    }

    void "Test remove all elements from a collection"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.books.clear()
        author.save(flush: true, failOnError: true)

        then:
        author.books.size() == 0

        def events = AuditLogEvent.findAllByClassName('test.Author')
        events.size() == 1

        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue == null

        and: "no delete orphan so books are NOT changed"
        def bookEvents = AuditLogEvent.findAllByClassName('test.Book')
        bookEvents.size() == 0
    }

    void "Test assign collection to null"() {
        given:
        def author = Author.findByName("Aaron")

        when:
        author.books = null
        author.save(flush: true, failOnError: true)

        then:
        def events = AuditLogEvent.findAllByClassName('test.Author')
        events.size() == 1

        def e = events.first()
        e.eventName == 'UPDATE'
        e.propertyName == 'books'
        e.oldValue == 'N/A'
        e.newValue == null
    }
}
