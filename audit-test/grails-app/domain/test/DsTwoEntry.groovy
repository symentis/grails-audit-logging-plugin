package test

// domain class bound to datasource "second".
// No audit logging shall ever happen, as this ds has auditlog.disabled=true set
class DsTwoEntry {

  static auditable = [ignore:[]]

  String name
  String description

  static mapping = {
    datasource 'second' // should never be auditted, as this datasource has auditLog.disabled set!
  }
}
