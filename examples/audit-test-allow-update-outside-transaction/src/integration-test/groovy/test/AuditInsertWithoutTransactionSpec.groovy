package test

import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.testing.mixin.integration.Integration
import spock.lang.Shared
import spock.lang.Specification
import test.Author

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
            events.size() == TestUtils.getAuditableProperties(Author.gormPersistentEntity, defaultIgnoreList).size()
        }
    }
}

//class Listener extends AbstractPersistenceEventListener {
//    protected Listener(Datastore datastore) {
//        super(datastore)
//    }
//
//    @Override
//    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
//        def asf = Holders.applicationContext.getBean(AuditLoggingProcessManager)
//        Author.withSession { Session session ->
//            def tx = session.getTransaction()
////            new GrailsTransactionTemplate(Holders.applicationContext.getBean(PlatformTransactionManager), new DefaultTransactionDefinition()).execute({})
//        }
//    }
//
//    @Override
//    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
//        eventType.isAssignableFrom(PostInsertEvent)
//    }
//}