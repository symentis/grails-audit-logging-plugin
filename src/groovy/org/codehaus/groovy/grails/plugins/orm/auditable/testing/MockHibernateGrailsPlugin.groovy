package org.codehaus.groovy.grails.plugins.orm.auditable.testing

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport

/**
 *
 */
class MockHibernateGrailsPlugin {
    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [dataSource: version,
            i18n: version,
            core: version,
            domainClass: version]

    def artefacts = [new AnnotationDomainClassArtefactHandler()]
    def loadAfter = ['controllers']
    def doWithSpring = HibernatePluginSupport.doWithSpring
    def doWithDynamicMethods = HibernatePluginSupport.doWithDynamicMethods
}