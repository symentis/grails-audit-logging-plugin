package test

import grails.test.spock.IntegrationSpec

/**
 * Test Audit Logging with handlersOnly auditing.
 */
class AuditInsertHandlersOnlySpec extends IntegrationSpec {

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
            codes.each { assert !it.id }
    }
}