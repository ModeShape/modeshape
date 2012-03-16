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
package org.modeshape.test.integration.jpa;

import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.CndNodeTypeReader;
import org.modeshape.jcr.JaasTestUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.test.ModeShapeUnitTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.GregorianCalendar;

public class JcrRepositoryWithJpaSourceTest extends ModeShapeUnitTest {

    private JcrEngine engine;
    private Repository repository;
    private Session session;
    private Credentials credentials;

    @BeforeClass
    public static void beforeAll() {
        // Initialize the JAAS configuration to allow for an admin login later
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    @Before
    public void beforeEach() throws Exception {
        final URL configUrl = getClass().getResource("/tck/simple-jpa/configRepository.xml");
        final String workspaceName = "otherWorkspace";
        assert configUrl != null;

        // Load the configuration from the config file ...
        JcrConfiguration config = new JcrConfiguration();
        config.loadFrom(configUrl);

        // Start the engine ...
        engine = config.build();
        engine.start();

        // Set up the fake credentials ...
        credentials = new SimpleCredentials("superuser", "superuser".toCharArray());

        repository = engine.getRepository("Test Repository Source");
        assert repository != null;
        session = repository.login(credentials, workspaceName);
        assert session != null;
    }

    @After
    public void afterEach() throws Exception {
        if (engine != null) {
            try {
                if (session != null) {
                    try {
                        session.logout();
                    } finally {
                        session = null;
                    }
                }
            } finally {
                repository = null;
                try {
                    engine.shutdown();
                } finally {
                    engine = null;
                }
            }
        }
    }

    // @Test
    // public void shouldHaveSession() {
    // assertThat(session, is(notNullValue()));
    // }

    @Test
    public void shouldBeAbleToRemoveNodeThatExists_Mode691() throws RepositoryException {
        // Create some content ...
        Node root = session.getRootNode();
        Node a = root.addNode("a");
        Node b = a.addNode("b");
        Node c = b.addNode("c");
        @SuppressWarnings( "unused" )
        Node d1 = c.addNode("d_one");
        @SuppressWarnings( "unused" )
        Node d2 = c.addNode("d_two");
        session.save();

        root = session.getRootNode();
        String pathToNode = "a/b";
        assertThat(root.hasNode(pathToNode), is(true));

        Node nodeToDelete = root.getNode(pathToNode);
        nodeToDelete.remove();
        session.save();

        root = session.getRootNode();
        assertThat(root.hasNode(pathToNode), is(false));
    }

    @Test
    public void shouldAllowCreatingWorkspaces() throws Exception {
        String workspaceName = "MyNewWorkspace";

        // Create a session ...
        Session jcrSession = repository.login();
        Workspace defaultWorkspace = jcrSession.getWorkspace();
        defaultWorkspace.createWorkspace(workspaceName);
        assertAccessibleWorkspace(defaultWorkspace, workspaceName);
        jcrSession.logout();

        Session session2 = repository.login(workspaceName);
        session2.logout();
    }

    @FixFor( "MODE-1066" )
    @Test
    public void shouldReimportContentThatWasJustDeletedInPriorSave() throws Exception {
        // Register the cars node type(s) ...
        registerNodeTypes("io/cars.cnd", session);

        // Create a node under which we'll import the content ...
        session.getRootNode().addNode("workArea");
        session.save();

        // Import some content ...
        InputStream stream = resourceStream("io/cars-system-view.xml");
        session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Delete the content ...
        session.getNode("/workArea/Cars").remove();
        session.save();

        // Import the same content ...
        stream = resourceStream("io/cars-system-view.xml");
        session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
    }

    @FixFor( "MODE-1066" )
    @Test
    public void shouldReimportContentThatWasJustDeletedInSameSave() throws Exception {
        // Register the cars node type(s) ...
        registerNodeTypes("io/cars.cnd", session);

        // Create a node under which we'll import the content ...
        session.getRootNode().addNode("workArea");
        session.save();

        for (int i = 0; i != 3; ++i) {
            // Import some content ...
            InputStream stream = resourceStream("io/cars-system-view.xml");
            session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

            // Delete the content ...
            session.getNode("/workArea/Cars").remove();
        }
        session.save();
    }

    @FixFor( "MODE-1066" )
    @Test
    public void shouldReimportContentWithUuidsThatWasJustDeletedInPriorSave() throws Exception {
        // Register the cars node type(s) ...
        registerNodeTypes("io/cars.cnd", session);

        // Create a node under which we'll import the content ...
        session.getRootNode().addNode("workArea");
        session.save();

        // Import some content ...
        InputStream stream = resourceStream("io/cars-system-view-with-uuids.xml");
        session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Delete the content ...
        session.getNode("/workArea/Cars").remove();
        session.save();

        // Import the same content ...
        stream = resourceStream("io/cars-system-view-with-uuids.xml");
        session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
    }

    @FixFor( "MODE-1066" )
    @Test
    public void shouldReimportContentWithUuidsThatWasJustDeletedInSameSave() throws Exception {
        // Register the cars node type(s) ...
        registerNodeTypes("io/cars.cnd", session);

        // Create a node under which we'll import the content ...
        session.getRootNode().addNode("workArea");
        session.save();

        for (int i = 0; i != 3; ++i) {
            // Import some content ...
            InputStream stream = resourceStream("io/cars-system-view-with-uuids.xml");
            session.importXML("/workArea", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

            // Delete the content ...
            session.getNode("/workArea/Cars").remove();
        }
        session.save();
    }

    @FixFor( "MODE-1241" )
    @Test
    public void shouldBeAbleToCreateBinaryProperty() throws Exception {
        String fileMime = "application/octet-stream";
        GregorianCalendar lastModified = new GregorianCalendar(2010, 12, 2, 8, 30);

        Node root = session.getRootNode();
        Node file = root.addNode("createfile.mode", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");

        File f = new File("./src/test/resources/test.txt");

        if (!f.exists()) throw new Exception("File " + f.getAbsolutePath() + " is not found");
        System.out.println("FILE: " + f.getAbsolutePath());
        InputStream is = new FileInputStream(f);
        content.setProperty("jcr:data", session.getValueFactory().createBinary(is));
        content.setProperty("jcr:mimeType", fileMime);
        content.setProperty("jcr:encoding", "");
        content.setProperty("jcr:lastModified", lastModified);

        session.save();
    }

    @Test
    @FixFor("MODE-1421")
    public void removeItemAfterRegisteringSessionNamespace() throws Exception {
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("rh", "http://www.redhat.com");
        session.setNamespacePrefix("rh", "http://www.redhat.com");

        assertNotNull(namespaceRegistry.getURI("rh"));
        Node rootNode = session.getRootNode();
        rootNode.addNode("folder", "nt:folder");
        session.save();
        
        session.removeItem("/folder");
        session.save();
    }

    protected void registerNodeTypes( String pathToCndResourceFile,
                                      Session session ) throws IOException, RepositoryException {
        // Register the cars node type(s) ...
        CndNodeTypeReader reader = new CndNodeTypeReader(session);
        reader.read(pathToCndResourceFile);
        if (!reader.getProblems().isEmpty()) {
            // Report problems
            System.err.println(reader.getProblems());
            fail("Error loading the CND file at '" + pathToCndResourceFile + "'");
        } else {
            boolean allowUpdate = false;
            session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), allowUpdate);
        }

    }

    protected InputStream resourceStream( String resourcePath ) {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        return stream;
    }

    protected void assertAccessibleWorkspace( Session session,
                                              String workspaceName ) throws Exception {
        assertAccessibleWorkspace(session.getWorkspace(), workspaceName);
    }

    protected void assertAccessibleWorkspace( Workspace workspace,
                                              String workspaceName ) throws Exception {
        assertContains(workspace.getAccessibleWorkspaceNames(), workspaceName);
    }

    protected void assertContains( String[] actuals,
                                   String... expected ) {
        // Each expected must appear in the actuals ...
        for (String expect : expected) {
            if (expect == null) continue;
            boolean found = false;
            for (String actual : actuals) {
                if (expect.equals(actual)) {
                    found = true;
                    break;
                }
            }
            assertThat("Did not find '" + expect + "' in the actuals: " + actuals, found, is(true));
        }
    }
}
