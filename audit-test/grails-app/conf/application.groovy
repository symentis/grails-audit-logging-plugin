
grails {
  plugin {
    auditLog {
      verbose = true
      defaultIgnore = ['version', 'lastUpdated', 'lastUpdatedBy']
      logFullClassName = true
      transactional = false
      defaultMask = ['ssn']
      logIds = true
      defaultActor = 'SYS'
      useDatasource = 'second' // store in "second" datasource
      replacementPatterns = ["a.b":""]
      TRUNCATE_LENGTH=255 // deprecated warning must occur.
      truncateLength=1000000
    }
  }
}

// Added by the Audit-Logging plugin:
grails.plugin.auditLog.auditDomainClassName = 'test.AuditTrail'

hibernate_second {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' 
}

