package test

import grails.plugins.orm.auditable.AuditLogContext
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.testing.mixin.integration.Integration
import spock.lang.Shared
import spock.lang.Specification

@Integration
class AuditInsertWithoutTransactionSpec extends Specification {

    @Shared
    def defaultIgnoreList

    void setup() {
        defaultIgnoreList = ['id'] + AuditLoggingConfigUtils.auditConfig.excluded?.asImmutable() ?: []
        AuditTrail.withNewTransaction {
            AuditTrail.executeUpdate('delete from AuditTrail')
        }
    }

    def "test insert without transaction"() {
        Author.withNewSession {
            new Author(name: "Aaron", age: 37, famous: true).save(flush: true, failOnError: true)
        }

        expect:
        Author.withNewSession {
            def events = AuditTrail.findAllByClassName("test.Author")
            events.size()
        } == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()
    }

    def "test insert without transaction second datasource"() {
        AuditLogContext.withConfig(auditDomainClassName: AuditTrailSecondDatasource.canonicalName) {
            Author.withNewSession {
                new Author(name: "Aaron", age: 37, famous: true).save(flush: true, failOnError: true)
            }
        }

        expect:
        AuditTrailSecondDatasource.withNewSession {
            def events = AuditTrailSecondDatasource.findAllByClassName("test.Author")
            events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()
        }
    }

    def "test transaction synchronization is active but not for changed domain"() {
        expect:
        Author.withNewSession {
            EntityInSecondDatastore.withNewTransaction {
                new Author(name: "Aaron", age: 37, famous: true).save(flush: true, failOnError: true)

                // We have a transaction active but not for the session that the Author is saved in
                // => AuditTrail needs to be saved immediately
                AuditTrail.withNewSession {
                    AuditTrail.count
                }
            }
        } > 0
    }
}