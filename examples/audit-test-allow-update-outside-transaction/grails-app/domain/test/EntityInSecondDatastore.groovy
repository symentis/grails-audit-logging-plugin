package test

import grails.plugins.orm.auditable.Auditable

class EntityInSecondDatastore implements Auditable{

    String name
    Integer someIntegerProperty

    static constraints = {
    }

    static mapping = {
        datasource("second")
    }
}
