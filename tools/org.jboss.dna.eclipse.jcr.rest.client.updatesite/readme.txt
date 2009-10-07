***** Steps to create an update site archive *****

1. Make sure you get a clean Maven build by running "mvn clean install" in the "trunk/" directory.

2. In the "web/dna-web-jcr-rest-client/" directory run "mvn assembly:assembly" (which generates a dependencies jar)

3. Copy these 2 files from "web/dna-web-jcr-rest-client/target/" to "tools/org.jboss.dna.eclipse.jcr.rest.client" project:
    - dna-web-jcr-rest-client-[release]-jar-with-dependencies.jar
    - dna-web-jcr-rest-client-[release]-sources.jar

4. Update the feature version number by editing "feature.xml" in the "tools/org.jboss.dna.eclipse.jcr.rest.client.feature" project.

5. Open "site.xml" in the tools/org.jboss.dna.eclipse.jcr.rest.client.updatesite" project. If the feature on the "Site Map" tab
   is not right, delete the existing one and then click "Add Feature" to add the correct version of the feature plugin.
   
6. Once the feature is added click "Build All." This will create 2 folders (features, plugins) and 2 files (artifacts.xml, content.xml).

7. At the top of both the artifacts.xml and content.xml, change the name to "JBoss DNA JCR REST Eclipse Client."

8. Create a zip archive containing the following: features/, plugins/, site.xml, artifacts.xml, & contents.xml.

***** Additional Information *****

- To install the archive download and install a new copy of Eclipse. Use Help -> Software Updates to add the local archive site.

- In order to test you need a DNA REST server. Copy the "resources.war" file found in the "targets/" directory of the
  "web/dna-web-jcr-rest-war" project. For JBoss copy it into the "[JBOSS-INSTALL-DIR]/server/[CONFIG-NAME]/deploy/" directory. Then
  in Eclipse using the DNA Servers View create a new server with the appropriate URL (for example: http://localhost:8080).
  The user is "dnauser" and the passowrd is "password".