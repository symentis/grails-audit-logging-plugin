package test

import grails.util.GrailsClassUtils as GCU;

import grails.test.mixin.integration.Integration
import grails.transaction.*
import grails.util.Holders
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import spock.lang.*

@Integration
@Rollback
class StampSpec extends Specification {
	def setup(){
		assert Train._stampable 
		assert Train._dateCreatedStampableProperty
		assert Train._createdByStampableProperty
		assert Train._lastUpdatedStampableProperty
		assert Train._lastUpdatedByStampableProperty
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
			def train = new Train(number:"10")
			train.save(flush:true)
		expect: 
			train.createdBy == 'SYS'
			train.lastUpdatedBy == 'SYS'
			train.dateCreated
			train.lastUpdated
			train.dateCreated == train.lastUpdated
		when: 
			Thread.sleep(1000)
			train.number = "20"
			train.save(flush:true)
		then:
			train.dateCreated != train.lastUpdated
	}

	void 'Test custom fieldnames'(){
		given:
			def truck = new Truck(number:"2")
		when:
			truck.save(flush:true)
		then:
			truck.originalWho == 'SYS'
			truck.lastWho == 'SYS'
			truck.originalWhen
			truck.lastWhen
	}
	
	void 'Test if autoTimestamp is disabled'(){
		given:
			def truckMapping = GrailsDomainBinder.getMapping(
				Holders.grailsApplication.domainClasses.find{it.name=='Truck'}.clazz
			)
			def trainMapping = GrailsDomainBinder.getMapping(
					Holders.grailsApplication.domainClasses.find{it.name=='Train'}.clazz
			)
		expect:
			!truckMapping.autoTimestamp
			!trainMapping.autoTimestamp
	}
}
