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
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener
import spock.lang.Unroll

class AuditNoLogSpec extends IntegrationSpec {

    void setup() {
        NoLog.auditable = true
    }

    void "Test NoLog insert logging"() {
        given:
        def entry = new NoLog(name: "Robert", description: "Shall not be logged")

        when:
          entry.save(flush: true, failOnError: true)

        then: "noLog entry is saved"
        entry.id

        and: "mo verbose audit logging is created"
        def events = AuditLogEvent.findAllByClassName('test.NoLog')
        events.size() == 0
    }
}
