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

import grails.gorm.transactions.Transactional
import grails.plugins.orm.auditable.AuditLogContext
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
/**
 * As auditing now happens during Transaction commit, we must
 * manage auditable transactions and cleanup on our own.
 */
@Integration
@Transactional
class AuditDeleteSpec extends Specification {

  private Logger logg = LoggerFactory.getLogger(AuditDeleteSpec.class);

  @Shared
  def defaultIgnoreList

  void setup() {
    defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.excluded?.asImmutable() ?: []
  }

  void setupData() {
    logg.debug("SetupData()")
    Author.withNewTransaction {
      AuditLogContext.withoutAuditLog {
        // Cleanup, first
        Book.where { id != null }.deleteAll()
        Author.where { id != null }.deleteAll()
        Publisher.where { id != null }.deleteAll()
        Resolution.where { id != null }.deleteAll()
        assert Book.count() == 0
        assert Author.count() == 0
        assert Publisher.count() == 0
        assert Resolution.count() == 0
        //
        def author = new Author(name:"Aaron", age:37, famous:true)
        author.addToBooks(new Book(title:'Hunger Games', description:'Blah', pages:400))
        author.addToBooks(new Book(title:'Catching Fire', description:'Blah', pages:500))
        author.save(flush:true, failOnError:true)
        //
        def publisher = new Publisher(code:'ABC123', name:'Random House', active:true)
        publisher.save(flush:true, failOnError:true)
      }
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

    when:
    Author.withNewTransaction {
      def author = Author.findByName("Aaron")
      assert author
      // Delete in own transaction, as audit log is written on commit.
      author.delete(flush:true, failOnError:true)
    }

    then: 'Author Audit Events are written'
    List<AuditTrail> events = AuditTrail.withCriteria { eq('className', 'test.Author') } as List<AuditTrail>
    events.size() == new Author().getAuditablePropertyNames().size() - 1 // persistentCollections (books) are not logged on delete anymore. See #153

    def first = events.find { it.propertyName == 'age' }
    first.oldValue == "37"
    first.newValue == null
    first.eventName == 'DELETE'

    and: 'Book Audit Events are written'
    List<AuditTrail> b1Events = AuditTrail.withCriteria { eq('className', 'test.Book'); eq('persistedObjectId', 'Hunger Games') } as List<AuditTrail>
    b1Events.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size()

    def b2Events = AuditTrail.withCriteria {
      eq('className', 'test.Book'); eq('persistedObjectId', 'Catching Fire')
    }
    b2Events.size() == TestUtils.getAuditableProperties(Book.gormPersistentEntity, defaultIgnoreList).size()
  }

  @Unroll
  void "Test conditional delete logging active:#activeFlag resultCount:#resultCount"() {
    given:
    setupData()

    when:
    Publisher.withNewTransaction {
      def publisher = Publisher.findByName("Random House")
      assert publisher
      publisher.active = activeFlag
      // Delete in own transaction, as audit log is written on commit.
      publisher.delete(flush:true, failOnError:true)
    }

    then:
    def publisher = Publisher.findByName("Random House")
    assert !publisher
    List<AuditTrail> events = AuditTrail.withCriteria { eq('className', 'test.Publisher') } as List<AuditTrail>
    events.size() == resultCount

    where: "publisher activeFlag determines logging"
    activeFlag | resultCount
    true       | 3
    false      | 0
  }

  void "Test only delete events are logged"() {
    given:
    Resolution.withNewTransaction {
      Resolution resolution = new Resolution()
      resolution.name = "One for all"
      resolution.save(flush:true, failOnError:true)
      // update
      resolution.name = "One for all and all for one"
      resolution.save(flush:true, failOnError:true)
    }

    when: "delete resolution"
    Resolution.withNewTransaction {
      Resolution resolution = Resolution.findByName("One for all and all for one")
      assert resolution
      resolution.delete(flush:true, failOnError:true)
    }

    then: "One event logged"
    def events = AuditTrail.withCriteria { eq('className', 'test.Resolution') }
    events.size() == 1
    and:
    events.get(0).eventName == "DELETE"
  }

  void "Test defaultExcluded properties"() {
    given:
    setupData()

    when:
    Author.withNewTransaction {
      def author = Author.findByName("Aaron")
      author.delete(flush:true, failOnError:true)
    }

    then: "ignored properties not logged"
    def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
    events.size() == 6 // persistentCollections (books) are not logged on delete anymore. See #153
    ['name', 'publisher', 'ssn', 'age', 'famous', 'dateCreated'].each { name ->
      assert events.find { it.propertyName == name }, "${name} was not logged"
    }
    ['version', 'lastUpdated', 'lastUpdatedBy'].each { name ->
      assert !events.find { it.propertyName == name }, "${name} was logged"
    }

  }

  void "Test context blacklist"() {
    given:
    setupData()

    when:
    Author.withNewTransaction {
      def author = Author.findByName("Aaron")
      AuditLogContext.withConfig(excluded:['famous', 'age', 'dateCreated']) {
        author.delete(flush:true, failOnError:true)
      }
    }

    then: "ignored properties not logged"
    List<AuditTrail> events = AuditTrail.withCriteria { eq('className', 'test.Author') } as List<AuditTrail>
    events.size() == 6 // persistentCollections (books) are not logged on delete anymore. See #153
    ['name', 'publisher', 'ssn', 'lastUpdated', 'lastUpdatedBy', 'version'].each { name ->
      assert events.find { it.propertyName == name }, "${name} was not logged"
    }
    ['famous', 'age', 'books', 'dateCreated'].each { name ->
      assert !events.find { it.propertyName == name }, "${name} was logged"
    }
  }

  void "Test context whitelist"() {
    given:
    setupData()

    when:
    Author.withNewTransaction {
      def author = Author.findByName("Aaron")
      AuditLogContext.withConfig(included:['famous', 'age', 'dateCreated']) {
        author.delete(flush:true, failOnError:true)
      }
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

    when:
    Author.withNewTransaction {
      def author = Author.findByName("Aaron")
      AuditLogContext.withConfig(included:['famous', 'age', 'dateCreated'], excluded:['famous', 'age']) {
        author.delete(flush:true, failOnError:true)
      }
    }

    then:
    List<AuditTrail> events = AuditTrail.withCriteria { eq('className', 'test.Author') } as List<AuditTrail>
    events.size() == 3
    ['famous', 'age', 'dateCreated'].each { name ->
      assert events.find { it.propertyName == name }, "${name} was not logged"
    }
  }
}

