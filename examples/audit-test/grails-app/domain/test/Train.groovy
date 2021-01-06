package test

import grails.plugins.orm.auditable.Stampable

class Train implements Stampable<Date, Train> {
    String number

    static constraints = {
    }
}
