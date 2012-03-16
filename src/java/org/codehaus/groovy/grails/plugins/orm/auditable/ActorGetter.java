package org.codehaus.groovy.grails.plugins.orm.auditable;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import javax.servlet.http.HttpSession;

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 2/22/12
 * Time: 12:43 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ActorGetter {
    String get(GrailsWebRequest request, HttpSession session);
}
