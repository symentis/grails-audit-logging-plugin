
String grailsHomeRoot = '/eclipse-common/grails'
String projectDirCommon = "target"
String dotGrailsCommon = "${System.properties.getProperty('user.home')}/.grails"


v20 {
	grailsVersion = '2.0.4'
	pluginVersion = version
	dotGrails = dotGrailsCommon
	projectDir = projectDirCommon
	grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}

v211 {
    grailsVersion = '2.1.1' // 2.1.5 has a plugin i18n bug
    pluginVersion = version
    dotGrails = dotGrailsCommon
    projectDir = projectDirCommon
    grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}

v21 {
	grailsVersion = '2.1.4' // 2.1.5 has a plugin i18n bug
	pluginVersion = version
	dotGrails = dotGrailsCommon
	projectDir = projectDirCommon
	grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}

v22 {
	grailsVersion = '2.2.4'
	pluginVersion = version
	dotGrails = dotGrailsCommon
	projectDir = projectDirCommon
	grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}

v23 {
	grailsVersion = '2.3.11'
	pluginVersion = version
	dotGrails = dotGrailsCommon
	projectDir = projectDirCommon
	grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}


v24 {
	grailsVersion = '2.4.2'
	pluginVersion = version
	dotGrails = dotGrailsCommon
	projectDir = projectDirCommon
	grailsHome = grailsHomeRoot + '/grails-' + grailsVersion
}