package test

import grails.plugins.orm.auditable.Auditable

class Airport implements Auditable {
    String code
    String name

    static hasMany = [runways: Runway]
}
