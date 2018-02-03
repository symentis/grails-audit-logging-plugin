package test

import grails.plugins.orm.auditable.Auditable

class Aircraft implements Auditable {
    String type
    String description

    static mapping = {
        id name: 'type', generator: 'assigned'
    }

    static constraints = {
        type bindable: true
    }
}
