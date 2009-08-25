package org.codehaus.groovy.grails.plugins.orm.auditable

import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

class AuditLogEventController {

  def index = { redirect(action: list, params: params) }

  // the delete, save and update actions only accept POST requests
  static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

  def list = {
    if (!params.max) params.max = 10
    [auditLogEventInstanceList: AuditLogEvent.list(params), auditLogEventInstanceTotal: AuditLogEvent.count()]
  }

  def show = {
    def auditLogEvent = AuditLogEvent.get(params.id)

    if (!auditLogEvent) {
      flash.message = "AuditLogEvent not found with id ${params.id}"
      redirect(action: list)
    }
    else { return [auditLogEventInstance: auditLogEvent] }
  }

  def delete = {
    redirect(action: list)
  }

  def edit = {
    redirect(action: list)
  }

  def update = {
    redirect(action: list)
  }

  def create = {
    redirect(action: list)
  }

  def save = {
    redirect(action: list)
  }
}