package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.springframework.core.NamedThreadLocal
import org.springframework.transaction.support.TransactionSynchronizationManager
/**
 * Queue audit logging changes for flushing on transaction commit to ensure proper transactional semantics
 *
 * @author Aaron Long
 */
@Slf4j
@CompileStatic
class AuditLogQueueManager {
    private static final ThreadLocal<AuditLogTransactionSynchronization> threadLocal = new NamedThreadLocal<AuditLogTransactionSynchronization>("auditLog.synch")

    static void addToQueue(GormEntity auditInstance, AbstractPersistenceEvent event) {
        if (!TransactionSynchronizationManager.synchronizationActive) {
            // Seems like no transaction is active
            //  => Save audit entry right now
            // In Hibernate > 5.2 this can only happen by setting `hibernate.allow_update_outside_transaction: true`
            //
            // When audit domain class is in same datastore as a newly INSERTed entity we can't cause a flush of the session here.
            // This would cause a Hibernate AssertionFailure: "null id in test.Author entry (don't flush the Session after an exception occurs)"
            //
            //  => we can't cause a session flush of the session that is used to flush the changes to the observed entity
            //    => we can't use withNewTransaction because it reuses the current session
            //  => we should still use a transaction because in theory the audit domain could be in another datastore where allow_update_outside_transaction isn't set
            //
            //  => use withNewSession + withNewTransaction
            auditInstance.invokeMethod("withNewSession") {
                auditInstance.invokeMethod("withTransaction") {
                    auditInstance.save(failOnError: true)
                }
            }
            return
        }

        AuditLogTransactionSynchronization auditLogSync = TransactionSynchronizationManager.synchronizations.findResult {
            if (it instanceof AuditLogTransactionSynchronization) {
                return (AuditLogTransactionSynchronization) it
            }
            return null
        }

        if (!auditLogSync) {
            auditLogSync = new AuditLogTransactionSynchronization()
            TransactionSynchronizationManager.registerSynchronization(auditLogSync)
            log.trace("Registered new ${AuditLogTransactionSynchronization.simpleName} in TransactionSynchronizationManager")
        }

        auditLogSync.addToQueue(auditInstance)
    }
}

