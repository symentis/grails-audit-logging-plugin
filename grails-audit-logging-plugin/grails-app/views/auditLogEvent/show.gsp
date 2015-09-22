

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show AuditLogEvent</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}">Home</a></span>
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
                            <td valign="top" class="name">${message(code: 'auditLogEvent.dateCreated.label', default: 'Date Created:')}</td>

                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'dateCreated')}</td>

                        </tr>


                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.id.label', default: 'Id:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'id')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.actor.label', default: 'Actor:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'actor')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.uri.label', default: 'Uri:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'uri')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.className.label', default: 'Class Name:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'className')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.persistedObjectId.label', default: 'Persisted Object Id:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'persistedObjectId')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.persistedObjectVersion.label', default: 'Persisted Object Version:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'persistedObjectVersion')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.eventName.label', default: 'Event Name:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'eventName')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.propertyName.label', default: 'Property Name:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'propertyName')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.oldValue.label', default: 'Old Value:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'oldValue')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">${message(code: 'auditLogEvent.newValue.label', default: 'New Value:')}</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:auditLogEventInstance, field:'newValue')}</td>
                            
                        </tr>

                    </tbody>
                </table>
            </div>
        </div>
    </body>
</html>
