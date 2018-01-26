package grails.plugins.orm.auditable

/**
 * Entities should implement this trait to provide automatic stamping of date and user information
 */
trait Stampable {
    Date dateCreated
    Date lastUpdated

    String createdBy
    String lastUpdatedBy
}
