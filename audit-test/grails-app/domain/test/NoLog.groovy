package test

// domain class bound to datasource "disabledauditlog". No audit logging shall ever happen. See tests
class NoLog {

  static auditable = [ignore:[]]

  String name
  String description

  static mapping = {
    datasource 'disabledauditlog'
  }
}
