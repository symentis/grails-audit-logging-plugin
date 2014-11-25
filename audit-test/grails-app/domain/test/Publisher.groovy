package test

class Publisher {
    String code
    String name

    boolean active = false

    // Only audit if active
    // Provide a list of id's instead of just a single id, these are concatenated with a '|'
    static auditable = [entityId: ['code', 'name'], isAuditable: { event, obj ->
        obj.active == true
    }]

    static constraints = {
    }
}
