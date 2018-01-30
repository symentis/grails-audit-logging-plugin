package test

import grails.plugins.orm.auditable.Stampable

class Train implements Stampable {
	String number
	
    static constraints = {
    }

    static mapping = {
        autoTimestamp false
    }
}
