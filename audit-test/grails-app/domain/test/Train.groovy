package test

import grails.plugins.orm.auditable.Stamp

@Stamp
class Train {
	String number
	
    static constraints = {
        
    }
}
