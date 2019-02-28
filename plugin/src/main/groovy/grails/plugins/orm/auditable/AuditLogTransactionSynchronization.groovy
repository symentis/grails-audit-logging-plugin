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
        pendingAuditInstances << auditInstance
        if (log.isTraceEnabled()) {
            log.trace("Added $auditInstance to synchronization queue")
        }
    }

    @Override
    void afterCommit() {
        if (!pendingAuditInstances) {
            return
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Writing ${pendingAuditInstances.size()} pending audit instances in afterCommit()")
            }
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
