#*Grails Audit Logging Plugin*#

The Grails Audit Logging plugin for Grails 3.x adds generic event based Audit Logging to a Grails project and also adds support to domain models for hooking into the GORM events system.

##**Documentation**##
 * For Grails 3.x documentation, see [Plugin Documentation](https://robertoschwald.github.io/grails-audit-logging-plugin/latest/)
 * For Grails 2.x documentation, see [Grails Plugin Page](http://grails.org/plugin/audit-logging "Grails Plugin Page")

##**Supported Grails versions**##
 * Grails 3.x (master branch)
 * Grails 2.x (1.x_maintenance branch)

##**Issue Management**##

See [GitHub Issues](https://github.com/robertoschwald/grails-audit-logging-plugin/issues "Issues")

JIRA issues have been migrated to GitHub issues recently (Thanks to Graeme).
Please do not add issues / comments to Jira anymore. Old Jira issues, see [JIRA](http://jira.grails.org/browse/GPAUDITLOGGING "GPAUDITLOGGING JIRA")

##**Pull Requests**##
Pull requests are highly appreciated and welcome!

Please add integration tests for new features to the audit-test application.

##**Contributors**##
Special thanks to all the contributors to the project (in alphabetical order):

	Aaron Long
	Aldrin
	Ankur Tripathi
	Burt Beckwith 
	Dennie de Lange
	Dhiraj Mahapatro
	Fernando Cambarieri
	Graeme Rocher
	Jorge Aguilera
	Matthew A Stewart
	Shawn Hartsock
	Tom Crossland
	
	Project lead: Robert Oschwald

##**audit-quickstart**##
Version 2.0.0 is the first version with audit-quickstart command support.
Therefore, you need to perform "grails audit-quickstart \<package\> \<DomainClass\>" after installing this plugin's version and later. See issue [#13](https://github.com/robertoschwald/grails-audit-logging-plugin/issues/13)
  
With this, you get a AuditLog domain class in your project which is fully under your control. The domain name is registered in your application.groovy with key "auditLog.auditDomainClassName".
  
Example:
  
```
grails audit-quickstart org.example.myproject MyAuditLogEvent
  
```

##**Continuous Integration Server**##
[![Build Status](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin.svg)](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin)





