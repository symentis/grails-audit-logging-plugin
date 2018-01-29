package test

import grails.plugins.orm.auditable.Auditable

class Book implements Auditable {
    String title
    String description
    Date published
    Long pages

    static hasMany = [reviews: Review]
    static belongsTo = [author: Author]

    @Override
    String getLogEntityId() {
        title
    }
    static constraints = {
        published nullable: true
    }
}
