package com.codehaus.groovy.grails.plugins.orm.auditable;

import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent;
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogListener;
import org.codehaus.groovy.grails.plugins.orm.auditable.DomainEventListenerService;
import org.junit.Before
import grails.test.mixin.TestFor;

/**
 * @author Shawn Hartsock
 */
@TestFor(AuditLogEvent)
public class DomainEventListenerServiceTest {
    DomainEventListenerService domainEventListenerService
    AuditLogListener auditLogListener
    def logCount

    @Before
    void init() {
        logCount = 0
        auditLogListener.saveAuditLog = {AuditLogEvent audit ->
                logCount++
        }
        applicationContext.applicationEventMulticaster.addApplicationListener(domainEventListenerService)
    }

}
