package org.codehaus.groovy.grails.plugins.orm.auditable

import org.springframework.web.context.request.RequestAttributes
import javax.servlet.http.HttpSession
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
/*
 * Created by IntelliJ IDEA.
 * User: Shawn Hartsock
 * Date: Sep 5, 2009
 * Time: 1:27:30 PM
 *
 * This class provides simple AuditLogListener utilities that
 * can be used as either templates for your own extensions to
 * the plugin or as default utilities.
 *
 * TODO: write a howto for the grails website on how to set up a closure
 * for the AuditLogListener and feed it to the Listener's configurator...
 */
public class AuditLogListenerUtil {

  /**
   * The original getActor method transplanted to the utility class as
   * a closure. The closure takes two arguments one a RequestAttributes object
   * the other is an HttpSession object.
   *
   * These are strongly typed here for the purpose of documentation.
   */
  static Closure actorDefaultGetter = { GrailsWebRequest request, HttpSession session ->
    def actor = null
    if(request.remoteUser) {
      actor = request.remoteUser
    }

    if(!actor && request.userPrincipal) {
      actor = request.userPrincipal.getName()
    }

    if (!actor && delegate.sessionAttribute) {
      log.debug "configured with session attribute ${delegate.sessionAttribute} attempting to resolve"
      actor = session?.getAttribute(delegate.sessionAttribute)
      log.trace "session.getAttribute('${delegate.sessionAttribute}') returned '${actor}'"
    }

    if(!actor && delegate.actorKey) {
      log.debug "configured with actorKey ${actorKey} resolve using request attributes "
      actor = resolve(attr, delegate.actorKey, delegate.log)
      log.trace "resolve on ${delegate.actorKey} returned '${actor}'"
    }
    return actor
  }

  /**
   * The resolve method was my attempt at being clever. It would attempt to
   * resolve the attribute you wanted from the RequestAttributes object passed
   * to it. This did not always work for people since on some designs
   * users are not *always* logged in to the system.
   *
   * It was originally assumed that if you *were* generating events then you
   * were logged in so the problem of null session.user did not come up in testing.
   */
  static resolve(attr, str, log) {
    def tokens = str?.split("\\.")
    def res = attr
    log.trace "resolving recursively ${str} from request attributes..."
    tokens.each({
      log.trace "\t\t ${it}"
      try {
        if (res) res = res."${it}"
      } catch (groovy.lang.MissingPropertyException mpe) {
        log.debug """
AuditLogListener:

You attempted to configure a request attribute named '${str}' and
${AuditLogListenerUtil.class} attempted to dynamically resolve this from
the servlet context session attributes but failed!

Last attribute resolved class ${res?.getClass()} value ${res}
        """
        mpe.printStackTrace()
        res = null
      }

    })
    return res?.toString() ?: null
  }
}
