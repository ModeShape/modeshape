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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.MockSecurityContext;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.MockObservable;

/**
 * Tests of round-trip importing/exporting of repository content.
 */
public class ImportExportTest {

    private enum ExportType {
        SYSTEM,
        DOCUMENT
    }

    private static final String BAD_CHARACTER_STRING = "Test & <Test>*";

    private InMemoryRepositorySource source;
    private JcrSession session;
    private JcrRepository repository;
    @SuppressWarnings( "unused" )
    private JcrTools tools;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.tools = new JcrTools();

        String workspaceName = "workspace1";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        ExecutionContext context = new ExecutionContext();
        // Register the test namespace
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        // Stub out the connection factory ...
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return source.getConnection();
            }
        };

        repository = new JcrRepository(context, connectionFactory, "unused", new MockObservable(), null, null, null, null);

        SecurityContext mockSecurityContext = new MockSecurityContext("testuser", Collections.singleton(ModeShapeRoles.ADMIN));
        session = (JcrSession)repository.login(new JcrSecurityContextCredentials(mockSecurityContext));
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    private void testImportExport( String sourcePath,
                                   String targetPath,
                                   ExportType useSystemView,
                                   boolean skipBinary,
                                   boolean noRecurse,
                                   boolean useWorkspace ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (useSystemView == ExportType.SYSTEM) {
            session.exportSystemView(sourcePath, baos, skipBinary, noRecurse);
        } else {
            session.exportDocumentView(sourcePath, baos, skipBinary, noRecurse);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        if (useWorkspace) {
            // import via workspace ...
            session.getWorkspace().importXML(targetPath, bais, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } else {
            // import via session ...
            session.importXML(targetPath, bais, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }
    }

    @Test
    public void shouldImportExportEscapedXmlCharactersInSystemViewUsingSession() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.SYSTEM, false, false, false);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    @Test
    public void shouldImportExportEscapedXmlCharactersInSystemViewUsingWorkspace() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);
        session.save();

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.SYSTEM, false, false, true);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    @Ignore( "JR TCK is broken" )
    @Test
    public void shouldImportExportEscapedXmlCharactersInDocumentViewUsingSession() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.DOCUMENT, false, false, false);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    protected void importFile( String importIntoPath,
                               String resourceName,
                               int importBehavior ) throws Exception {
        // Import the car content ...
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        try {
            session.importXML(importIntoPath, stream, importBehavior); // shouldn't exist yet
        } finally {
            stream.close();
        }
    }

    protected Node assertNode( String path ) throws Exception {
        Node node = session.getNode(path);
        assertThat(node, is(notNullValue()));
        return node;
    }

    protected void assertNoNode( String path ) throws Exception {
        try {
            session.getNode(path);
            fail("Did not expect to find node at \"" + path + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    @Test
    public void shouldImportSystemViewWithUuidsAfterNodesWithSameUuidsAreDeletedInSessionAndSaved() throws Exception {
        // Register the Cars node types ...
        CndNodeTypeReader reader = new CndNodeTypeReader(session);
        reader.read("cars.cnd");
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), true);

        // Create the node under which the content will be imported ...
        session.getRootNode().addNode("/someNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
        // tools.printSubgraph(assertNode("/"));

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();
        session.save();

        // Now import again ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
    }

    @Test
    public void shouldImportSystemViewWithUuidsAfterNodesWithSameUuidsAreDeletedInSessionButNotSaved() throws Exception {
        // Register the Cars node types ...
        CndNodeTypeReader reader = new CndNodeTypeReader(session);
        reader.read("cars.cnd");
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), true);

        // Create the node under which the content will be imported ...
        session.getRootNode().addNode("/someNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();
        // session.save(); // DO NOT SAVE

        // Now import again ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
    }

    @Test
    public void shouldImportSystemViewWithUuidsIntoDifferentSpotAfterNodesWithSameUuidsAreDeletedInSessionButNotSaved()
        throws Exception {
        // Register the Cars node types ...
        CndNodeTypeReader reader = new CndNodeTypeReader(session);
        reader.read("cars.cnd");
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), true);

        // Create the node under which the content will be imported ...
        Node someNode = session.getRootNode().addNode("/someNode");
        session.getRootNode().addNode("/otherNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();

        // Now create a node at the same spot as cars, but with a different UUID ...
        Node newCars = someNode.addNode("Cars");
        assertThat(newCars.getIdentifier(), is(not("e41075cb-a09a-4910-87b1-90ce8b4ca9dd")));

        // session.save(); // DO NOT SAVE

        // Now import again ...
        importFile("/otherNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/otherNode/Cars");
        assertThat(cars.getIdentifier(), is("e41075cb-a09a-4910-87b1-90ce8b4ca9dd"));

        // Make sure some duplicate nodes didn't show up ...
        assertNoNode("/sameNode/Cars[2]");
        assertNoNode("/sameNode[2]");
        assertNoNode("/otherNode/Cars[2]");
        assertNoNode("/otherNode[2]");
    }
}
