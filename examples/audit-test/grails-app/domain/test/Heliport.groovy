package test

import grails.plugins.orm.auditable.Auditable

class Heliport implements Auditable {
    String code
    String name

    static mapping = {
        version false
    }
}
