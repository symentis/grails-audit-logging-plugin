# Grails Audit Logging Plugin

## Description

The Audit Logging plugin for Grails adds generic event based Audit Logging to a Grails project.

The master branch holds the codebase for plugin version 4.0.x (Grails 4.0.x).

We currently work on a Grails 4.x branch using a new approach. See [feature/4.0.0_wip](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/feature/4.0.0_wip) 

For older Grails versions, see "Supported Grails Versions" below.

## Documentation
 * For current release documentation, see [User Guide](https://robertoschwald.github.io/grails-audit-logging-plugin/latest/plugin.html)
 * For snapshot documentation, see [Snapshot User Guide](https://robertoschwald.github.io/grails-audit-logging-plugin/snapshot/plugin.html)
 * For 3.x documentation, see [3.x User Guide](https://robertoschwald.github.io/grails-audit-logging-plugin/3.0.x/plugin.html)
 * For 2.x documentation, see [2.x User Guide](https://robertoschwald.github.io/grails-audit-logging-plugin/2.0.x/plugin.html)
 * For 1.x documentation, see [1.x Grails Plugin Page](http://grails.org/plugin/audit-logging "Grails Plugin Page")

## Supported Grails versions
 * Grails 4.0.x: [master branch](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/master) [ ![Download](https://api.bintray.com/packages/robertoschwald/plugins/audit-logging/images/download.svg) ](https://bintray.com/robertoschwald/plugins/audit-logging/_latestVersion)
 * Grails 3.3.x: [3.x_maintenance branch](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/3.x_maintenance) [ ![Download](https://api.bintray.com/packages/robertoschwald/plugins/audit-logging/images/download.svg?version=3.0.6) ](https://bintray.com/robertoschwald/plugins/audit-logging/3.0.6/link)
 * Grails 3.0.x-3.2.x: [2.x_maintenance branch](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/2.x_maintenance) [ ![Download](https://api.bintray.com/packages/robertoschwald/plugins/audit-logging/images/download.svg?version=2.0.6) ](https://bintray.com/robertoschwald/plugins/audit-logging/2.0.6/link)
 * Grails 2.x: [1.x_maintenance branch](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/1.x_maintenance)

## audit-quickstart
You need to perform "grails audit-quickstart \<package\> \<DomainClass\>" after installing this plugin's 2.0.x version or later.

With this, you get an auditlog domain class in your project which is fully under your control.
The domain name is registered in your application.groovy with key "grails.plugins.auditLog.auditDomainClassName".

Example:

```
grails audit-quickstart org.example.myproject MyAuditLogEvent
```

## Issue Management

See [GitHub Issues](https://github.com/robertoschwald/grails-audit-logging-plugin/issues "Issues")

## Pull Requests
Pull requests are highly appreciated and welcome!

Please add integration tests for new features to the audit-test application.

## Contributors
Special thanks to all the <a href="https://github.com/robertoschwald/grails-audit-logging-plugin/graphs/contributors">contributors</a> (in alphabetical order):

	Aaron Long
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
[![Build Status](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin.svg?branch=master)](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin)

## Bintray Repository
<a href='https://bintray.com/robertoschwald/plugins/audit-logging/view?source=watch' alt='Get automatic notifications about new "audit-logging" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

[ ![Download](https://api.bintray.com/packages/robertoschwald/plugins/audit-logging/images/download.svg) ](https://bintray.com/robertoschwald/plugins/audit-logging/_latestVersion)

***

<a href="https://www.yourkit.com/java/profiler/index.jsp"><img src="https://www.yourkit.com/images/yklogo.png" alt="YourKit Java Profiler"/></a>

YourKit is kindly supporting Grails open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).





