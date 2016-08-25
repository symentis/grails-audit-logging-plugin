<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'auditTrail.label', default: 'AuditTrail')}" />
        <title><g:message code="default.list.label" args="[entityName]" /></title>
    </head>
    <body>
        <a href="#list-auditTrail" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
            <ul>
                <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
                <li><g:link class="index" action="index"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
            </ul>
        </div>
        <div id="list-auditTrail" class="content scaffold-search" role="main">

            <h1><g:message code="default.list.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
                <div class="message" role="status">${flash.message}</div>
            </g:if>

            <g:form action="search" method="GET">
                <fieldset class="search">
                    <label for="query">${message(code: 'search.label', default: 'Query:')}</label>
                    <input id="query" value="${query}" class="search" name="query" type="text" />
                </fieldset>
                <fieldset class="dateCreated">
                    <label for="byDate">${message(code: 'search.byDate.label', default: 'After Date:')}</label>
                    <g:checkBox id="searchByDate" name="searchByDate" />
                    <g:datePicker name="dateCreated"/>
                </fieldset>
                <fieldSet class="buttons">
                    <input class="search" type="submit" value="${message(code: 'button.search.label', default: 'Search')}" />
                </fieldSet>
            </g:form>

            <f:table collection="${auditTrailList}" properties="['persistedObjectId', 'className','eventName', 'oldValue', 'newValue','propertyName']"/>

            <div class="pagination">
                <g:paginate total="${auditTrailCount ?: 0}" />
            </div>
        </div>
    </body>
</html>