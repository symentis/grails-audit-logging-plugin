# Grails Audit Logging Plugin 1.x

This is the branch for the 1.x series of this plugin, compatible with Grails < 3.x.

For Grails 3.x compatible version, see [master](https://github.com/robertoschwald/grails-audit-logging-plugin/tree/master) branch.

The Grails Audit Logging plugin can add generic event based Audit Logging to a Grails project and it can also add support to domain models for hooking into the events system.

## Documentation
For documentation, see [Grails 1.x/2.x Plugin Page](https://grails.org/plugin/audit-logging?skipRedirect=true "Grails 1.x/2.x Plugin Page")

## audit-quickstart (version >=1.1.0)
1.1.0 added the audit-quickstart command to generate a domain artifact in the developers Grails project. 
Therefore, you need to perform "grails audit-quickstart \<package.DomainClass\>" after installing this plugin's version 1.1.0 and later. See issue [#13](https://github.com/robertoschwald/grails-audit-logging-plugin/issues/13)

With this, you get a AuditLog domain class in your project which is fully under your control. The domain name is registered in your Config.groovy with key "auditLog.auditDomainClassName". 

This feature is the default in the Grails 3.x plugin version as well.

Example:

```
grails audit-quickstart org.example.myproject.AuditLogEvent

```
 
#### Migration from plugin versions < 1.1.0
If you already use a plugin version < 1.1.0, you must change the created AuditLogEvent domain class to fit your previous settings. The generated domain class artifact contains hint-comments what to change.


## Issue Management

See [GitHub Issues](https://github.com/robertoschwald/grails-audit-logging-plugin/issues "Issues")

(old) See [JIRA](http://jira.grails.org/browse/GPAUDITLOGGING "GPAUDITLOGGING JIRA") (All tickets migrated to GH)

## Pull Requests
Pull requests are highly appreciated and welcome!
Please add integration tests for new features to the audit-test application.
Execute "grails perform-audit-log-test-apps" in the audit-test directory. This tests the plugin against several Grails versions to ensure compatibility.
Before placing a pull request, please create an issue in Jira and state the issue# in the pull request. Thanks.


## Continuous Integration Server
[![Build Status](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin.svg?branch=1.x_maintenance)](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin?branch=1.x_maintenance) (1.x_maintenance branch)

