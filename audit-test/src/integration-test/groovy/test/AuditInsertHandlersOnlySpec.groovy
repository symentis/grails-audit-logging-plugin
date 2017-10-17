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


/**
 * Test Audit Logging with handlersOnly auditing.
 */
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

@Integration
@Rollback
class AuditInsertHandlersOnlySpec extends Specification {

    void "test onSave handler called"() {
        given:
            def jfk = new Airport(code: 'JFK', name: 'John F Kennedy International')
            jfk.addToRunways(new Runway(length: 300, width: 50))

        when:
            jfk.save(flush: true)

        then:
            jfk.id
            jfk.handlerMap
            ['code', 'name', 'id', 'runways'].containsAll(jfk.handlerMap.keySet())
    }

    void "test id not available for assigned identifier"() {
        given:
            def boeing777 = new Aircraft(type: 'BE-777', description: "Boeing 777")

        when:
            boeing777.save(flush: true)

        then:
            !boeing777.id
            !boeing777.handlersMap.containsKey('id')
    }

    void "test id not available in onSave handler map for domain class with composite primary key"() {
        given:
            def codes = [new Code(tag: 'AircraftType', code: 747, value: 'BE-747', description: "Boeing 747"),
                    new Code(tag: 'AircraftType', code: 432, value: 'AB-432', description: "Air Bus 432")
            ]

        when:
            codes*.save(flush: true)

        then:
            [747, 432].containsAll(Code.all*.code)
            codes.every { !it.id }
    }
}