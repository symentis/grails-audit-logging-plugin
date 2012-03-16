

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show AuditLogEvent</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${resource(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">AuditLogEvent List</g:link></span>
        </div>
        <div class="body">
            <h1>Show AuditLogEvent</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>

                    
                        <tr class="prop">
                            <td valign="top" class="name">Id:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'id')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Actor:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'actor')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Uri:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'uri')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Class Name:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'className')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Persisted Object Id:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'persistedObjectId')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Persisted Object Version:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'persistedObjectVersion')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Event Name:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'eventName')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Property Name:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'propertyName')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Old Value:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'oldValue')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">New Value:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'newValue')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Date Created:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'dateCreated')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Last Updated:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'lastUpdated')}</td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
        </div>
    </body>
</html>
