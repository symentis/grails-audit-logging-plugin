package test

class Book {
    String title
    String description
    Date published
    Long pages

    static belongsTo = [author: Author]

    // Show title instead of id
    static auditable = [entityId: 'title']

    static constraints = {
        published nullable: true
    }
}
