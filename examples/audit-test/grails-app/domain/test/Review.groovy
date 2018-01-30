package test

import grails.plugins.orm.auditable.Auditable

class Review implements Auditable {
    String name
    Book book

    /**
     * Override entity id to use a nested entityId from another domain object
     * @return
     */
    @Override
    String getLogEntityId() {
        "${name}|${book.logEntityId}"
    }

    static constraints = {
    }
}
