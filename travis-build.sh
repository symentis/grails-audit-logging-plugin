#!/bin/bash

# build script for travis-ci.org build
# (c) 2014 Robert Oschwald
#
# Note:
# On Travis-CI, we only test a subset of the supported Grails versions of this plugin.
# We test many more Grails versions in the audit-test test application when using grails perform-audit-log-test-apps, locally.
# This is currently not possible in Travis-CI, as we need to install all those Grails versions up-front in a common dir.
#
# To test with all supported Grails versions, we use the release.sh script before we publish any new SNAPSHOT
# or release version of this plugin.

set -e

current_dir=${PWD##*/}

if [ ${current_dir} != "audit_test" ]; then
  cd audit-test
fi

grails clean

if [ ${GRAILS_VERSION} == 2.[0-3].* ]; then
    grails upgrade --non-interactive
else
    echo "Setting Grails version ${GRAILS_VERSION}"
    grails set-grails-version ${GRAILS_VERSION}
fi

echo "Start test..."
grails test-app --non-interactive --stacktrace
