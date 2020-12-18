package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager

import java.util.function.Function

@Slf4j
@CompileStatic
class AuditLogTransactionSynchronization extends TransactionSynchronizationAdapter {
    private List<GormEntity> pendingAuditInstances = []

    Function<Integer, Void> afterCompletion = null

    void addToQueue(GormEntity auditInstance) {
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
        if (!pendingAuditInstances) {
            TransactionSynchronizationManager.registerSynchronization(this)
        }
        // Otherwise, if we have a transaction queue this instance.
        // Save them all when the transaction where changes where made commits.
        pendingAuditInstances << auditInstance
        log.trace("Added {} to synchronization queue", auditInstance)
    }

    @Override
    void afterCommit() {
        if (!pendingAuditInstances) {
            return
        }
        try {
            log.debug("Writing {} pending audit instances in afterCommit()", pendingAuditInstances.size())
            // Use withNewSession + withTransaction here as well to be completely independent from user session
            pendingAuditInstances[0].invokeMethod("withNewSession") {
                pendingAuditInstances[0].invokeMethod("withTransaction") {
                    for (GormEntity entity in pendingAuditInstances) {
                        entity.save(failOnError: true)
                    }
                }
            }
        }
        finally {
            pendingAuditInstances.clear()
        }
    }

    @Override
    void afterCompletion(int status) {
        afterCompletion?.apply(status)
    }
}
