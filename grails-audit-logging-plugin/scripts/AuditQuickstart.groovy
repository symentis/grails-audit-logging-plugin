/* Copyright 2006-2015 SpringSource.
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
import grails.util.GrailsNameUtils


includeTargets << new File(auditLoggingPluginDir, 'scripts/_AuditCommon.groovy')

USAGE = '''
Usage: grails audit-quickstart <domain-class-package> <domain-class-name>

Creates a AuditLogEvent class in the specified package.

Example: grails audit-quickstart com.yourapp MyAuditLogEvent
'''

includeTargets << grailsScript('_GrailsBootstrap')

packageName = ''
auditClassName = ''

target(auditQuickstart: 'Creates artifacts for the Audit Logging plugin') {
	depends(checkVersion, configureProxy, packageApp, classpath)

	if (!configure()) {
		return 1
	}

	createDomain()

	updateConfig()

	printMessage '''
*******************************************************
* Created auditLogEvent domain class.                 *
* Your grails-app/conf/Config.groovy has been updated *
* with the class name of the configured domain class. *
* Please verify that the values are correct.          *
*******************************************************
'''
}

private boolean configure() {

	def argValues = parseArgs()
	if (!argValues) {
		return false
	}

	if (argValues.size() == 2) {
		(packageName, auditClassName) = argValues
	}

	templateAttributes = [packageName: packageName,
	                      auditClassName: auditClassName,
	                      auditClassProperty: GrailsNameUtils.getPropertyName(auditClassName)]

	true
}

private void createDomain() {

	String dir = packageToDir(packageName)
	String domainDir = "$appDir/domain/$dir"
	generateFile "$templateDir/AuditLogEvent.groovy.template", "$domainDir${auditClassName}.groovy"
}

private void updateConfig() {

	def configFile = new File(appDir, 'conf/Config.groovy')
	if (!configFile.exists()) {
		return
	}

	configFile.withWriterAppend { BufferedWriter writer ->
		writer.newLine()
		writer.newLine()
		writer.writeLine '// Added by the Audit-Logging plugin:'
		writer.writeLine "auditLog.auditDomainClassName = '${packageName}.$auditClassName'"
		writer.newLine()
	}
}

private parseArgs() {

	def args = argsMap.params

	if (2 == args.size()) {
		printMessage "Creating AuditLogging class ${args[1]} in package ${args[0]}"
		return args
	}

	errorMessage USAGE
	null
}

setDefaultTarget 'auditQuickstart'
