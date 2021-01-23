package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.orm.hibernate.HibernateDatastore
import org.hibernate.Transaction
import org.hibernate.action.spi.AfterTransactionCompletionProcess
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.internal.SessionImpl

import java.util.concurrent.ConcurrentHashMap

/**
 * Queue audit logging changes for flushing on transaction commit to ensure proper transactional semantics
 *
 * @author Aaron Long
 */
@Slf4j
@CompileStatic
class AuditLogQueueManager {

    private static final Map<Transaction, AuditLogTransactionSynchronization> auditProcesses = new ConcurrentHashMap<>()

    static void addToQueue(GormEntity auditInstance, AbstractPersistenceEvent event) {
        if (!(event.source instanceof HibernateDatastore)) {
            log.warn("Can't handle audit event for entity from unsupported datastore ${event.source.class.name}")
            return
        }
        SessionImpl session = (SessionImpl) ((HibernateDatastore) event.source).sessionFactory.currentSession

        if (!session.transactionInProgress) {
            // Seems like no transaction is active
            //  => Save audit entry right now
            // In Hibernate > 5.2 this can only happen by setting `hibernate.allow_update_outside_transaction: true`
            //
            // When audit domain class is in same datastore as a newly INSERTed entity we can't cause a flush of the session here.
            // This would cause a Hibernate AssertionFailure: "null id in test.Author entry (don't flush the Session after an exception occurs)"
            //
            //  => we can't cause a session flush of the session that is used to flush the changes to the observed entity
            //    => we can't use withNewTransaction because it reuses the current session
            //  => we should still use a transaction because in theory the audit domain could be in another datastore
            // where allow_update_outside_transaction isn't set
            //
            //  => use withNewSession + withNewTransaction
            auditInstance.invokeMethod("withNewSession") {
                auditInstance.invokeMethod("withTransaction") {
                    auditInstance.save(failOnError: true)
                }
            }
            return
        }


        Transaction transaction = (Transaction) session.transaction

        AuditLogTransactionSynchronization auditProcess = auditProcesses[transaction]
        if (auditProcess == null) {
            auditProcess = auditProcesses[transaction] = new AuditLogTransactionSynchronization()
            // Roughly this is like a Spring transaction synchronization (TransactionSynchronizationManager.registerSynchronization)
            // The difference is that even when using nested transactions like
            // DomainInDatastoreOne.withNewTransaction {
            //   DomainInDatastoreTwo.withNewTransaction {
            //     <some operation on DomainInDatastoreOne> + flush
            //   }
            // }
            // we are able to register a synchroniation on the OUTER transaction even though this code is running while the
            // INNER transaction is active.
            //
            // Just using Spring Transactions and/or GORM would be better because it would allow to be datastore agnostic
            // But simply using TransactionSynchronizationManager.registerSynchronization doesn't work because it would 
            // register the synchronisation in the innermost transaction.
            //
            // TODO: Find GORM agnostic way of doing this
            //       If we don't find a GORM agnostic way we need to abstract this implementation away e.g. auditlogging-hibernate
            session.actionQueue.registerProcess(
              new AfterTransactionCompletionProcess() {
                  @Override
                  void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session2) {
                      if (success && auditProcesses.remove(transaction)) {
                          auditProcess.afterCommit()
                      }
                  }
              }
            )
        }

        auditProcess.addToQueue(auditInstance)
    }
}
