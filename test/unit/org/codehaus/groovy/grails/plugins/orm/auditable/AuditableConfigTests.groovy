package org.codehaus.groovy.grails.plugins.orm.auditable

/**
 * Created by IntelliJ IDEA.
 * User: hartsock
 * Date: 2/8/12
 * Time: 10:09 PM
 * To change this template use File | Settings | File Templates.
 */
class AuditableConfigTests extends GroovyTestCase {
    void testConfConstructor() {
        def conf = [handlersOnly:true,ignoreList:[1,2,3]]
        def config = new AuditableConfig(conf)
        assert config.ignoreList == conf.ignoreList
        assert config.handlersOnly == conf.handlersOnly
    }
}
