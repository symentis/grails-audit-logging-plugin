package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 *
 */

class AuditableRegistry {
    def registry = [:]
    
    public Boolean isAuditable(entity) {
        entity?.newInstance()?.auditable?true:false
    }

    AuditableConfig getAuditableConfig(entity) {
        final AuditableConfig auditableConfig
        def c = entity?.newInstance()?.auditable
        // TODO: make this more elegant
        if(c instanceof AuditableConfig) {
            auditableConfig = c
        }
        else if(c instanceof Map) {
            auditableConfig = new AuditableConfig(config(c))
        }
        else {
            auditableConfig = new AuditableConfig(config())
        }
        return auditableConfig
    }

    AuditableConfig getConfiguration(clazz) {
        registry[clazz.getCanonicalName()]
    }

    public Boolean register(clazz) {
        isAuditable(clazz)?
            register(clazz,getAuditableConfig(clazz)) : false

    }

    Map config() {
        config([:])
    }
    
    Map config(Map config) {
        config.handlersOnly = config.handlersOnly ?: false
        config.ignoreList = config.ignoreList ?: ['version','lastUpdated']
        return config
    }

    Boolean register(clazz, AuditableConfig config) {
        // Is this elegant? I'm not sure. It is Functional(tm)
        isAuditable(clazz)?
            {-> registry.put(clazz,config); true }.call() : false

    }
}
