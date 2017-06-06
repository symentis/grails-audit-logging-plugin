package test

class Tunnel {
    static auditable = true

    String name
    String description

    static constraints = {
        description maxSize:1073741824, nullable:true
    }
}
