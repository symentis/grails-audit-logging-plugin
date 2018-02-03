package test

import grails.plugins.orm.auditable.AuditEventType
import grails.plugins.orm.auditable.Auditable

class Publisher implements Auditable {
    String code
    String name

    boolean active = false

    @Override
    String getLogEntityId() {
        "${code}|${name}"
    }

    @Override
    boolean isAuditLogEnabled(AuditEventType eventType) {
        active
    }

    static constraints = {
    }
}
