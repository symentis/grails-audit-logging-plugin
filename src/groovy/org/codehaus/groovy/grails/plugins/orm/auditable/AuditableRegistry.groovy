package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * The auditableRegistry service provides a mechanism to more rapidly process
 * whether a class is auditable or not. It should <strong>never</strong> have
 * a class evicted from it. So it should be safe to have multiple threads reading
 * this service information.
 */
class AuditableRegistry {
    Map registry = new LinkedHashMap<Object,AuditableConfig>()

    public Boolean isAuditable(entity) {
        // TODO: make more efficient
        entity?.newInstance()?.auditable?true:false
    }

    Collection registered() {
        registry.keySet()
    }

    boolean isRegistered(domainClass) {
        registry.keySet().contains(domainClass)
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

    boolean register(clazz, AuditableConfig config) {
        // Is this elegant? I'm not sure. It is Functional(tm)
        isAuditable(clazz)?
            {-> registry.put(clazz,config); true }.call() : false

    }
}
