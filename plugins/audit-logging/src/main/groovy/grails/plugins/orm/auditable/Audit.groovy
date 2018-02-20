package grails.plugins.orm.auditable

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Implementing this annotation will make the domainclass implement the Auditable trait
 * and allows configuration of the included and excluded auditable properties
 */
@Retention(RetentionPolicy.RUNTIME)
@interface Audit {
    String[] includes() default []
    String[] excludes() default []
}