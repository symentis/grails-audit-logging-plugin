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

/**
 * Test Audit Logging with handlersOnly auditing.
 */
class AuditDeleteHandlersOnlySpec extends IntegrationSpec {

    void setup() {
        def jfk = new Airport(code: 'JFK', name: 'John F Kennedy International')
        jfk.addToRunways(new Runway(length: 300, width: 50))
        jfk.save(flush: true, failOnError: true)

        def boeing777 = new Aircraft(type: 'BE-777', description: "Boeing 777")
        boeing777.save(flush: true)

        def codes = [new Code(tag: 'AircraftType', code: 747, value: 'BE-747', description: "Boeing 747"),
                     new Code(tag: 'AircraftType', code: 432, value: 'AB-432', description: "Air Bus 432")
        ]
        codes*.save(flush: true)

        // Remove all logging of the inserts, we are focused on deletes here
        org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent.where { id != null }.deleteAll()
        assert org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent.count() == 0

    }

    void "test onDelete handler has an id available"() {
        given:
        def jfk = Airport.findByCode("JFK")
        assert jfk

        when:
        jfk.delete(flush: true, failOnError: true)

        then: "handlerMap contains id and other fields"
        jfk.id
        jfk.handlerMap
        jfk.handlerMap.keySet().containsAll(['code', 'name', 'id', 'runways'])
    }

    void "test id not available for assigned identifier"() {
        given:
        def boeing777 = Aircraft.findByType('BE-777')
        assert boeing777

        when:
        boeing777.delete(flush: true, failOnError: true)

        then: "handlersMap contains the assigned identifier"
        boeing777.handlersMap
        boeing777.handlersMap.keySet().containsAll(['type', 'description'])
        !boeing777.handlersMap.containsKey('id')
    }

    void "test id not available in onDelete handler map for domain class with composite primary key"() {
        given:
        def codes = Code.findAllByTag('AircraftType')
        assert codes
        assert codes.size() == 2

        when:
        codes*.delete(flush: true)

        then:
        [747, 432].containsAll(Code.all*.code)
        codes.each { assert !it.id }
    }

}