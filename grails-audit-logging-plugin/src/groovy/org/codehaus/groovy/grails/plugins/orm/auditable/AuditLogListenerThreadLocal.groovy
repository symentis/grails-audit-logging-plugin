/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
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
