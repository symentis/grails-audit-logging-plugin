# Audit stampable actor plugin

This plugin applies a `Stampable` trait to all domain classes. The `Stampable` trait adds a `createdBy` and `lastUpdateBy` prop to the domainclass. If a DomainClass shouldn't be stamped, you can apply the `@ExcludeAuditStampActor` annotation.  
