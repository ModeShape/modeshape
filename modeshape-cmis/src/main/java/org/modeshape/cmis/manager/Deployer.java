/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.cmis.manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author kulikov
 */
public class Deployer {
    private String deploymentDir;

    public Deployer() {

    }

    public Deployer(String deploymentDir) {
        this.deploymentDir = deploymentDir;
    }

    public void install(String context, String jndiName) throws IOException {

        this.uninstall(context);

        //make dirs
        File app = new File(deploymentDir +"/modeshape-" + context + "-cmis.war");
        app.mkdir();

        File css = new File(deploymentDir +"/modeshape-" + context + "-cmis.war/css");
        css.mkdir();

        File images = new File(deploymentDir +"/modeshape-" + context + "-cmis.war/images");
        images.mkdir();

        File web = new File(deploymentDir +"/modeshape-" + context + "-cmis.war/web");
        web.mkdir();

        File webinf = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF");
        webinf.mkdir();

        File lib = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF/lib");
        lib.mkdir();

        File wsdl = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF/wsdl");
        wsdl.mkdir();

        File classes = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF/classes");
        classes.mkdir();

        File metainf = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF/classes/META-INF");
        metainf.mkdir();

        File services = new File (deploymentDir +"/modeshape-" + context + "-cmis.war/WEB-INF/classes/META-INF/services");
        services.mkdir();

        //Do copy
        Copy cp = new Copy();

        //root
        cp.copy("/index.html", app);
        cp.copy("/css/opencmis.css", app);

        cp.copy("/web/createdocument.html", app);
        cp.copy("/web/createfolder.html", app);
        cp.copy("/web/demo.html", app);
        cp.copy("/web/index.html", app);
        cp.copy("/web/opencmis.js", app);

        cp.copyBinary("/images/asf_logo.png", app);
        cp.copyBinary("/images/chemistry_logo_small.png", app);

        //WEB-INF
        cp.copy("/web.xml", webinf, "context:" + context);
        cp.copy("/jboss-deployment-structure.xml", webinf);
        cp.copy("/sun-jaxws.xml", webinf);

        cp.copy("/wsdl/CMIS-Core.xsd", webinf);
        cp.copy("/wsdl/CMIS-Messaging.xsd", webinf);
        cp.copy("/wsdl/CMISWS-Service.wsdl", webinf);
        cp.copy("/wsdl/xml.xsd", webinf);

        //WEB-INF/lib
        cp.copyBinary("/lib/activation-1.1.jar", webinf);
        cp.copyBinary("/lib/antlr-2.7.7.jar", webinf);
        cp.copyBinary("/lib/antlr-runtime-3.2.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-client-api-0.7.0.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-client-bindings-0.7.0.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-client-impl-0.7.0.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-commons-api-0.7.0.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-commons-impl-0.7.0.jar", webinf);
//        cp.copyBinary("/lib/chemistry-opencmis-server-bindings-0.7.0.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-server-bindings-0.7.0-classes.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-server-jcr-0.7.0-classes.jar", webinf);
        cp.copyBinary("/lib/chemistry-opencmis-server-support-0.7.0.jar", webinf);
        cp.copyBinary("/lib/commons-codec-1.4.jar", webinf);
        cp.copyBinary("/lib/commons-collections-3.2.1.jar", webinf);
        cp.copyBinary("/lib/commons-fileupload-1.2.1.jar", webinf);
        cp.copyBinary("/lib/commons-io-2.0.1.jar", webinf);
        cp.copyBinary("/lib/commons-lang-2.6.jar", webinf);
        cp.copyBinary("/lib/commons-logging-1.1.1.jar", webinf);
        cp.copyBinary("/lib/jaxb-api-2.1.jar", webinf);
//        cp.copyBinary("/lib/jaxb-impl-2.1.11.jar", webinf);
        cp.copyBinary("/lib/jaxb-impl-2.1.12.jar", webinf);
        cp.copyBinary("/lib/jaxws-api-2.1.jar", webinf);
        cp.copyBinary("/lib/jaxws-rt-2.1.7.jar", webinf);
        cp.copyBinary("/lib/joda-time-1.6.2.jar", webinf);
        cp.copyBinary("/lib/mimepull-1.3.jar", webinf);
        cp.copyBinary("/lib/org.osgi.core-1.0.0.jar", webinf);
        cp.copyBinary("/lib/resolver-20050927.jar", webinf);
        cp.copyBinary("/lib/saaj-api-1.3.jar", webinf);
        cp.copyBinary("/lib/saaj-impl-1.3.3.jar", webinf);
        cp.copyBinary("/lib/stax-api-1.0.1.jar", webinf);
        cp.copyBinary("/lib/stax-api-1.0.jar", webinf);
        cp.copyBinary("/lib/stax-ex-1.2.jar", webinf);
        cp.copyBinary("/lib/streambuffer-0.9.jar", webinf);
        cp.copyBinary("/lib/stringtemplate-3.2.jar", webinf);
        cp.copyBinary("/lib/wstx-asl-3.2.3.jar", webinf);

        //WEB-INF/classes
        cp.copy("/repository.properties", classes, "jndi-name:" + jndiName);

        //WEB-INF/classes/META-INF
        cp.copy("/MANIFEST.MF", metainf);

        //WEB-INF/classes/META-INF/services
        cp.copy("/javax.jcr.RepositoryFactory", services);

        this.touch(context);
    }

