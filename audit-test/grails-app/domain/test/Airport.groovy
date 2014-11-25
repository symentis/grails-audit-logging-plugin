package test

class Airport {
    String code
    String name

    Map handlerMap = [:]
    static transients = ['handlerMap']

    static hasMany = [runways: Runway]

    static auditable = [handlersOnly: true]

  static mapping = {
    id generator:"increment", type:"long" // we have a default "uuid" mapping in the config
  }

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
