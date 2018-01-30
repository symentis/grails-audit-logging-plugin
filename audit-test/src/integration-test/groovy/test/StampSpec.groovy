package test

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
@Rollback
class StampSpec extends Specification {
    void 'Stamp inserted object'() {
        given:
        def train = new Train(number: "10")

        when:
        train.save(flush: true)

        then:
        train.createdBy == 'SYS'
        train.lastUpdatedBy == 'SYS'
        train.dateCreated
        train.lastUpdated
        train.dateCreated == train.lastUpdated

        when:
        train.number = "20"
        train.save(flush: true)

        then:
        train.dateCreated != train.lastUpdated
    }
}
