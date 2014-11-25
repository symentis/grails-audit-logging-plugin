package test

class Heliport {
    String code
    String name

    static auditable = [ignore:[]]

    static mapping = {
        version false
        id generator:"increment", type:"long" // we have a default "uuid" mapping in the config
    }
}
