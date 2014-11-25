package test

class Review {
    String name
    Book book

    // Use a domain class as entityId, will recursively get the entityId from book
    static auditable = [entityId: ['name', 'book']]

    static constraints = {
    }
}
