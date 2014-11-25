package test

class Runway {
    Integer length
    Integer width

    static belongsTo = [airport: Airport]

    static mapping = {
      id generator:"increment", type:"long" // we have a default "uuid" mapping in the config
    }
}
