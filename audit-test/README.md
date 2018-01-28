To test the plugin with different Grails versions, perform

grails perform-audit-log-test-apps

in the audit-test dir. Ensure you use Java 1.7.

You might need to tweak the settings in the testapps.config.groovy file for your environment, especially the
grailsHomeRoot property which points to a common directory which holds all needed Grails versions.
