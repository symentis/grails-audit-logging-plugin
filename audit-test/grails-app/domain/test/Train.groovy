package test

import org.codehaus.groovy.grails.plugins.orm.auditable.Stamp

@Stamp
class Train {
	String number
	
    static constraints = {
    }
}
