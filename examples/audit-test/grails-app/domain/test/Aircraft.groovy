package test

import grails.plugins.orm.auditable.Auditable

class Aircraft implements Auditable {
    String type
    String description

    Map handlersMap = [:], handlersOldMap = [:]
    static transients = ['handlersMap', 'handlersOldMap']

    static mapping = {
        id name: 'type', generator: 'assigned'
    }

    static constraints = {
        type bindable: true
    }

    def onSave = { newMap ->
        assert newMap
        assert !newMap.id

        this.handlersMap = newMap
    }

    def onDelete = { oldMap ->
        assert oldMap

        this.handlersMap = oldMap
    }

    def onChange = { oldMap, newMap ->
        assert oldMap && newMap
        assert !newMap.id && !oldMap.id

        this.handlersOldMap = oldMap
        this.handlersMap = newMap
    }
}
