auditLog {
    verbose = true
    defaultIgnore = ['version', 'lastUpdated', 'lastUpdatedBy']
    logFullClassName = true
    transactional = false
    defaultMask = ['ssn']
    logIds = true
    defaultActor = 'SYS'
    useDatasource = 'second' // store in "second" datasource
    idMapping = [generator:"uuid2", type:"string", length:36] // UUID id-type for AuditLogEvent
}

// Added by the Audit-Logging plugin:
grails.plugin.auditLog.auditDomainClassName = 'test.AuditTrail'


dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' 
}

hibernate_second {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' 
}

// environment specific settings
environments {
    development {
        dataSources {
          dataSource {
              dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
              url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
          }
          second {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb2;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
          }

        }
    }
    test {
      dataSources {
          // we test with several configured datasources. See GPAUDITLOGGING-64
          dataSource {
              dbCreate = "update"
              url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
          }
          second {
              dbCreate = "update"
              url = "jdbc:h2:mem:testDb2;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
          }

      }
    }
    production {
      dataSources {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=false
               validationQuery="SELECT 1"
               jdbcInterceptors="ConnectionState"
            }
        }
        second {
          dbCreate = "update"
          url = "jdbc:h2:mem:testDb2;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }

      }
    }
}


