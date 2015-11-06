grails {
  plugin {
    auditLog {
      verbose = true
      defaultIgnore = ['version', 'lastUpdated', 'lastUpdatedBy']
      logFullClassName = true
      transactional = false
      defaultMask = ['password']
      logIds = true
      defaultActor = 'SYS'
      auditDomainClassName = null
    }
  }
}