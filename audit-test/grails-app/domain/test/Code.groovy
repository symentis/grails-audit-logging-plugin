package test

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = "tag, code")
class Code implements Serializable {
    String tag
    Integer code
    String value
    String description

    static mapping = {
        id composite: ['tag', 'code']
    }

    Map handlersMap = [:], handlersOldMap = [:]
    static transients = ['handlersMap', 'handlersOldMap']

    static auditable = [handlersOnly : true]

    def onSave = { Map newMap ->
        assert newMap
        assert !newMap.containsKey(id)

        this.handlersMap = newMap
    }

    def onChange = { Map oldMap, Map newMap ->
        assert oldMap && newMap
        assert !oldMap.id && !newMap.id

        this.handlersMap = newMap
        this.handlersOldMap = oldMap
    }
}
