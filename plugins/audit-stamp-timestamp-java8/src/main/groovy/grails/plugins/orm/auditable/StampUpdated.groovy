package grails.plugins.orm.auditable

import java.time.LocalDateTime

trait StampUpdated {
    LocalDateTime lastUpdated
}
