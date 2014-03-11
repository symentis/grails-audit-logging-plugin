/* Copyright 2011-2013 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Creates test applications for tests.
 */
includeTargets << grailsScript('_GrailsBootstrap')

grailsHome = null
dotGrails = null
grailsVersion = null
projectDir = null
appName = null
testprojectRoot = null
deleteAll = false

target(performAuditLogTestApps: 'Creates Audit Log Plugin test apps and runs the tests on them') {

  def configFile = new File(basedir, 'testapps.config.groovy')
  if (!configFile.exists()) {
    error "$configFile.path not found"
  }

  new ConfigSlurper().parse(configFile.text).each { name, config ->
    printMessage "\nCreating app based on configuration $name: ${config.flatten()}\n"
    init name, config
    createApp()
    configureApp()
    installPlugins()
    createProjectFiles()
    performTests()
  }
  println "\n *** ALL TESTS PASSED ***"
}

private void init(String name, config) {

  /*
  pluginVersion = config.pluginVersion
  if (!pluginVersion) {
    error "pluginVersion wasn't specified for config '$name'"
  }

  def pluginZip = new File(basedir, "../grails-audit-logging-plugin/grails-audit-logging-${pluginVersion}.zip")
  if (!pluginZip.exists()) {
    error "plugin $pluginZip.absolutePath not found"
  }
  */

  grailsHome = config.grailsHome
  if (!new File(grailsHome).exists()) {
    error "Grails home $grailsHome not found"
  }

  projectDir = config.projectDir
  appName = 'grails-audit-logging-test-' + name
  testprojectRoot = "$projectDir/$appName"
  grailsVersion = config.grailsVersion
  dotGrails = config.dotGrails + '/' + grailsVersion
}

private void createApp() {

  ant.mkdir dir: projectDir

  deleteDir testprojectRoot
  deleteDir "$dotGrails/projects/$appName"

  println "Creating application for $grailsHome in $projectDir"
  callGrails(grailsHome, projectDir, 'dev', 'create-app', [appName])
}

private void installPlugins() {

  File buildConfig = new File(testprojectRoot, 'grails-app/conf/BuildConfig.groovy')
  String contents = buildConfig.text

  contents = contents.replace('grails.project.class.dir = "target/classes"', "grails.project.work.dir = 'target'")
  contents = contents.replace('grails.project.test.class.dir = "target/test-classes"', '')
  contents = contents.replace('grails.project.test.reports.dir = "target/test-reports"', '')
  contents = contents.replace('//mavenLocal()', 'mavenLocal()')
  contents = contents.replace('grails.project.dependency.resolution =', '''grails.plugin.location.'audit-logging'="../../../grails-audit-logging-plugin"\ngrails.project.dependency.resolution =''')
  contents = contents.replace('grails.project.fork', 'grails.project.forkDISABLED')

  float grailsMinorVersion = grailsVersion[0..2] as float

  if (grailsMinorVersion < 2.3f) {
    // need to add Spock as plugin
    String spockDependency = grailsMinorVersion > 2.1f ? '		test "org.spockframework:spock-grails-support:0.7-groovy-2.0"' : ''
    String spockExclude = grailsMinorVersion > 2.1f ? '			exclude "spock-grails-support"' : ''

    println "Adding spock dependency.."
    contents = contents.replace('dependencies {', """
        dependencies {
          $spockDependency
        """)

    println "Adding spock plugin.."
    contents = contents.replace('plugins {', """plugins {
        test ":spock:0.7", {
          $spockExclude
        }
        """)
  }

  buildConfig.withWriter { it.writeLine contents }

  println "Calling $grailsHome dev compile"
  callGrails grailsHome, testprojectRoot, 'dev', 'compile', null, true // can fail when installing the functional-test plugin
  callGrails grailsHome, testprojectRoot, 'dev', 'compile'
}

private void configureApp() {
  File config = new File(testprojectRoot, 'grails-app/conf/Config.groovy')
  config << '''
  auditLog {
    verbose = true
    defaultIgnore = ['version', 'lastUpdated', 'lastUpdatedBy']
    transactional = false
    defaultMask = ['ssn']
    logIds = true
    defaultActor = 'SYS'
  }
  '''
}

private void createProjectFiles() {
  String source = "$basedir"
  float grailsMinorVersion = grailsVersion[0..2] as float

  ant.copy(todir:"$testprojectRoot/test/integration/test") {
    fileset(dir:"$source/test/integration/test")
  }

  ant.copy(todir:"$testprojectRoot/grails-app/domain/test") {
    fileset(dir:"$source/grails-app/domain/test")
  }

  if (grailsMinorVersion < 2.3f) {
    // rework test groovy files. (Spock package name differs)
    println "Grails version: ${grailsMinorVersion}, using SPOCK package name 'grails.plugin.spock'"
    new File("$testprojectRoot/test/integration/test").listFiles().each { file ->
      println "Reworking spock package name in $file"
      String contents = file.text
      contents = contents.replace('grails.test.spock','grails.plugin.spock')
      file.withWriter { it.writeLine contents}
    }
  }
}

private void performTests() {
  println "Performing test-app"
  callGrails grailsHome, testprojectRoot, 'dev', 'test-app'
}


private void deleteDir(String path) {
  /*
  if (new File(path).exists() && !deleteAll) {
    String code = "confirm.delete.$path"
    ant.input message: "$path exists, ok to delete?", addproperty: code, validargs: 'y,n,a'
    def result = ant.antProject.properties[code]
    if ('a'.equalsIgnoreCase(result)) {
      deleteAll = true
    } else if (!'y'.equalsIgnoreCase(result)) {
      printMessage "\nNot deleting $path"
      exit 1
    }
  }
  */
  deleteAll = true
  println "Deleting $path"
  ant.delete dir: path
}

private void error(String message) {
  errorMessage "\nERROR: $message"
  exit 1
}

private void callGrails(String grailsHome, String dir, String env, String action, List extraArgs = null, boolean ignoreFailure = false) {

  String resultproperty = 'exitCode' + System.currentTimeMillis()
  String outputproperty = 'execOutput' + System.currentTimeMillis()

  println "Running 'grails $env $action ${extraArgs?.join(' ') ?: ''}'"

  ant.exec(executable: "${grailsHome}/bin/grails", dir: dir, failonerror: false,
      resultproperty: resultproperty, outputproperty: outputproperty) {
    ant.env key: 'GRAILS_HOME', value: grailsHome
    ant.arg value: env
    ant.arg value: action
    extraArgs.each { ant.arg value: it }
    ant.arg value: '--stacktrace'
    //ant.arg value: '-verbose'
  }

  println ant.project.getProperty(outputproperty)

  int exitCode = ant.project.getProperty(resultproperty) as Integer
  if (exitCode && !ignoreFailure) {
    exit exitCode
  }
}

printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError', [message]) }


setDefaultTarget 'performAuditLogTestApps'
