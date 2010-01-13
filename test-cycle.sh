grails clean && grails package-plugin && pushd && grails uninstall-plugin audit-logging && grails clean && grails install-plugin ../trunk/grails-audit-logging-0.5.zip && grails run-app; pushd
