package test

import grails.plugins.orm.auditable.Auditable

class Author implements Auditable {
    String name
    Long age
    Boolean famous = false

    // This should get masked globally
    String ssn = "123-456-7890"

    Date dateCreated
    Date lastUpdated
    String lastUpdatedBy

    static constraints = {
        lastUpdatedBy nullable: true
    }
}
