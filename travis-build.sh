#!/bin/bash

set -e

current_dir=${PWD##*/}

if [ ${current_dir} != "audit_test" ]; then
  cd audit-test
fi

./grailsw perform-audit-log-test-apps
