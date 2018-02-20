package grails.plugins.orm.auditable

/**
 * Entities should implement this trait to provide automatic stamping of date and user information
 */
trait StampActor<D> {

    // We initialize these to non-null to they pass initial validation, they are set on insert/update
    String createdBy = "N/A"
    String lastUpdatedBy = "N/A"
}
