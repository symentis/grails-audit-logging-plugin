package test

class Heliport {
    String code
    String name

    static auditable = [ignore:[]]

    static mapping = {
        version false
    }
}
