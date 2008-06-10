/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.tests.integration.jackrabbit;

import static org.junit.Assert.assertNotNull;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JackrabbitBasicFunctionTest {

    public static final String TESTATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTATA_PATH + "jackrabbitDerbyTestRepositoryConfig.xml";
    public static final String DERBY_SYSTEM_HOME = JACKRABBIT_DATA_PATH + "/derby";

    private Logger logger;
    private Repository repository;

    @Before
    public void beforeEach() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up Derby and the logger ...
        System.setProperty("derby.system.home", DERBY_SYSTEM_HOME);
        logger = Logger.getLogger(JackrabbitBasicFunctionTest.class);

        // Set up the transient repository ...
        this.repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);
    }

    @After
    public void afterEach() {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);
    }

    @Test
    public void shouldConnectWithAnonymous() throws Exception {
        Session session = null;
        try {
            session = this.repository.login();
            assertNotNull(session);
            String username = session.getUserID();
            String name = repository.getDescriptor(Repository.REP_NAME_DESC);
            logger.info(MockI18n.passthrough, "Logged in as " + username + " to a " + name + " repository");
        } finally {
            if (session != null) session.logout();
        }
    }

    @Test
    public void shouldConnectWithSimpleCredentials() throws Exception {
        Session session = null;
        try {
            SimpleCredentials creds = new SimpleCredentials("jsmith", "password".toCharArray());
            session = this.repository.login(creds);
            assertNotNull(session);
            String username = session.getUserID();
            String name = repository.getDescriptor(Repository.REP_NAME_DESC);
            logger.info(MockI18n.passthrough, "Logged in as " + username + " to a " + name + " repository");
        } finally {
            if (session != null) session.logout();
        }
    }

    @Test
    public void shouldSupportConcurrentSessionsForDifferentUsers() throws Exception {
        List<Session> sessions = new ArrayList<Session>();
        try {
            for (int i = 0; i != 10; ++i) {
                SimpleCredentials creds = new SimpleCredentials("user" + i, ("secret" + i).toCharArray());
                Session session = this.repository.login(creds);
                assertNotNull(session);
                sessions.add(session);
                logger.info(MockI18n.passthrough, "Logged in as " + session.getUserID());
            }
        } finally {
            while (!sessions.isEmpty()) {
                sessions.remove(0).logout();
            }
        }
    }

    @Test( expected = javax.jcr.AccessDeniedException.class )
    public void shouldNotAllowAnonymousUserToCreateContent() throws Exception {
        Session session = null;
        try {
            session = this.repository.login();
            assertNotNull(session);
            Node root = session.getRootNode();

            // Store content
            Node hello = root.addNode("hello");
            Node world = hello.addNode("world");
            world.setProperty("message", "Hello, World!");
            session.save(); // Should fail
        } finally {
            if (session != null) session.logout();
        }
    }

    @Test
    public void shouldAllowAuthenticatedUserToCreateAndManipulateContent() throws Exception {
        SimpleCredentials creds = new SimpleCredentials("user", "secret".toCharArray());
        Session session = null;
        try {
            session = this.repository.login(creds);
            assertNotNull(session);
            Node root = session.getRootNode();

            // Store content
            Node hello = root.addNode("hello");
            Node world = hello.addNode("world");
            world.setProperty("message", "Hello, World!");
            session.save();

            // Retrieve content ...
            Node node = root.getNode("hello/world");
            this.logger.info(MockI18n.passthrough, "Node 'hello/world' has path: " + node.getPath());
            this.logger.info(MockI18n.passthrough, "Node 'hello/world' has 'message' property: "
                                                   + node.getProperty("message").getString());
        } finally {
            if (session != null) session.logout();
        }

        try {
            session = this.repository.login(creds);
            assertNotNull(session);
            Node root = session.getRootNode();

            // Retrieve content
            Node node = root.getNode("hello/world");
            this.logger.info(MockI18n.passthrough, "Node 'hello/world' has path: " + node.getPath());
            this.logger.info(MockI18n.passthrough, "Node 'hello/world' has 'message' property: "
                                                   + node.getProperty("message").getString());

            // Remove content
            this.logger.info(MockI18n.passthrough, "Node 'hello' is being removed");
            root.getNode("hello").remove();
            session.save();
        } finally {
            if (session != null) session.logout();
        }
    }

    @Test
    public void shouldImportFile() throws Exception {
        SimpleCredentials creds = new SimpleCredentials("user", "secret".toCharArray());
        Session session = null;
        try {
            session = this.repository.login(creds);
            assertNotNull(session);

            // Use the root node as a starting point
            Node root = session.getRootNode();

            // Import the XML file unless already imported
            if (!root.hasNode("importxml")) {
                System.out.print("Importing xml... ");
                // Create an unstructured node under which to import the XML
                Node node = root.addNode("importxml", "nt:unstructured");
                // Import the file "test.xml" under the created node
                FileInputStream xml = new FileInputStream(TESTATA_PATH + "jcr-import-test.xml");
                try {
                    session.importXML(node.getPath(), xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                } finally {
                    xml.close();
                }
                // Save the changes to the repository
                session.save();
                System.out.println("done.");
            }

            JackrabbitTestUtil.dumpNode(root, System.out, true);
        } finally {
            if (session != null) session.logout();
        }
    }

}
