grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {

        compile 'org.hibernate:hibernate-core:3.6.10.Final', {
            excludes 'ant', 'antlr', 'cglib', 'commons-collections', 'commons-logging', 'commons-logging-api',
                     'dom4j', 'h2', 'hibernate-commons-annotations', 'hibernate-jpa-2.0-api', 'hibernate-validator',
                     'javassist', 'jaxb-api', 'jaxb-impl', 'jcl-over-slf4j', 'jboss-jacc-api_JDK4', 'jta',
                     'junit', 'slf4j-api', 'slf4j-log4j12', 'validation-api'
        }
        
        compile 'org.hibernate:hibernate-commons-annotations:3.2.0.Final', {
            excludes 'commons-logging', 'commons-logging-api', 'jcl-over-slf4j', 'junit', 'slf4j-api', 'slf4j-log4j12'
        }
        
        runtime 'dom4j:dom4j:1.6.1', {
            excludes 'jaxen', 'jaxme-api', 'junitperf', 'pull-parser', 'relaxngDatatype', 'stax-api',
                     'stax-ri', 'xalan', 'xercesImpl', 'xpp3', 'xsdlib', 'xml-apis'
        }
    }

    plugins {
        build ':release:2.2.1', ':rest-client-builder:1.0.3', {
            export = false
        }
    }
}
