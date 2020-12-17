package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager

@Slf4j
@CompileStatic
class AuditLogTransactionSynchronization extends TransactionSynchronizationAdapter {
    private List<GormEntity> pendingAuditInstances = []

    void addToQueue(GormEntity auditInstance) {
        if (!TransactionSynchronizationManager.synchronizationActive) {
            // Seems like no transaction is active => Save audit instance without transaction as well
            // withSession to ensure we have a session when using multiple datasources
            AuditLogListenerUtil.getAuditDomainClass().invokeMethod("withSession") {
                auditInstance.save(failOnError: true)
            }
            return
        }
        if (!pendingAuditInstances) {
            TransactionSynchronizationManager.registerSynchronization(this)
        }
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
            AuditLogListenerUtil.getAuditDomainClass().invokeMethod("withNewTransaction") {
                for (GormEntity entity in pendingAuditInstances) {
                    entity.save(failOnError: true)
                }
            }
        }
        finally {
            pendingAuditInstances.clear()
        }
    }
}
