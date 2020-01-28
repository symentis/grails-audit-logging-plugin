package test

import grails.plugins.orm.auditable.Stampable

class Coach implements Stampable<Date, Coach> {

    static constraints = {
    }
}
