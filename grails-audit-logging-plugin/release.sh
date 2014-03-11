#!/bin/sh
rm -rf target/release
mkdir target/release
cd target/release
git clone git@github.com:robertoschwald/grails-audit-logging-plugin
cd grails-audit-logging-plugin/grails-audit-logging-plugin
grails clean
grails compile

#grails publish-plugin --snapshot --stacktrace
grails publish-plugin --stacktrace