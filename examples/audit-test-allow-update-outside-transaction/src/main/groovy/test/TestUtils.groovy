package test

import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty

class TestUtils {
    static getAuditableProperties(PersistentEntity entity, List<String> ignoreList) {
        List<String> properties = entity.persistentProperties.findResults { PersistentProperty p ->
            return p.name
        }
        return properties - ignoreList
    }
}
