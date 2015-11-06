package test

class Airport {
    String code
    String name

    Map handlerMap = [:], handlerOldMap = [:]
    static transients = ['handlerMap', 'handlerOldMap']

    static hasMany = [runways: Runway]

    static auditable = [handlersOnly: true]

    def onSave = { newMap ->
        assert newMap
        assert newMap.id instanceof Long

        this.handlerMap = newMap
    }

    def onDelete = { oldMap ->
        assert oldMap

        this.handlerMap = oldMap
    }

    def onChange = { oldMap, newMap ->
        assert oldMap && oldMap.id
        assert newMap && oldMap.id
        assert oldMap.id == newMap.id

        this.handlerMap = newMap
        this.handlerOldMap = oldMap
    }
}
