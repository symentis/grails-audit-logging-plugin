package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.transaction.support.TransactionSynchronizationAdapter

@Slf4j
@CompileStatic
class AuditLogTransactionSynchronization extends TransactionSynchronizationAdapter {
    private List<GormEntity> pendingAuditInstances = []

    void addToQueue(GormEntity auditInstance) {
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
        log.trace("afterComplication called with status $status")
    }
}
