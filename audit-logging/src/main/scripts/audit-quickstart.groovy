/* Copyright 2006-2015 the original author or authors.
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
import grails.codegen.model.Model
import groovy.transform.Field

@Field Map templateAttributes
Model auditModel
@Field String usageMessage = '''
   grails audit-quickstart <domain-class-package> <domain-class-name>

   Example: grails audit-quickstart com.yourapp MyAuditLogEvent

'''

description 'Creates domain classes and updates config settings for the Audit-Logging plugin', {

  usage usageMessage

  argument name:'Domain class package', description:'The package to use for the domain class', required:false
}

if (args.size() < 2) {
  error 'Usage:' + usageMessage
  return false
}

def argValues = parseArgs()
if (!argValues) {
  return false
}

String packageName = args[0]
auditModel = model(packageName + '.' + args[1])

String message = "Creating Audit domain class '" + auditModel.simpleName + "'"
message += " in package '" + packageName + "'"
addStatus message

templateAttributes = [packageName       :auditModel.packageName,
                      auditClassName    :auditClassName,
                      auditClassProperty:auditModel.modelName]


createDomain(auditModel)

updateConfig(auditModel?.simpleName)

printMessage '''
*******************************************************
* Created auditLogEvent domain class.                 *
* Your grails-app/conf/Config.groovy has been updated *
* with the class name of the configured domain class. *
* Please verify that the values are correct.          *
*******************************************************
'''


private parseArgs() {
  def args = argsMap.params
  if (2 == args.size()) {
    printMessage "Creating AuditLogging class ${args[1]} in package ${args[0]}"
    return args
  }
  errorMessage USAGE
  null
}

private void createDomain(Model auditModel) {
  generateFile 'AuditLogEvent', auditModel.packagePath, auditModel.simpleName
//  String dir = packageToDir(packageName)
//  String domainDir = "$appDir/domain/$dir"
//  generateFile "$templateDir/AuditLogEvent.groovy.template", "$domainDir${auditClassName}.groovy"
}

private void updateConfig(String auditClassName) {
  file("grails-app/conf/application.groovy").withWriterAppend { BufferedWriter writer ->
    writer.newLine()
    writer.newLine()
    writer.writeLine '// Added by the Audit-Logging plugin:'
    writer.writeLine "auditLog.auditDomainClassName = '${packageName}.$auditClassName'"
    writer.newLine()
  }
}

private void generateFile(String templateName, String packagePath, String className) {
  render template(templateName + '.groovy.template'),
    file("grails-app/domain/$packagePath/${className}.groovy"),
    templateAttributes, false
}



