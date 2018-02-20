package grails.plugins.orm.auditable

import grails.gorm.annotation.Entity
import groovy.transform.ToString

/**
 * AuditTrails are reported to the AuditLog table.
 * This requires you to set up a table or allow
 * Grails to create a table for you. (e.g. DDL or db-migration plugin)
 */
@ToString(includes = 'id,className,actor,propertyName,oldValue,newValue')
@Entity
class AuditLogEvent implements Serializable {
    private static final long serialVersionUID = 1L

    String id
    Date dateCreated
    Date lastUpdated

    String actor
    String uri
    String className
    String persistedObjectId
    Long persistedObjectVersion = 0

    String eventName
    String propertyName
    String oldValue
    String newValue

    static constraints = {
        actor(nullable: true)
        uri(nullable: true)
        className(nullable: true)
        persistedObjectId(nullable: true)
        persistedObjectVersion(nullable: true)
        eventName(nullable: true)
        propertyName(nullable: true)

        oldValue(nullable: true)
        newValue(maxSize: 10000, nullable: true)

        // for large column support (as in < 1.0.6 plugin versions), use
        // oldValue(nullable: true, maxSize: 65534)
        // newValue(nullable: true, maxSize: 65534)
    }

    static mapping = {

        table 'audit_log'

        cache usage: 'read-only', include: 'non-lazy'


        // no HQL queries package name import (was default in 1.x version)
        //autoImport false

        // Since 2.0.0, mapping is not done by config anymore. Configure your ID mapping here.
        id generator: "uuid2", type: 'string', length: 36

        version false
    }

    static namedQueries = {
        forQuery { String q ->
            if (!q?.trim()) return // return all
            def queries = q.tokenize()?.collect {
                '%' + it.replaceAll('_', '\\\\_') + '%'
            }
            queries.each { query ->
                or {
                    ilike 'actor', query
                    ilike 'persistedObjectId', query
                    ilike 'propertyName', query
                    ilike 'oldValue', query
                    ilike 'newValue', query
                }
            }
        }

        forDateCreated { Date date ->
            if (!date) return
            gt 'dateCreated', date
        }
    }

    /**
     * Deserializer that maps a stored map onto the object
     * assuming that the keys match attribute properties.
     */
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        def map = input.readObject()
        map.each { k, v -> this."$k" = v }
    }

    /**
     * Because Closures do not serialize we can't send the constraints closure
     * to the Serialize API so we have to have a custom serializer to allow for
     * this object to show up inside a webFlow context.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        def map = [
            id                    : id,
            dateCreated           : dateCreated,
            lastUpdated           : lastUpdated,

            actor                 : actor,
            uri                   : uri,
            className             : className,
            persistedObjectId     : persistedObjectId,
            persistedObjectVersion: persistedObjectVersion,

            eventName             : eventName,
            propertyName          : propertyName,
            oldValue              : oldValue,
            newValue              : newValue,
        ]
        out.writeObject(map)
    }
}