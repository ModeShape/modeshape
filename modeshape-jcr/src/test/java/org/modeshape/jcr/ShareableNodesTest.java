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
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * A series of integration tests of the shareable nodes feature.
 */
public class ShareableNodesTest {

    protected static final String MIX_SHAREABLE = "mix:shareable";

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static String[] carColumnNames() {
        return new String[] {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine", "car:mpgHighway",
            "car:valueRating", "jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model", "car:msrp", "jcr:created",
            "jcr:createdBy"};
    }

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;
    private Workspace workspace;
    private JcrRepository repository2;
    private Session session2;
    private Workspace workspace2;

    @Before
    public void beforeEach() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repositorySource("import-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The source used to import content");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     // Added ADMIN privilege to allow permanent namespace registration in one of the tests
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        configuration.repository("import-repo").setSource("import-source").addNodeTypes(resourceUrl("cars.cnd"))
        // Added ADMIN privilege to allow permanent namespace registration in one of the tests
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);

        engine = configuration.build();
        engine.start();

        // Start the repository ...
        repository = engine.getRepository("cars");

        // Use a session to load the contents ...
        session = repository.login();
        try {
            InputStream stream = resourceStream("io/cars-system-view.xml");
            try {
                session.getWorkspace().importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                stream.close();
            }

            session.getRootNode().addNode("NewArea");
            session.getRootNode().addNode("NewSecondArea");
            session.save();
        } finally {
            session.logout();
        }

        // Obtain a session using the anonymous login capability, which we granted READ privilege
        session = repository.login();
        workspace = session.getWorkspace();

        // Start the import repository ...
        repository2 = engine.getRepository("import-repo");
        session2 = repository2.login();
        workspace2 = session2.getWorkspace();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
                workspace = null;
                repository = null;
                try {
                    session2.logout();
                } finally {
                    session2 = null;
                    workspace2 = null;
                    repository2 = null;
                    try {
                        engine.shutdown();
                        engine.awaitTermination(3, TimeUnit.SECONDS);
                    } finally {
                        configuration = null;
                        engine = null;
                    }
                }
            }
        }
    }

    /**
     * Verify that it is possible to create a new shareable node and then clone it to create a shared node and a shared set of
     * exactly one node.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldCreateOneSharedNode() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        // Make the original a shareable node ...
        Node original = makeShareable(originalPath);
        session.save();
        // Now create the share ...
        Node sharedNode = makeShare(originalPath, sharedPath);
        assertSharedSetIs(original, originalPath, sharedPath);
        assertSharedSetIs(sharedNode, originalPath, sharedPath);
    }

    /**
     * Verify that it is possible to create a new shareable node and then clone it to create several shared nodes and a shared set
     * of more than one node.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldCreateMultipleSharedNode() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        // Make the original a shareable node ...
        Node original = makeShareable(originalPath);
        session.save();
        // Now a share ...
        Node sharedNode1 = makeShare(originalPath, sharedPath);
        assertSharedSetIs(original, originalPath, sharedPath);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath);
        // Create another share ...
        String sharedPath2 = "/NewArea/SharedUtility[2]";
        Node sharedNode2 = makeShare(originalPath, sharedPath2);
        assertSharedSetIs(original, originalPath, sharedPath, sharedPath2);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath, sharedPath2);
        assertSharedSetIs(sharedNode2, originalPath, sharedPath, sharedPath2);
        // Create another share ...
        String sharedPath3 = "/NewSecondArea/SharedUtility";
        Node sharedNode3 = makeShare(originalPath, sharedPath3);
        assertSharedSetIs(original, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode2, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode3, originalPath, sharedPath, sharedPath2, sharedPath3);
    }

    /**
     * Verify that it is possible to move a (proxy) node in a shared set.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldAllowingMovingSharedNode() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        // Make the original a shareable node ...
        Node original = makeShareable(originalPath);
        session.save();
        // Now create the share ...
        Node sharedNode = makeShare(originalPath, sharedPath);
        assertSharedSetIncludes(original, originalPath, sharedPath);
        assertSharedSetIncludes(sharedNode, originalPath, sharedPath);
        // Now move the new shared node ...
        String newPath = "/NewSecondArea/SharedUtility"; // no index
        session.move(sharedPath, newPath);
        session.save();
        // Verify ...
        Node newSharedNode = session.getNode(newPath);
        assertSharedSetIs(original, originalPath, newPath);
        assertSharedSetIs(newSharedNode, originalPath, newPath);
        verifyShare(original, newSharedNode);

        // Verify that the old node and new node have the same location ...
        assertThat(sharedNode.getPath(), is(newSharedNode.getPath()));
    }

    /**
     * Verify that it is possible to copy a (proxy) node in a shared set.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldAllowingCopyingSharedNode() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        // Make the original a shareable node ...
        Node original = makeShareable(originalPath);
        session.save();
        // Now create the share ...
        Node sharedNode = makeShare(originalPath, sharedPath);
        assertSharedSetIncludes(original, originalPath, sharedPath);
        assertSharedSetIncludes(sharedNode, originalPath, sharedPath);
        // Now move the new shared node ...
        workspace.copy("/NewArea", "/NewSecondArea/NewArea");

        // Verify ...
        String copiedSharedPath = "/NewSecondArea" + sharedPath;
        session.refresh(false);
        Node node = session.getNode(copiedSharedPath);
        assertSharedSetIncludes(original, originalPath, sharedPath, copiedSharedPath);
        assertSharedSetIncludes(sharedNode, originalPath, sharedPath, copiedSharedPath);
        assertSharedSetIncludes(node, originalPath, sharedPath, copiedSharedPath);
        verifyShare(original, node);
    }

    @Test
    public void shouldExportSharedNodesAsSystemViewXml() throws RepositoryException, IOException {
        createExportableContent();
        // Export the content ...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        session.exportSystemView("/", baos, false, false);
        // System.out.println(baos);
        // Now import the content ...
        session2.importXML("/", new ByteArrayInputStream(baos.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session2.save();
        checkImportedContent(session2);
    }

    @Test
    public void shouldExportSharedNodesAsDocumentViewXml() throws RepositoryException, IOException {
        createExportableContent();
        // Export the content ...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        session.exportDocumentView("/", baos, false, false);
        // System.out.println(baos);
        // Now import the content ...
        session2.importXML("/", new ByteArrayInputStream(baos.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session2.save();
        checkImportedContent(session2);
    }

    protected void createExportableContent() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        // Make the original a shareable node ...
        Node original = makeShareable(originalPath);
        session.save();
        // Now a share ...
        Node sharedNode1 = makeShare(originalPath, sharedPath);
        assertSharedSetIs(original, originalPath, sharedPath);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath);
        // Create another share ...
        String sharedPath2 = "/NewArea/SharedUtility[2]";
        Node sharedNode2 = makeShare(originalPath, sharedPath2);
        assertSharedSetIs(original, originalPath, sharedPath, sharedPath2);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath, sharedPath2);
        assertSharedSetIs(sharedNode2, originalPath, sharedPath, sharedPath2);
        // Create another share ...
        String sharedPath3 = "/NewSecondArea/SharedUtility";
        Node sharedNode3 = makeShare(originalPath, sharedPath3);
        assertSharedSetIs(original, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode2, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode3, originalPath, sharedPath, sharedPath2, sharedPath3);
    }

    protected void checkImportedContent( Session session ) throws RepositoryException {
        String originalPath = "/Cars/Utility";
        String sharedPath = "/NewArea/SharedUtility";
        String sharedPath2 = "/NewArea/SharedUtility[2]";
        String sharedPath3 = "/NewSecondArea/SharedUtility";
        Node original = session.getNode(originalPath);
        Node sharedNode1 = session.getNode(sharedPath);
        Node sharedNode2 = session.getNode(sharedPath2);
        Node sharedNode3 = session.getNode(sharedPath3);
        assertSharedSetIs(original, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode1, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode2, originalPath, sharedPath, sharedPath2, sharedPath3);
        assertSharedSetIs(sharedNode3, originalPath, sharedPath, sharedPath2, sharedPath3);
    }

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String string( Object object ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(object);
    }

    protected Node makeShareable( String absPath ) throws RepositoryException {
        Node node = session.getNode(absPath);
        if (!node.isNodeType(MIX_SHAREABLE)) {
            node.addMixin(MIX_SHAREABLE);
        }
        return node;
    }

    protected Node makeShare( String sourcePath,
                              String newSharePath ) throws RepositoryException {
        // Make sure the source node exists ...
        Node original = session.getNode(sourcePath);
        // It is expected that a node does not exist at the supplied path, so verify this...
        boolean exists = session.nodeExists(newSharePath);

        // Then this call will create a shared node at this exact path ...
        workspace.clone(workspace.getName(), sourcePath, newSharePath, false);
        // If we've succeeded in the creation of the share, we can make sure that a node did
        // not already exist (otherwise the call should have then failed) ...
        assertThat(exists, is(false));
        // Now look up the new share node ...
        Node node = session.getNode(newSharePath);

        // And verify that this node has the same path and name ...
        assertThat(node.getPath(), is(newSharePath));
        assertThat(node.getName(), is(string(path(newSharePath).getLastSegment().getName())));
        assertThat(node.getIndex(), is(path(newSharePath).getLastSegment().getIndex()));

        // But that the identity, properties and children match the original node ...
        verifyShare(original, node);

        return node;
    }

    protected void verifyShare( Node original,
                                Node sharedNode ) throws RepositoryException {
        // The identity, properties and children match the original node ...
        assertThat(sharedNode.getIdentifier(), is(original.getIdentifier()));
        assertThat(sharedNode.isSame(original), is(true));
        assertSameProperties(sharedNode, original);
        assertSameChildren(sharedNode, original);

        // Verify the shared attributes ...
        assertThat(sharedNode.isNodeType(MIX_SHAREABLE), is(true));
        assertSharedSetIncludes(original, original.getPath(), sharedNode.getPath());
        assertSharedSetIncludes(sharedNode, original.getPath(), sharedNode.getPath());
    }

    protected void assertSharedSetIs( Node node,
                                      String... paths ) throws RepositoryException {
        Set<String> pathsInShare = sharedSetPathsFor(node);
        for (String path : paths) {
            pathsInShare.remove(path);
        }
        assertThat(pathsInShare.isEmpty(), is(true));
    }

    protected void assertSharedSetIncludes( Node node,
                                            String... paths ) throws RepositoryException {
        Set<String> pathsInShare = sharedSetPathsFor(node);
        for (String path : paths) {
            pathsInShare.remove(path);
        }
    }

    protected Set<String> sharedSetPathsFor( Node node ) throws RepositoryException {
        Set<String> paths = new HashSet<String>();
        for (NodeIterator iter = node.getSharedSet(); iter.hasNext();) {
            Node nodeInShare = iter.nextNode();
            paths.add(nodeInShare.getPath());
        }
        return paths;
    }

    protected void assertSameProperties( Node share,
                                         Node original ) throws RepositoryException {
        Set<String> originalPropertyNames = new HashSet<String>();
        for (PropertyIterator iter = original.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            originalPropertyNames.add(property.getName());
        }
        for (PropertyIterator iter = share.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            Property originalProperty = original.getProperty(property.getName());
            originalPropertyNames.remove(property.getName());
            assertThat(property.isModified(), is(originalProperty.isModified()));
            assertThat(property.isMultiple(), is(originalProperty.isMultiple()));
            assertThat(property.isNew(), is(originalProperty.isNew()));
            assertThat(property.isNode(), is(originalProperty.isNode()));
            assertThat(property.isSame(originalProperty), is(true)); // not the same property owner instance, but isSame()
            if (property.isMultiple()) {
                Value[] values = property.getValues();
                Value[] originalValues = originalProperty.getValues();
                assertThat(values.length, is(originalValues.length));
                for (int i = 0; i != values.length; ++i) {
                    assertThat(values[i].equals(originalValues[i]), is(true));
                }
            } else {
                assertThat(property.getValue(), is(originalProperty.getValue()));
            }
        }
        assertThat("Extra properties in original: " + originalPropertyNames, originalPropertyNames.isEmpty(), is(true));
    }

    protected void assertSameChildren( Node share,
                                       Node original ) throws RepositoryException {
        Set<String> originalChildNames = new HashSet<String>();
        for (NodeIterator iter = original.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            originalChildNames.add(node.getName());
        }
        for (NodeIterator iter = share.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            Node originalChild = original.getNode(child.getName());
            originalChildNames.remove(child.getName());
            assertThat(child.isSame(originalChild), is(true)); // Should be the exact same child nodes
        }
        assertThat("Extra children in original: " + originalChildNames, originalChildNames.isEmpty(), is(true));
    }

}
