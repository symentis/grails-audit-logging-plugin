package test

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

class StampSpec extends IntegrationSpec{
	def setup(){
		assert Train.stampable
	}
	
	void 'Test Stamp AST transformations'(){
		given:
			def train = new Train()
		when:
			def expectedProperties = [
				'dateCreated':Date.class,
				'lastUpdated':Date.class,
				'lastUpdatedBy':String.class,
				'createdBy':String.class
			]
		then:
			expectedProperties.each{
				assert train.hasProperty(it.key)
				assert GCU.getPropertyType(Train.class,it.key) == it.value
			}
	}
	
	void 'Test custom Stamp AST transformation'(){
		given:
			def coach = new Coach()
		when:
			def expectedProperties = [
				'dateCreated':Date.class,
				'lastUpdated':Date.class,
				'lastUpdatedBy':String.class,
				'createdBy':String.class,
				'removed':Boolean.class
			]
		then:
			expectedProperties.each{
				assert coach.hasProperty(it.key)
				assert GCU.getPropertyType(Coach.class,it.key) == it.value
			}
	}
	
	void 'Test Auto Stamp'(){
		given: 
			def train = new Train()
		when:
			train.number="10"
			train.save(flush:true)
		then: 
			train.createdBy == 'SYS'
			train.lastUpdatedBy == 'SYS'
	}
}
