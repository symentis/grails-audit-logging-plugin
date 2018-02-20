package test

import grails.plugins.orm.auditable.StampActor

class Train implements StampActor {
	String number
	
    static constraints = {
    }
}
