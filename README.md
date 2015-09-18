#*Grails Audit Logging Plugin 1.x*#

This is the branch for the 1.x series of this plugin, compatible with Grails < 3.x.

The Grails Audit Logging plugin can add generic event based Audit Logging to a Grails project and it can also add support to domain models for hooking into the events system.

##**Documentation**##
For documentation, see [Grails Plugin Page](http://grails.org/plugin/audit-logging "Grails Plugin Page")

##**audit-quickstart (version >=1.1.0**##
Use version 1.1.0-SNAPSHOT to test this new feature in your project.  Any feedback is highly appreciated.

Currently, we are impementing audit-quickstart to generate a domain artifact in the developers Grails project. 
Therefore, you need to perform "grails audit-quickstart <package.DomainClass> after installing this plugin's version 1.1.0 and later. 

With this, you get a AuditLog domain class in your project which is fully under your control. See issue #13.

This feature will be the default in the upcoming Grails 3.x plugin version as well.

Example:

```
grails audit-quickstart org.example.myproject.AuditLogEvent

```
 
####**Migration from plugin versions < 1.1.0**####
If you already use a plugin version < 1.1.0, you must change the created AuditLogEvent domain class to fit your previous settings. The generated domain class contains hint-comments what to change.


##**Issue Management**##

See [GitHub Issues](https://github.com/robertoschwald/grails-audit-logging-plugin/issues "Issues")

(old) See [JIRA](http://jira.grails.org/browse/GPAUDITLOGGING "GPAUDITLOGGING JIRA") (All tickets migrated to GH)

##**Pull Requests**##
Pull requests are highly appreciated and welcome!
Please add integration tests for new features to the audit-test application.
Execute "grails perform-audit-log-test-apps" in the audit-test directory. This tests the plugin against several Grails versions to ensure compatibility.
Before placing a pull request, please create an issue in Jira and state the issue# in the pull request. Thanks.


##**Continuous Integration Server**##
[![Build Status](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin.svg?branch=1.x_maintenance)](https://travis-ci.org/robertoschwald/grails-audit-logging-plugin?branch=1.x_maintenance)

