

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>AuditLogEvent List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}">Home</a></span>
        </div>
        <div class="body">
            <h1>AuditLogEvent List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                            <g:sortableColumn property="dateCreated" title="${message(code: 'auditLogEvent.dateCreated.label', default: 'Created')}" />
                        
                   	        <g:sortableColumn property="actor" title="${message(code: 'auditLogEvent.actor.label', default: 'Actor')}" />

                   	        <g:sortableColumn property="className" title="${message(code: 'auditLogEvent.className.label', default: 'Class')}" />

                            <g:sortableColumn property="uri" title="${message(code: 'auditLogEvent.eventName.label', default: 'eventName')}" />

                            <g:sortableColumn property="persistedObjectId" title="${message(code: 'auditLogEvent.persistedObjextId.label', default: 'Persisted Object Id')}" />
                        
                   	        <g:sortableColumn property="persistedObjectVersion" title="${message(code: 'auditLogEvent.persistedObjextVersion.label', default: 'Persisted Object Version')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${auditLogEventInstanceList}" status="i" var="auditLogEventInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                            <td><g:link action="show" id="${auditLogEventInstance.id}">${fieldValue(bean: auditLogEventInstance, field: "dateCreated")}</g:link></td>

                            <td>${fieldValue(bean:auditLogEventInstance, field:'actor')}</td>

                            <td>${fieldValue(bean:auditLogEventInstance, field:'className')}</td>

                            <td>${fieldValue(bean:auditLogEventInstance, field:'eventName')}</td>

                            <td>${fieldValue(bean:auditLogEventInstance, field:'persistedObjectId')}</td>
                        
                            <td>${fieldValue(bean:auditLogEventInstance, field:'persistedObjectVersion')}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${auditLogEventInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
