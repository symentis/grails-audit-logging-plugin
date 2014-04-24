package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * ThreadLocals for AuditLog
 *
 * User: Robert Oschwald
 * Date: 24.04.2014 05:57:44
 *
 */
class AuditLogListenerThreadLocal {
  public static final ThreadLocal auditLogEnabledThreadLocal = new ThreadLocal<Boolean>();
  public static final ThreadLocal auditLogVerboseThreadLocal = new ThreadLocal<Boolean>();

  public static void setAuditLogDisabled(Boolean enabled) {
    auditLogEnabledThreadLocal.set(enabled);
  }

  public static boolean getAuditLogDisabled() {
    return auditLogEnabledThreadLocal.get();
  }

  public static void clearAuditLogDisabled() {
    auditLogEnabledThreadLocal.remove();
  }

  public static void setAuditLogNonVerbose(Boolean enabled) {
    auditLogVerboseThreadLocal.set(enabled);
  }

  public static boolean getAuditLogNonVerbose() {
    return auditLogVerboseThreadLocal.get();
  }

  public static void clearAuditLogNonVerbose() {
    auditLogVerboseThreadLocal.remove();
  }
}
