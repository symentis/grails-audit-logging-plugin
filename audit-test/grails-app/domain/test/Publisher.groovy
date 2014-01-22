package test

class Publisher {
    String code
    String name

    boolean active = false

    // Only audit if active, show the code, not the id
    static auditable = [entityId: 'code', isAuditable: { event, obj ->
        obj.active == true
    }]

    static constraints = {
    }
}
