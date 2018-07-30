package grails.plugins.orm.auditable

import grails.plugins.orm.auditable.domain.Person
import grails.plugins.orm.auditable.resolvers.DefaultAuditRequestResolver
import grails.testing.gorm.DataTest
import spock.lang.Specification

class StampListenerSpec extends Specification implements DataTest {

    void setupSpec() {
        applicationContext.addApplicationListener(
            new StampListener(dataStore, grailsApplication)
        )
    }

    @Override
    Class[] getDomainClassesToMock() {
        [Person] as Class[]
    }

    @Override
    Closure doWithSpring() {
        return {
            auditRequestResolver(DefaultAuditRequestResolver)
        }
    }

    @Override
    Closure doWithConfig() {
        return { config ->
            config.grails.plugin.auditLog.stampEnabled = true
            config.grails.plugin.auditLog.defaultActor = 'SYS'
        }
    }

    void 'should stamp createdBy and lastUpdatedBy on save'() {
        given:
            Person p = new Person(name: 'tkvw')
            p.save(flush: true)
        expect:
            p.createdBy == 'SYS'
            p.lastUpdatedBy == 'SYS'
    }

    void 'should stamp lastUpdatedBy on update'() {
        given:
            Person p = new Person(name: 'tkvw')
            p.save(flush: true)
        expect:
            p.createdBy == 'SYS'
            p.lastUpdatedBy == 'SYS'
        when:
            AuditLogContext.withConfig(defaultActor: 'foo') {
                p.name = 'other'
                p.save(flush: true)
            }
        then:
            p.createdBy == 'SYS'
            p.lastUpdatedBy == 'foo'

    }

}

