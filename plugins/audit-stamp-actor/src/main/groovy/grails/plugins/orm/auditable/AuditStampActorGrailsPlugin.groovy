package grails.plugins.orm.auditable

import grails.plugins.Plugin
import org.grails.datastore.mapping.core.Datastore

class AuditStampActorGrailsPlugin extends Plugin {
    def grailsVersion = '3.3.0 > *'

    def title = "Audit Stampable Actor Plugin"
    def authorEmail = "roos@symentis.com"
    def description = """ 
        Automatically stamp actor createdBy and lastUpdatedBy on domain objects.
    """
    String documentation = 'https://github.com/robertoschwald/grails-audit-logging-plugin'
    String license = 'APACHE'

    def developers = [
        [name: 'Robert Oschwald', email: 'roos@symentis.com'],
        [name: 'Elmar Kretzer', email: 'elkr@symentis.com'],
        [name: 'Aaron Long', email: 'longwa@gmail.com']
    ]

    def issueManagement = [system: 'GitHub', url: 'https://github.com/robertoschwald/grails-audit-logging-plugin/issues']
    def scm = [url: 'https://github.com/robertoschwald/grails-audit-logging-plugin']
    def loadAfter = ['auditLogging']

    // Register generic GORM listener
    @Override
    void doWithApplicationContext() {
        Set<String> excludedDataStores = grailsApplication.config.getProperty(
            'grails.plugin.auditLog.exludedDataSources'
        )?: [] as Set<String>

        applicationContext.getBeansOfType(Datastore).each { String key, Datastore datastore ->
            String dataSourceName = datastore.metaClass.getProperty(datastore, 'dataSourceName')

            if (!excludedDataStores.contains(dataSourceName)) {
                applicationContext.addApplicationListener(new StampActorListener(datastore, grailsApplication))
            }
        }
    }

    @Override
    Closure doWithSpring() {{->

    }}

    @Override
    void onConfigChange(Map<String, Object> event) {

    }
}