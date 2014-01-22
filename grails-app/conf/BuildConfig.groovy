grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    inherits("global") {
    }

    log "warn"

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        runtime 'dom4j:dom4j:1.6.1', {
            excludes 'jaxen', 'jaxme-api', 'junitperf', 'pull-parser', 'relaxngDatatype', 'stax-api',
                     'stax-ri', 'xalan', 'xercesImpl', 'xpp3', 'xsdlib', 'xml-apis'
        }
    }

    plugins {
        build(":release:3.0.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
