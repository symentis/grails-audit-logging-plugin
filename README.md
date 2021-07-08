# Grails Audit Logging Plugin

## Description

The Audit Logging plugin for Grails adds generic event based Audit Logging to a Grails project.

The master branch holds the codebase for plugin version 4.0.x (Grails 4.0.x).

We currently work on a Grails 4.x branch using a new approach. See [PR #212](https://github.com/symentis/grails-audit-logging-plugin/pull/212)

For older Grails versions, see "Supported Grails Versions" below.
## Moving to Maven Central
This repositories new artifacts are currently moved to Maven Central, sind Bintray will shut down MAY/01/21. You can obtain the old artifacts from https://repo.grails.org.

## Documentation
 * For current release documentation, see [User Guide](https://symentis.github.io/grails-audit-logging-plugin/latest/plugin.html)
 * For snapshot documentation, see [Snapshot User Guide](https://symentis.github.io/grails-audit-logging-plugin/snapshot/plugin.html)
 * For 4.x documentation, see [4.x User Guide](https://symentis.github.io/grails-audit-logging-plugin/4.0.x/plugin.html)
 * For 3.x documentation, see [3.x User Guide](https://symentis.github.io/grails-audit-logging-plugin/3.0.x/plugin.html)
 * For 2.x documentation, see [2.x User Guide](https://symentis.github.io/grails-audit-logging-plugin/2.0.x/plugin.html)

## Supported Grails versions
 * Grails 4.0.x: [master branch (5.0.x version)](https://github.com/symentis/grails-audit-logging-plugin/tree/master) Alternatively up to Grails 4.0.9: [4.0.x version](https://github.com/symentis/grails-audit-logging-plugin/tree/4.x_maintenance)
 * Grails 3.3.x: [3.x_maintenance branch](https://github.com/symentis/grails-audit-logging-plugin/tree/3.x_maintenance)
 * Grails 3.0.x-3.2.x: [2.x_maintenance branch](https://github.com/symentis/grails-audit-logging-plugin/tree/2.x_maintenance)
 * Grails 2.x: [1.x_maintenance branch](https://github.com/symentis/grails-audit-logging-plugin/tree/1.x_maintenance)

## audit-quickstart
You need to perform "grails audit-quickstart \<package\> \<DomainClass\>" after installing this plugin's 2.0.x version or later.

With this, you get an auditlog domain class in your project which is fully under your control.
The domain name is registered in your application.groovy with key "grails.plugins.auditLog.auditDomainClassName".

Example:

```
grails audit-quickstart org.example.myproject MyAuditLogEvent
```

## Issue Management

See [GitHub Issues](https://github.com/symentis/grails-audit-logging-plugin/issues "Issues")

## Pull Requests
Pull requests are highly appreciated and welcome!

Please add integration tests for new features to the audit-test application.

## Contributors
Special thanks to all the <a href="https://github.com/symentis/grails-audit-logging-plugin/graphs/contributors">contributors</a> (in alphabetical order):

	Aaron Long
    Alan Wikie
	Aldrin
	Andrey Zhuchkov
	Ankur Tripathi
	Burt Beckwith
	bzamora33
	Danny Casady
	Dennie de Lange
	Dhiraj Mahapatro
	Elmar Kretzer
    Felix Scheinost
	Fernando Cambarieri
	Graeme Rocher
	Jeff Palmer
	Jorge Aguilera
	Juergen Baumann
	Madhava Jay
    Matt Long
	Matthew A Stewart
	Paul Taylor
	Sami Mäkelä
	Sebastien Arbogast
	Semyon Atamas
	Shawn Hartsock
	Tom Crossland

	Project lead: Robert Oschwald


## Continuous Integration Server
![Build Status](https://github.com/symentis/grails-audit-logging-plugin/actions/workflows/ci.yaml/badge.svg)

## Maven Central Repository
This plugin will be available from Maven Central, soon. Older versions (up to 4.0.3) are available from the [Grails Artifact Repository](https://repo.grails.org/ui/).
***

<a href="https://www.yourkit.com/java/profiler/index.jsp"><img src="https://www.yourkit.com/images/yklogo.png" alt="YourKit Java Profiler"/></a>

YourKit is kindly supporting Grails open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).





