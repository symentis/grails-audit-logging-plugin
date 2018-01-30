package grails.plugins.orm.auditable

import org.grails.datastore.gorm.GormEntity

/**
 * Entities should implement this trait to provide automatic stamping of date and user information
 */
trait Stampable<D> extends GormEntity<D> {
    Date dateCreated
    Date lastUpdated

    String createdBy
    String lastUpdatedBy
}
