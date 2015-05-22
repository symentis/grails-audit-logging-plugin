package test

class Resolution {

	String name

	static auditable = [ignoreEvents:["onChange","onSave"]]

	static constraints = {
	}
}
