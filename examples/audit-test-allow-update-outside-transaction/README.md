This is a sample test project for audit-logging.

You can run the tests by

    ./gradlew check
    
In comparison to `examples/audit-test` this application tests support for applications that are using `hibernate.allow_update_outside_transaction = true` and are not using a second datasource for `AuditTrail`.

