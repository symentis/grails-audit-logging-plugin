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

class AuditDsTowEntryInsertSpec extends IntegrationSpec {

    void setup() {
        DsTwoEntry.auditable = true
    }

    void "Test insert in auditLog-disabled datasource"() {
        given:
        def entry = new DsTwoEntry(name: "testMe", description: "Shall not be logged")
        assert entry.validate()

        when:
        entry.save(flush: true, failOnError: true)

        then: "entry is saved"
        entry.id

        and: "no verbose audit log entries are created"
        def events = AuditLogEvent.findAllByClassName('test.DsTwoEntry')
        events.size() == 0
    }
}
