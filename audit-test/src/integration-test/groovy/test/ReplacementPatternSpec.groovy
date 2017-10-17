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

import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.test.mixin.integration.Integration
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Shared
import spock.lang.Specification

/**
 * Test replacementPattern config option
 */
@Integration
@Rollback
class ReplacementPatternSpec extends Specification {

    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.defaultIgnore?.asImmutable() ?: []
        Author.auditable = true
        AuditTrail.withNewSession { AuditTrail.executeUpdate('delete from AuditTrail') }
    }

    void "Test replacementPattern converts a.b.MySample to .MySample"() {
        given:
        def author = new Author(name: "a.b.MySample", age: 1, famous: true)

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()
        def first = events.find { it.propertyName == 'name' }
        first.oldValue == null
        first.newValue == ".MySample"
    }

    void "Test replacementPattern does not convert c.d.MySample"() {
        given:
        def author = new Author(name: "c.d.MySample", age: 1, famous: true)

        when:
        author.save(flush: true, failOnError: true)

        then: "author is saved"
        author.id

        and: "verbose audit logging is created"
        def events = AuditTrail.withCriteria { eq('className', 'test.Author') }
        events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()

        def first = events.find { it.propertyName == 'name' }
        first.oldValue == null
        first.newValue == "c.d.MySample"
    }
}
