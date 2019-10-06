package test

import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty

class TestUtils {
    static getAuditableProperties(PersistentEntity entity, List<String> ignoreList) {
        List<String> properties = entity.persistentProperties.findResults { PersistentProperty p ->
            if (p.type in Collection){
                // persistentCollections are not logged anymore. See #153
                return null
            }
            return p.name
        }
        return properties - ignoreList
    }
}
