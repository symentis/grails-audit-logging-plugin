

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>AuditLogEvent List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New AuditLogEvent</g:link></span>
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
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <g:sortableColumn property="actor" title="Actor" />
                        
                   	        <g:sortableColumn property="className" title="Class Name" />
                        
                   	        <g:sortableColumn property="persistedObjectId" title="Persisted Object Id" />
                        
                   	        <g:sortableColumn property="persistedObjectVersion" title="Persisted Object Version" />
                        
                   	        <g:sortableColumn property="eventName" title="Event Name" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${auditLogEventList}" status="i" var="auditLogEvent">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${auditLogEvent.id}">${auditLogEvent.id?.encodeAsHTML()}</g:link></td>
                        
                            <td>${auditLogEvent.actor?.encodeAsHTML()}</td>
                        
                            <td>${auditLogEvent.className?.encodeAsHTML()}</td>
                        
                            <td>${auditLogEvent.persistedObjectId?.encodeAsHTML()}</td>
                        
                            <td>${auditLogEvent.persistedObjectVersion?.encodeAsHTML()}</td>
                        
                            <td>${auditLogEvent.eventName?.encodeAsHTML()}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${AuditLogEvent.count()}" />
            </div>
        </div>
    </body>
</html>
