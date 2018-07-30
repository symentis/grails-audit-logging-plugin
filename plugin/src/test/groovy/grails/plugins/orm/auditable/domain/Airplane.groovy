package grails.plugins.orm.auditable.domain

import grails.gorm.annotation.Entity
import grails.plugins.orm.auditable.Auditable

@Entity
class Airplane implements Auditable{
    String make
    String number

    static constraints = {
        make nullable: true
    }
}
