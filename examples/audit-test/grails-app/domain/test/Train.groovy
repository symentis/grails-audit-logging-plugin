package test

import grails.plugins.orm.auditable.StampActor
import grails.plugins.orm.auditable.StampCreated
import grails.plugins.orm.auditable.StampUpdated

class Train implements StampActor,StampCreated,StampUpdated {
	String number
	
    static constraints = {
    }
}
