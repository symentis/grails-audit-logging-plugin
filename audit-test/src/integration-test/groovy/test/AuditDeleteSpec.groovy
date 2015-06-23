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

import grails.plugins.orm.auditable.AuditLogEvent

import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*

@Integration
@Rollback
class AuditDeleteSpec extends Specification {
    void setupData() {
        Author.auditable = true

        def author = new Author(name: "Aaron", age: 37, famous: true)
        author.addToBooks(new Book(title: 'Hunger Games', description: 'Blah', pages: 400))
        author.addToBooks(new Book(title: 'Catching Fire', description: 'Blah', pages: 500))
        author.save(flush: true, failOnError: true)

        def publisher = new Publisher(code: 'ABC123', name: 'Random House', active: true)
        publisher.save(flush: true, failOnError: true)

        // Remove all logging of the inserts, we are focused on deletes here
        AuditLogEvent.where { id != null }.deleteAll()
        assert AuditLogEvent.count() == 0

        author.handlerCalled = ""
    }

    void "Test delete logging"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")
        assert author

        when:
        author.delete(flush: true, failOnError: true)

        then: "audit logging is created"
        def events = AuditLogEvent.findAllByClassName('test.Author')

        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - ['id', 'version']).size()

        def first = events.find { it.propertyName == 'age' }
        first.oldValue == "37"
        first.newValue == null
        first.eventName == 'DELETE'

        and: 'all books are deleted'
        def b1Events = AuditLogEvent.findAllByClassNameAndPersistedObjectId('test.Book', 'Hunger Games')
        b1Events.size() == (Book.gormPersistentEntity.persistentPropertyNames  - ['id', 'version']).size()

        def b2Events = AuditLogEvent.findAllByClassNameAndPersistedObjectId('test.Book', 'Catching Fire')
        b2Events.size() == (Book.gormPersistentEntity.persistentPropertyNames  - ['id', 'version']).size()
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
        def events = AuditLogEvent.findAllByClassName('test.Publisher')
        events.size() == resultCount

        where: "publisher active flag determines logging"
        activeFlag << [false, true]
        resultCount << [0, 3]
    }

    void "Test handler is called"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")

        when:
        author.delete(flush: true, failOnError: true)

        then: "verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('test.Author')
        events.size() == (Author.gormPersistentEntity.persistentPropertyNames  - ['id', 'version']).size()

        and:
        author.handlerCalled == "onDelete"
    }

    void "Test only handler is called"() {
        given:
        setupData()
        def author = Author.findByName("Aaron")
        Author.auditable = [handlersOnly: true]

        when:
        author.delete(flush: true, failOnError: true)

        then: "nothing logged"
        def events = AuditLogEvent.findAllByClassName('test.Author')
        events.size() == 0

        and:
        author.handlerCalled == "onDelete"
    }


}

