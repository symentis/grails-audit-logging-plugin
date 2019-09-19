package test

import grails.plugins.orm.auditable.Auditable

@SuppressWarnings("GroovyUnusedDeclaration")
class TestEntity implements Auditable {
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
