package test

import grails.util.GrailsClassUtils as GCU;

import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*

@Integration
@Rollback
class StampSpec extends Specification {
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
			expectedProperties.every{
				train.hasProperty(it.key) &&
				GCU.getPropertyType(Train.class,it.key) == it.value
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
			expectedProperties.every {
				coach.hasProperty(it.key) &&
				GCU.getPropertyType(Coach.class,it.key) == it.value
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
