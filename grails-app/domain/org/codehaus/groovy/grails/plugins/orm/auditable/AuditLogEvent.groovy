package org.codehaus.groovy.grails.plugins.orm.auditable
/**
 * AuditLogEvents are reported to the AuditLog table
 * this requires you to set up a table or allow
 * Grails to create a table for you.
 */
class AuditLogEvent implements java.io.Serializable {
  private static final long serialVersionUID = 1L
  Date dateCreated
  Date lastUpdated

  String actor
  String uri
  String className
  Long persistedObjectId
  Long persistedObjectVersion = 0

  String eventName
  String propertyName
  String oldValue
  String newValue

  static constraints = {
    actor(nullable:true)
    uri(nullable:true)
    className(nullable:false)
    persistedObjectId(nullable:true)
    persistedObjectVersion(nullable:true)
    eventName(nullable:false)
    propertyName(nullable:true)
    oldValue(nullable:true)
    newValue(nullable:true)
  }

  static mapping = {
    table 'audit_log'
    cache usage:'read-only', include:'non-lazy'
    // id composite:['className','persistedObjectId','persistedObjectVersion', 'eventName','propertyName']
    // version false
  }

  /**
   * A very Groovy de-serializer that maps a stored map onto the object
   * assuming that the keys match attribute properties.
   */
  private void readObject(java.io.ObjectInputStream input) throws IOException, ClassNotFoundException {
    def map = (java.util.LinkedHashMap) input.readObject()
    map.each({ k,v -> this."$k" = v })
  }

  /**
   * Because Closures do not serialize we can't send the constraints closure
   * to the Serialize API so we have to have a custom serializer to allow for
   * this object to show up inside a webFlow context.
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    def map = [
            id:id,
            version:version,
            dateCreated:dateCreated,
            lastUpdated:lastUpdated,

            actor:actor,
            uri:uri,
            className:className,
            persistedObjectId:persistedObjectId,
            persistedObjectVersion:persistedObjectVersion,

            eventName:eventName,
            propertyName:propertyName,
            oldValue:oldValue,
            newValue:newValue,
    ]
    out.writeObject(map)
  }
}

