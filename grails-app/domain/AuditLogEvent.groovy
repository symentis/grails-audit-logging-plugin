class AuditLogEvent {
	Long id
	Long version
	Date dateCreated
	Date lastUpdated
	
	String actor
	String uri
	String className
	String persistedObjectId
	String persistedObjectVersion

	String eventName
	String propertyName
	String oldValue
	String newValue
	
	static constraints = {
		actor(nullable:true)
		uri(nullable:true)
		className(nullable:false)
		persistedObjectId(nullable:true)
		persistedObjectVersion(nullable:true)
		eventName(nullable:false)
		propertyName(nullable:true)
		oldValue(nullable:true)
		newValue(nullable:true)
	}
	
	static mapping = {
		table 'audit_log'
	}
}
