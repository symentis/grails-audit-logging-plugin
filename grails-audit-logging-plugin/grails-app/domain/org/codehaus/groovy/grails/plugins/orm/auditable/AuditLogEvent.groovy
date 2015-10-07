/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package org.codehaus.groovy.grails.plugins.orm.auditable

import grails.util.Holders

/**
 * AuditLogEvents are reported to the AuditLog table.
 * This requires you to set up a table or allow
 * Grails to create a table for you.
 */
class AuditLogEvent implements Serializable {
    private static final long serialVersionUID = 1L

    Object id // id can be configured type since 1.0.4 (depending on mapping config)

    static auditable = false

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

        if(Holders.config.auditLog.largeValueColumnTypes) {
            oldValue(nullable: true, maxSize: 65534)
            newValue(nullable: true, maxSize: 65534)
        }
        else {
            oldValue(nullable: true)
            newValue(nullable: true)
        }
    }

    static mapping = {
        // GPAUDITLOGGING-30
        table Holders.config.auditLog.tablename ?: 'audit_log'

        // Disable caching by setting auditLog.cacheDisabled = true in your app's Config.groovy
        if (!Holders.config.auditLog.cacheDisabled) {
            cache usage: 'read-only', include: 'non-lazy'
        }

        // GPAUDITLOGGING-70 configurable Datasource name to use for AuditLogEvent
        if (Holders.config.auditLog.useDatasource){
          datasource "$Holders.config.auditLog.useDatasource"
        }

        // GPAUDITLOGGING-29 support configurable id mapping for AuditLogEvent
        if (Holders.config.auditLog.idMapping) {
          def map = Holders.config.auditLog.idMapping
          id generator:map.generator, type:map.type, length:map.length
        } else {
          id generator:'native', type:'long' // default
        }

        // GH-106, Create strange dataType when using with database migration
        if (Holders.config.auditLog.largeValueColumnTypes) {
          oldValue type: "text"
          newValue type: "text"
        }

        autoImport false
        version false
    }

    /**
     * A very Groovy de-serializer that maps a stored map onto the object
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
                id: id,
                dateCreated: dateCreated,
                lastUpdated: lastUpdated,

                actor: actor,
                uri: uri,
                className: className,
                persistedObjectId: persistedObjectId,
                persistedObjectVersion: persistedObjectVersion,

                eventName: eventName,
                propertyName: propertyName,
                oldValue: oldValue,
                newValue: newValue,
        ]
        out.writeObject(map)
    }

    String toString() {
        String actorStr = actor ? "user ${actor}" : "user ?"
        "audit log ${dateCreated} ${actorStr} " +
                "${eventName} ${className} " +
                "id:${persistedObjectId} version:${persistedObjectVersion}"
    }
}
