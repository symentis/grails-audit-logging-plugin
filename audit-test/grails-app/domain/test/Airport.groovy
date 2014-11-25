package test

class Airport {
    String code
    String name

    Map handlerMap = [:]
    static transients = ['handlerMap']

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
}
