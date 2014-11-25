package test

class Review {
    String name
    Book book

    // Use a domain class as entityId, will recursively get the entityId from book
    static auditable = [entityId: ['name', 'book']]

    static constraints = {
    }
    static mapping = {
      id generator:"increment", type:"long" // we have a default "uuid" mapping in the config
    }
}
