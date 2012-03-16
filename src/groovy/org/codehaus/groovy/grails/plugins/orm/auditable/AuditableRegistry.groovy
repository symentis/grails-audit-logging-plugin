package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 *
 */

class AuditableRegistry {
    def registry = [:]
    
    public boolean isAuditable(entity) {
        boolean is = false
        try {
            if( entity.newInstance().auditable ) {
                is = true
            }
        } catch (Throwable t ) {
            is = false
        }
        return is
    }

    AuditableConfig getConfiguration(Class clazz) {
        registry[clazz.getCanonicalName()]
    }

    void register(Class clazz, Map config) {
        config.handlersOnly = config.handlersOnly?:false
        config.ignoreList = config.ignoreList?:[]
        register(clazz,new AuditableConfig(config))
    }
    
    void register(Class clazz, AuditableConfig config) {
        registry.put(clazz.getCanonicalName(),config)
    }
}
