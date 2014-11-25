package test

class Book {
    String title
    String description
    Date published
    Long pages

    static hasMany = [reviews: Review]
    static belongsTo = [author: Author]

    // Show title instead of id
    static auditable = [entityId: 'title']

    static constraints = {
        published nullable: true
    }

    static mapping = {
      id generator:"increment", type:"long" // we have a default "uuid" mapping in the config
    }
}
