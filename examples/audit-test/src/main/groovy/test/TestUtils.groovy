package test

import org.grails.datastore.mapping.model.PersistentEntity

class TestUtils {
    static getAuditableProperties(PersistentEntity entity, List<String> ignoreList) {
        return entity.persistentProperties.collect { it.name } - ignoreList
    }
}
