package test

import grails.core.GrailsApplication
import grails.gorm.transactions.Rollback
import grails.plugins.orm.auditable.resolvers.DefaultAuditRequestResolver
import grails.spring.BeanBuilder
import grails.testing.mixin.integration.Integration
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@Integration
@Rollback
class StampSpec extends Specification {
    GrailsApplication grailsApplication

    @DirtiesContext
    void 'Stamp inserted with custom request resolver'() {
        given:
        def train = new Train(number: "10")

        and: "custom request resolver"
        BeanBuilder bb = new BeanBuilder()
        bb.beans {
            auditRequestResolver(CustomRequestResolver)
        }
        bb.registerBeans(grailsApplication.mainContext)

        when:
        train.save(flush: true, failOnError: true)

        then:
        train.createdBy == 'Aaron'
        train.lastUpdatedBy == 'Aaron'
        train.dateCreated
        train.lastUpdated

        when:
        train.number = "20"
        train.save(flush: true)

        then:
        train.dateCreated != train.lastUpdated
    }

    void 'Stamp inserted with default request resolver'() {
        given:
        def train = new Train(number: "10")

        when:
        train.save(flush: true)

        then:
        train.createdBy == 'SYS'
        train.lastUpdatedBy == 'SYS'
        train.dateCreated
        train.lastUpdated

        when:
        train.number = "20"
        train.save(flush: true)

        then:
        train.dateCreated != train.lastUpdated
    }
}

class CustomRequestResolver extends DefaultAuditRequestResolver {
    @Override
    String getCurrentActor() {
        "Aaron"
    }
}