    private void touch(String context) throws IOException {
        FileOutputStream fout = new FileOutputStream(deploymentDir +"/modeshape-" + context + "-cmis.war.dodeploy");
        fout.write(("modeshape-" + context + "-cmis.war").getBytes());
        fout.flush();
        fout.close();
    }

    public void uninstall(String context) {
        File rootDir = new File(deploymentDir +"/modeshape-" + context + "-cmis.war");
        
        File failed = new File(deploymentDir +"/modeshape-" + context + "-cmis.war.failed");
        failed.delete();

        File deployed = new File(deploymentDir +"/modeshape-" + context + "-cmis.war.deployed");
        deployed.delete();

        rootDir.delete();
    }
    
    public void printHelp() {
        System.out.println("Usage: java -jar modeshape-cmis-manager command context-name repository-URL");
        System.out.println("Sample: java -jar modeshape-cmis-manager -i sample jndi:///jcr/sample");
        System.out.println("Commands:");
        System.out.println("-i Intall new CMIS bridge");
        System.out.println("-u Unintall CMIS bridge");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Modeshape CMIS bridge management util");

        Deployer deployer = new Deployer();
        if (args.length == 0) {
            deployer.printHelp();
            System.exit(0);
        }

        if (args.length == 1) {
            if (args[0].toLowerCase().contains("help")) {
                deployer.printHelp();
                System.exit(0);
            }
        }

        String JBOSS_HOME = System.getenv("JBOSS_HOME");
        if (JBOSS_HOME == null) {
            System.err.println("JBOSS_HOME environment variable not set");
            System.exit(1);
        } 

        System.out.println("JBOSS_HOME=" + JBOSS_HOME);
        deployer.deploymentDir = JBOSS_HOME +"/standalone/deployments";

        String cmd = args[0];
        if (cmd.equalsIgnoreCase("-i")) {
            if (args.length < 3) {
                System.err.println("Not enough parameters");
                deployer.printHelp();
                System.exit(1);
            }

            deployer.install(args[1], args[2]);
        } else if (cmd.equalsIgnoreCase("-u")) {
            if (args.length < 2) {
                System.err.println("Not enough parameters");
                deployer.printHelp();
                System.exit(1);
            }

            deployer.uninstall(args[1]);
        } else {
            System.err.println("Unknown command: " + cmd);
            deployer.printHelp();
        }
    }
}
