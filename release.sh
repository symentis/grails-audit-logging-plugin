#!/bin/sh
rm -rf grails-audit-logging-plugin/target/release
mkdir -p grails-audit-logging-plugin/target/release
cd grails-audit-logging-plugin/target/release
git clone https://github.com/robertoschwald/grails-audit-logging-plugin.git
if [ "$1" != "" ]; then
    echo "Switching to branch $1"
    git checkout $1
    if [ $? != 0 ]; then
        echo "Switching to branch $1 failed. Aborting"
        exit 1
    fi
else 
    echo "Building 1.x_maintenance release"
    git checkout 1.x_maintenance
fi
cd grails-audit-logging-plugin/grails-audit-logging-plugin
grails clean
grails compile

echo "Testing with several different Grails versions"
cd ../audit-test
grails perform-audit-log-test-apps
if [ $? != 0 ]; then
	echo "Tests failed. See perform-audit-log-test-apps_$$.log"
	exit 1
fi 

cd ../grails-audit-logging-plugin

version=`cat AuditLoggingGrailsPlugin.groovy | grep "def version" | cut -d "=" -f 2 | cut -d "\"" -f 2`

echo $version | grep -q SNAPSHOT
if [ $? = 0 ]; then
	echo "Note: Publishing as SNAPSHOT version"
	SNAPSHOT="--snapshot"
else
	echo "NOTE: Publishing as release version"
fi

echo "Are you sure you want to publish plugin version $version ?"
read yn
if [ $yn != "y" ]; then
	echo "Aborted by user"
	exit 2
fi
grails publish-plugin $SNAPSHOT --stacktrace
