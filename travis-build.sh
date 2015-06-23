#!/bin/bash
set -e
rm -rf *.zip
./gradlew clean check assemble

filename=$(find build/libs -name "*.jar" | head -1)
filename=$(basename "$filename")

EXIT_STATUS=0
echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  # uncomment and configure appropriate Travis environment variables to automate publishing

  echo "Publishing archives"

  # if [[ -n $TRAVIS_TAG ]]; then
  #     ./gradlew audit-logging:bintrayUpload || EXIT_STATUS=$?
  # else
  #     ./gradlew audit-logging:publish || EXIT_STATUS=$?
  # fi

fi
exit $EXIT_STATUS