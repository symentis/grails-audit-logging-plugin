package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * The AuditableConfig is an immutable object and so it should not be possible to
 * alter the data contained inside once the constructor has built an object for us
 * this test illustrates that fact for us.
 */
class AuditableConfigTest extends GroovyTestCase {
    AuditableConfig auditableConfig
    void setUp() {
        auditableConfig = new AuditableConfig([handlersOnly:true,ignoreList:['a','b','c'],transactional:false])
    }

    void tearDown() {

    }

    void testGetHandlersOnly() {
        assert auditableConfig.handlersOnly == true
        assert auditableConfig.getHandlersOnly() == auditableConfig.handlersOnly
    }

    void testSetHandlersOnly() {
        shouldFail {
            auditableConfig.handlersOnly = false
        }
    }

    void testGetIgnoreList() {
        assert auditableConfig.ignoreList == ['a','b','c']
    }

    void testSetIgnoreList() {
        shouldFail {
            auditableConfig.ignoreList = []
        }
    }

    void testGetTransactional() {
        assert auditableConfig.transactional == false
    }

    void testSetTransactional() {
        shouldFail {
            auditableConfig.transactional = true
        }
    }

    void testConfConstructor() {
        def conf = [handlersOnly:true,ignoreList:[1,2,3]]
        def config = new AuditableConfig(conf)
        assert config.ignoreList == conf.ignoreList
        assert config.handlersOnly == conf.handlersOnly
    }

}
