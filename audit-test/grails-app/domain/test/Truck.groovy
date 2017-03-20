package test

import grails.plugins.orm.auditable.Stamp
import grails.plugins.orm.auditable.StampInfo

@Stamp(
    createdBy = @StampInfo(fieldname="originalWho",type=Date.class,exclude = false),
    dateCreated = @StampInfo(fieldname="originalWhen",type=Date.class,exclude = false),
    lastUpdatedBy = @StampInfo(fieldname="lastWho",nullable = true,exclude = false),
    lastUpdated = @StampInfo(fieldname="lastWhen",nullable = true,exclude = false))
class Truck {
    String number

    static constraints = {
    }
}
