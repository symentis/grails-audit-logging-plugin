#!/bin/bash
set -ex

TRAVIS_BRANCH=`git rev-parse --abbrev-ref HEAD`
TRAVIS_PULL_REQUEST="false"

./gradlew -q clean check install --stacktrace

EXIT_STATUS=0
echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  echo "Publishing archives"

  if [[ -n $TRAVIS_TAG ]]; then
      echo "Publishing to Bintray.."
      ./gradlew audit-logging:bintrayUpload -S || EXIT_STATUS=$?
  else
      echo "Publishing to Grails Artifactory"
#      ./gradlew audit-logging:publish -S || EXIT_STATUS=$?
  fi

  ./gradlew docs --stacktrace || EXIT_STATUS=$?
  
  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global credential.helper "store --file=~/.git-credentials"
  echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

  echo " "
  echo "** Updating gh-pages branch **"
  pushd plugin/build
  git clone https://github.com/symentis/grails-audit-logging-plugin.git -b gh-pages gh-pages --single-branch > /dev/null
  cd gh-pages

  # prepare index.html
  mv ../docs/index.html ../docs/plugin.html
  mv ../docs/ghpages.html ../docs/index.html

  # If this is the master branch then update the snapshot
  if [[ $TRAVIS_BRANCH == 'master' ]]; then
    mkdir -p snapshot
    cp -r ../docs/. ./snapshot/
    git add snapshot/*
  fi

    # If there is a tag present then this becomes the latest
    if [[ -n $TRAVIS_TAG ]]; then
        mkdir -p latest
        cp -r ../docs/. ./latest/
        git add latest/*

        version="$TRAVIS_TAG"
        version=${version:1}
        majorVersion=${version:0:4}
        majorVersion="${majorVersion}x"

        mkdir -p "$version"
        cp -r ../docs/. "./$version/"
        git add "$version/*"

        mkdir -p "$majorVersion"
        cp -r ../docs/. "./$majorVersion/"
        git add "$majorVersion/*"

    fi

    git commit -a -m "Updating docs manually from ${TRAVIS_BRANCH} #noref"
    git push origin HEAD
    cd ..
    rm -rf gh-pages
    popd
fi
exit $EXIT_STATUS
