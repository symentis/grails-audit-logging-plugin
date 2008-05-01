

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show AuditLogEvent</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">AuditLogEvent List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New AuditLogEvent</g:link></span>
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
                            
                            <td valign="top" class="value">${auditLogEvent.id}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Actor:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.actor}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Class Name:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.className}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Persisted Object Id:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.persistedObjectId}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Persisted Object Version:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.persistedObjectVersion}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Event Name:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.eventName}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Property Name:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.propertyName}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Old Value:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.oldValue}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">New Value:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.newValue}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Date Created:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.dateCreated}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Last Updated:</td>
                            
                            <td valign="top" class="value">${auditLogEvent.lastUpdated}</td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <input type="hidden" name="id" value="${auditLogEvent?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Edit" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
