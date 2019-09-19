package test

class NonAuditableCompositeId implements Serializable {

    String foo
    String bar

    @Override
    String toString() {
      "toString_for_non_auditable_${foo}_${bar}"
    }

    static constraints = {
    }

    static mapping = {
        id composite: ['foo', 'bar']
    }
}
