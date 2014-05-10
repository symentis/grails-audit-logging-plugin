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

    Map handlersMap = [:]
    static transients = ['handlersMap']

    static auditable = [handlersOnly : true]

    def onSave = { Map newMap ->
        assert newMap
        assert !newMap.containsKey(id)

        this.handlersMap = newMap
    }
}
