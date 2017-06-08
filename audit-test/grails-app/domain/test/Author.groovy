package test

class Author {
    String name
    Long age
    Boolean famous = false
    Publisher publisher
    // This should get masked globally
    String ssn = "123-456-7890"
    Date dateCreated
    Date lastUpdated
    String lastUpdatedBy
    Address address

    String handlerCalled = ""

    static transients = ['handlerCalled']
    static hasMany = [books: Book]
    static auditable = true


    static constraints = {
        lastUpdatedBy nullable: true
        publisher nullable: true
        address(nullable:true)
    }

    static embedded = ['address']

    // Event handlers
    def onSave = { newMap ->
        assert newMap
        handlerCalled += "onSave"
    }

    def onChange = { oldMap, newMap ->
        assert oldMap
        assert newMap
        handlerCalled += "onChange"
    }

    def onDelete = { oldMap ->
        assert oldMap
        handlerCalled += "onDelete"
    }
}

class Address implements Serializable {
    String street
    String city
    String zip
}
