package grails.plugins.orm.auditable

import grails.plugins.orm.auditable.domain.Airplane
import grails.plugins.orm.auditable.domain.AuditTrail
import grails.plugins.orm.auditable.resolvers.DefaultAuditRequestResolver
import grails.testing.gorm.DataTest
import spock.lang.Specification

class AuditLogListenerSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [AuditTrail, Airplane] as Class[]
    }

    void setupSpec() {
        applicationContext.addApplicationListener(
            new AuditLogListener(dataStore, grailsApplication)
        )
    }

    @Override
    Closure doWithConfig() {
        return { config ->
            config.grails.plugin.auditLog.auditDomainClassName = 'grails.plugins.orm.auditable.domain.AuditTrail'
            config.grails.plugin.auditLog.defaultActor = 'SYS'
            config.grails.plugin.auditLog.verbose = true
            config.grails.plugin.auditLog.excluded = ['version']
        }
    }

    @Override
    Closure doWithSpring() {
        return {
            auditRequestResolver(DefaultAuditRequestResolver)
        }
    }

    void 'should store audit events on insert'() {
        given:
            Airplane airplane = new Airplane(
                make: 'Airbus',
                number: '1'
            )
            airplane.save(flush: true)
        expect:
            AuditTrail.count() == 2
    }
    void 'should rollback store audit events on insert'() {
        expect: 'implement me'
        true
    }
}
