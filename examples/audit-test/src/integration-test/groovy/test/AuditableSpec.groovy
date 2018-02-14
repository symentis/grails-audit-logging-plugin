package test

import grails.core.GrailsApplication
import grails.gorm.transactions.Rollback
import grails.plugins.orm.auditable.AuditEventType
import grails.plugins.orm.auditable.AuditLogContext
import grails.plugins.orm.auditable.Auditable
import grails.plugins.orm.auditable.resolvers.AuditRequestResolver
import grails.spring.BeanBuilder
import grails.testing.mixin.integration.Integration
import org.grails.datastore.gorm.GormEntity
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Integration
@Rollback
class AuditableSpec extends Specification {
    GrailsApplication grailsApplication
    @Shared TestEntity entity

    void setup() {
        entity = new TestEntity(property: 'foo')
    }

    void "excluded properties are respected"() {
        expect:
        AuditLogContext.withConfig(excluded: ['p1', 'p2']) { entity.getLogExcluded() } == ['p1', 'p2'] as Set<String>
    }

    void "included properties are respected"() {
        expect:
        AuditLogContext.withConfig(included: ['p1', 'p2']) { entity.getLogIncluded() } == ['p1', 'p2'] as Set<String>
    }

    void "mask properties are respected"() {
        expect:
        AuditLogContext.withConfig(mask: ['p1', 'p2']) { entity.getLogMaskProperties() } == ['p1', 'p2'] as Set<String>
    }

    void "ignore events are respected"() {
        expect:
        AuditLogContext.withConfig(ignoreEvents: [AuditEventType.DELETE]) {
            entity.getLogIgnoreEvents()
        } == [AuditEventType.DELETE] as Set<AuditEventType>
    }

    void "entity id uses ident by default"() {
        expect:
        entity.getLogEntityId() == entity.ident()
    }

    @Unroll
    void "convert logged property to string with logIds enabled for #value"(Object value, String expected) {
        when:
        String result = entity.convertLoggedPropertyToString('ignored', value)

        then:
        result == expected

        where:
        value                 | expected
        AuditEventType.DELETE | "DELETE"
        10                    | "10"
        1.29                  | "1.29"
        null                  | null
        "Foo"                 | "Foo"
        [1, 2, 3]             | "1, 2, 3"

        // Associated auditable
        new TestEntity(property: 'bar') | "[id:id]bar"
        [new TestEntity(property: 'bar'), new TestEntity(property: 'baz')] | "[id:id]bar, [id:id]baz"
    }

    @Unroll
    void "convert logged property to string with logIds disabled for #value"(Object value, String expected) {
        when:
        String result = AuditLogContext.withConfig(logIds: false) {
            entity.convertLoggedPropertyToString('ignored', value)
        }

        then:
        result == expected

        where:
        value                 | expected
        AuditEventType.DELETE | "DELETE"
        10                    | "10"
        1.29                  | "1.29"
        null                  | null
        "Foo"                 | "Foo"

        // Non-collection entity is still logged
        new TestEntity(property: 'bar') | "[id:id]bar"

        // Collection values are not logged
        [1, 2, 3] | "N/A"
        [new TestEntity(property: 'bar'), new TestEntity(property: 'baz')] | "N/A"
    }

    void "full class name logging is enabled when configured"(Boolean flag, result) {
        expect:
        AuditLogContext.withConfig(logFullClassName: flag) { entity.getLogClassName() } == result

        where:
        flag  | result
        true  | "test.TestEntity"
        false | "TestEntity"
    }

    void "logIds flag is respected by configuration"(Boolean flag, result) {
        expect:
        AuditLogContext.withConfig(logIds: flag) { entity.isLogAssociatedIds() } == result

        where:
        flag  | result
        true  | true
        false | false
    }

    void "verbose log is enabled for all events by flag"(Boolean flag, result) {
        expect:
        AuditLogContext.withConfig(verbose: flag) { entity.getLogVerboseEvents() } == result

        where:
        flag  | result
        true  | AuditEventType.values() as Set<AuditEventType>
        false | Collections.EMPTY_SET
    }

    void "verbose log flag is ignored if specific verboseEvents are given"(Boolean flag, result) {
        expect:
        AuditLogContext.withConfig(verbose: flag, verboseEvents: [AuditEventType.UPDATE]) {
            entity.getLogVerboseEvents()
        } == result

        where:
        flag  | result
        true  | [AuditEventType.UPDATE] as Set<AuditEventType>
        false | [AuditEventType.UPDATE] as Set<AuditEventType>
    }

    void "auditable property names omit excluded properties"() {
        given:
        Author author = new Author(name: 'Aaron', age: 41, famous: false)
        def allProps = Author.gormPersistentEntity.persistentProperties*.name as Set<String>

        when:
        def props = AuditLogContext.withConfig(excluded: ['name']) { author.getAuditablePropertyNames() } as Set<String>

        then:
        props == (allProps - "name")
    }

    void "auditable property names include only whitelist properties"() {
        given:
        Author author = new Author(name: 'Aaron', age: 41, famous: false)

        when:
        def props = AuditLogContext.withConfig(included: ['name']) { author.getAuditablePropertyNames() } as Set<String>

        then:
        props == ["name"] as Set<String>
    }

    void "auditable property names include whitelist even if excluded"() {
        given:
        Author author = new Author(name: 'Aaron', age: 41, famous: false)

        when:
        def props = AuditLogContext.withConfig(included: ['name'], excluded: ['name']) { author.getAuditablePropertyNames() } as Set<String>

        then:
        props == ["name"] as Set<String>
    }

    @DirtiesContext
    void "uses the registered audit resolver bean"() {
        given:
        BeanBuilder bb = new BeanBuilder()
        bb.beans { auditRequestResolver(TestAuditRequestResolver) }
        bb.registerBeans(grailsApplication.mainContext)

        when:
        String uri = entity.getLogURI()
        String actor = entity.getLogCurrentUserName()

        then:
        uri == "http://foo.com"
        actor == "Aaron"
    }
}

@SuppressWarnings("GroovyUnusedDeclaration")
class TestEntity implements Auditable, GormEntity<TestEntity> {
    String property
    String otherProperty
    String anotherProperty

    // Just for testing
    Serializable ident() {
        "id"
    }

    @Override
    String toString() {
        property
    }
}

class TestAuditRequestResolver implements AuditRequestResolver {
    @Override
    String getCurrentActor() {
        "Aaron"
    }

    @Override
    String getCurrentURI() {
        "http://foo.com"
    }
}
