package grails.plugins.orm.auditable

import java.time.LocalDateTime

trait StampCreated {
    LocalDateTime dateCreated
}
