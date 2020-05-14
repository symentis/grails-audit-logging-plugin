package grails.plugins.orm.auditable

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.core.NamedThreadLocal

/**
 * Queue audit logging changes for flushing on transaction commit to ensure proper transactional semantics
 *
 * @author Aaron Long
 */
@Slf4j
@CompileStatic
class AuditLogQueueManager {
    private static final ThreadLocal<AuditLogTransactionSynchronization> threadLocal = new NamedThreadLocal<AuditLogTransactionSynchronization>("auditLog.synch")

    static void addToQueue(GormEntity auditInstance) {
        AuditLogTransactionSynchronization auditLogSync = threadLocal.get()
        if (!auditLogSync) {
            auditLogSync = new AuditLogTransactionSynchronization()
            threadLocal.set(auditLogSync)
        }

        auditLogSync.addToQueue(auditInstance)
    }
}

