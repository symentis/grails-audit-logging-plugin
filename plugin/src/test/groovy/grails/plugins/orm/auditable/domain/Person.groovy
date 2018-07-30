package grails.plugins.orm.auditable.domain

import grails.gorm.annotation.Entity
import grails.plugins.orm.auditable.StampActor
import grails.plugins.orm.auditable.StampAutoTimestamp

@Entity
class Person implements StampActor, StampAutoTimestamp {
    String name
}