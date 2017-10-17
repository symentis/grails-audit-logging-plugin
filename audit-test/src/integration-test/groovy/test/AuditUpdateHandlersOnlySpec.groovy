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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback


/**
 * Test Audit Logging with handlersOnly auditing.
 */
import spock.lang.Issue
import spock.lang.Specification

@Integration
@Rollback
class AuditUpdateHandlersOnlySpec extends Specification {

    @Issue('Make identifiers available in the maps for onChange Event')
    void "test onChange handler called"() {
        given:
        def jfk = new Airport(code: 'JFK', name: 'John F Kennedy International')
        jfk.save(flush: true)

        when: 'Airport updated'
        Airport updated = Airport.findByCode('JFK')
        updated.name = 'La Gaurdia Airport'
        updated.save(flush: true)

        then:
        updated.id
        updated.handlerMap && jfk.handlerOldMap
        ['name', 'id'].containsAll(updated.handlerMap.keySet())
        ['name', 'id'].containsAll(updated.handlerOldMap.keySet())
    }

    void "test id not available for assigned identifier in onChange event"() {
        given:
        def boeing777 = new Aircraft(type: 'BE-777', description: "Boeing 777")
        boeing777.save(flush: true)

        when:
        def updated = Aircraft.findByType('BE-777')
        updated.description = "Boeing 777 Latest model"
        updated.save(flush: true)

        then:
        !updated.id
        !updated.handlersMap.containsKey('id')
        !updated.handlersOldMap.containsKey('id')
    }

    void "test id not available in onChange handler map for domain class with composite primary key"() {
        given:
        def codes = [
            new Code(tag: 'AircraftType', code: 747, value: 'BE-747', description: "Boeing 747"),
            new Code(tag: 'AircraftType', code: 432, value: 'AB-432', description: "Air Bus 432")
        ]
        codes*.save(flush: true)

        when:
        Code toUpdate = Code.get(new Code(tag: 'AircraftType', code: 747))
        toUpdate.description = "Boeing 747 Latest Model"
        toUpdate.save(flush: true)

        then:
        [747, 432].containsAll(Code.all*.code)
        toUpdate.every { !it.id }

        and:
        toUpdate.handlersMap.containsKey('description') && toUpdate.handlersOldMap.containsKey('description')
        !toUpdate.handlersMap.containsKey('id') && !toUpdate.handlersOldMap.containsKey('id')
    }
}