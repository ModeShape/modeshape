@echo off

REM ---------------------------------------------------------------------------------------------------------------------------------------
REM This script installs a downloaded version of JBoss EAP into the local Maven Repository.
REM Its purpose is to allow running the "integration" and "assembly" profiles from the build, run the AS7/EAP integration tests
REM The values used for "artifactId", "groupId" and "version" *must* match the following properties from the modeshape-parent/pom.xml file:
REM groupId <-> ${jboss.eap.groupId}
REM artifactId <-> ${jboss.eap.artifactId}
REM version <-> ${jboss.eap.version}
REM ---------------------------------------------------------------------------------------------------------------------------------------

if "%1" == "" (
    echo "Usage: install-eap.bat <eap_zip_file>"
) else (
    mvn install:install-file -Dfile="%1" -DgroupId=org.jboss.as -DartifactId=jboss-eap -Dversion=6.3.0.Beta1 -Dpackaging=zip
)
