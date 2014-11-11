package test

class Aircraft {
    String type
    String description

    Map handlersMap = [:]
    static transients = ['handlersMap']

    static auditable = [handlersOnly : true]

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
}
