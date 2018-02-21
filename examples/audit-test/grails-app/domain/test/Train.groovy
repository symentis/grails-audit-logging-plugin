package test

import grails.plugins.orm.auditable.StampActor
import grails.plugins.orm.auditable.StampAutoTimestamp

class Train implements StampActor, StampAutoTimestamp{
	String number
	
    static constraints = {
    }
}
