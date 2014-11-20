grails.servlet.version = "3.0"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    // compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    // test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    // run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    // war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    // console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.plugin.location.'audit-logging' = '../grails-audit-logging-plugin'

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
    }

    dependencies {
    }

    plugins {
        // plugins for the build system only
        build ":tomcat:7.0.54"

        // plugins for the compile step
        compile ":scaffolding:2.0.3"
        compile ':cache:1.1.1'

        // plugins needed at runtime but not for compilation
        // Version to use see release notes: https://grails.org/2.3.11+Release+Notes
        runtime ":hibernate:3.6.10.16"
        // runtime ":hibernate4:4.3.5.4"

        runtime ":database-migration:1.4.0"
        runtime ":jquery:1.10.2.2"
        runtime ":resources:1.2.1"
    }
}
