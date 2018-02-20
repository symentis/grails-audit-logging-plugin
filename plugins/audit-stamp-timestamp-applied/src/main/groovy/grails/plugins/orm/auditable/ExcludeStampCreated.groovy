package grails.plugins.orm.auditable

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.SOURCE)
@interface ExcludeStampCreated {

}