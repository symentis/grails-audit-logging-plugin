package test

import grails.plugins.orm.auditable.AuditEventType
import grails.plugins.orm.auditable.Auditable

class Resolution implements Auditable {

	String name

	@Override
	Set<AuditEventType> getLogIgnoreEvents() {
        [AuditEventType.UPDATE, AuditEventType.INSERT]
	}

	static constraints = {
	}
}
