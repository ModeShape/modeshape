#!/bin/sh

##########################################################################################################################################
# This script installs a downloaded version of JBoss EAP into the local Maven Repository.
# Its purpose is to allow running the "integration" and "assembly" profiles from the build, run the AS7/EAP integration tests
# The values used for "artifactId", "groupId" and "version" *must* match the following properties from the modeshape-parent/pom.xml file:
# groupId <-> ${jboss.eap.groupId}
# artifactId <-> ${jboss.eap.artifactId}
# version <-> ${jboss.eap.version}
##########################################################################################################################################

if [ "$#" -ne 1 ] ; then
	echo "Usage: install-eap.sh <eap_zip_file>"
	exit 1
else
	 mvn install:install-file -Dfile="$1" -DgroupId=org.jboss.as -DartifactId=jboss-eap -Dversion=6.3.0.GA -Dpackaging=zip
fi
