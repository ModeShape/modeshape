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
import static org.hamcrest.core.IsNull.notNullValue;
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
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.version.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * A series of integration tests of the shareable nodes feature.
 */
public class ShareableNodesTest {

    protected static final String CAR_CARRIER_TYPENAME = "car:Carrier";
    protected static final String CAR_TYPENAME = "car:Car";
    protected static final String MIX_SHAREABLE = "mix:shareable";
    protected static final String MIX_VERSIONABLE = "mix:versionable";
    protected static final String JCR_BASEVERSION = "jcr:baseVersion";

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

    /**
     * This test attempts to create a share underneath a node, A, that has a node type without a child definition for the
     * "mode:share" node type. This means that normally, a child of type "mode:share" cannot be placed under the node A. However,
     * because that node is only there as a proxy, ModeShape should transparently allow this.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldAllowCreatingShareUnderNodeWithTypeThatDoesNotAllowProxyNodeButAllowsPrimaryTypeOfOriginal()
        throws RepositoryException {
        // Register the "car:Carrier" node type ...
        registerCarCarrierNodeType(session);
        // And create a new node of this type ...
        Node myCarrier = session.getNode("/NewArea").addNode("MyCarrier", CAR_CARRIER_TYPENAME);
        // Make one of the cars shareable ...
        Node prius = session.getNode("/Cars/Hybrid/Toyota Prius");
        prius.addMixin(MIX_SHAREABLE);
        session.save();

        // Now create a share under the carrier ...
        String sharedPath = myCarrier.getPath() + "/The Prius";
        String originalPath = prius.getPath();
        Node sharedNode = makeShare(originalPath, sharedPath);
        assertSharedSetIncludes(prius, originalPath, sharedPath);
        assertSharedSetIncludes(sharedNode, originalPath, sharedPath);

        // Now, try refreshing the session so we have to re-materialize the node ...
        session.refresh(false);
        prius = session.getNode(prius.getPath());
        sharedNode = session.getNode(sharedNode.getPath());
        assertSharedSetIncludes(prius, originalPath, sharedPath);
        assertSharedSetIncludes(sharedNode, originalPath, sharedPath);
    }

    @SuppressWarnings( "unchecked" )
    @FixFor( "MODE-883" )
    @Test
    public void shouldAllowCreatingShareableNodeUnderParentThatDoesNotAllowSameNameSiblings() throws RepositoryException {
        // Now create a node type that allows only one car ...
        NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate template = ntManager.createNodeTypeTemplate();
        template.setName("car:Owner");
        NodeDefinitionTemplate childDefn = ntManager.createNodeDefinitionTemplate();
        childDefn.setSameNameSiblings(false);
        childDefn.setName("*");
        childDefn.setRequiredPrimaryTypeNames(new String[] {"car:Car"});
        template.getNodeDefinitionTemplates().add(childDefn);

        // Register the node type ...
        ntManager.registerNodeType(template, false);

        // Create two nodes with this node type ...
        Node joe = session.getNode("/NewSecondArea").addNode("Joe", "car:Owner");
        Node sally = session.getNode("/NewSecondArea").addNode("Sally", "car:Owner");
        session.save();

        // Create a node under Joe, since he will be the owner ...
        Node minibus = joe.addNode("Type 2", "car:Car");
        minibus.setProperty("car:maker", "Volkswagen");
        minibus.setProperty("car:year", "1952");
        minibus.addMixin("mix:shareable");
        session.save();

        // Share the minibus under sally ...
        String originalPath = minibus.getPath();
        String sharedPath = sally.getPath() + "/Our Bus";
        Node sharedNode = makeShare(originalPath, sharedPath);

        assertSharedSetIs(minibus, originalPath, sharedPath);
        assertSharedSetIs(sharedNode, originalPath, sharedPath);

        // Remove the node from Joe ..
        minibus.remove();
        session.save();
    }

    /**
     * This test attempts to verify that a user cannot explicitly use the "mode:share" node type as the primary type for a new
     * manually-created node.
     * 
     * @throws RepositoryException
     */
    @Test( expected = ConstraintViolationException.class )
    public void shouldNotBeAbleToCreateNodeWithProxyNodeTypeAsPrimaryType() throws RepositoryException {
        session.getRootNode().addNode("ShouldNotBePossible", string(ModeShapeLexicon.SHARE));
    }

    /**
     * This test attempts to verify that 'canAddNode()' returns false if using the "mode:share" node type as the primary type for
     * a new manually-created node.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldReturnFalseFromCanAddNodeIfUsingProxyNodeTypeAsPrimaryType() throws RepositoryException {
        boolean can = ((AbstractJcrNode)session.getRootNode()).canAddNode("ShouldNotBePossible", string(ModeShapeLexicon.SHARE));
        assertThat(can, is(false));
    }

    @Test
    public void shouldBeAbleToRegisterCarCarrierNodeType() throws RepositoryException {
        registerCarCarrierNodeType(session);
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

    @Test
    public void shouldAllowingCreatingShareOfVersionedNode() throws RepositoryException {
        String originalPath = "/Cars/Utility";
        // Make the original versionable ...
        Node original = makeVersionable(originalPath);
        session.save();
        Version version1 = checkin(originalPath);
        assertThat(version1, is(notNullValue()));
        Node baseVersion = findBaseVersion(originalPath);
        System.out.println("original     => " + original);
        System.out.println("baseVersion  => " + baseVersion);

        // Make the original a shareable node ...
        checkout(originalPath);
        String sharedPath = "/NewArea/SharedUtility";
        Node original2 = makeShareable(originalPath);
        session.save();
        Version version2 = checkin(originalPath);
        assertThat(version2, is(notNullValue()));
        Node baseVersion2 = findBaseVersion(original2);
        System.out.println("original2    => " + original2);
        System.out.println("baseVersion2 => " + baseVersion2);

        // Now create the share ...
        Node sharedNode = makeShare(originalPath, sharedPath);
        assertSharedSetIs(original2, originalPath, sharedPath);
        assertSharedSetIs(sharedNode, originalPath, sharedPath);
        System.out.println("sharedNode => " + sharedNode);

        // Now copy a subgraph that contains the shared area ...
        session.getWorkspace().copy("/NewArea", "/OtherNewArea");
        Node baseVersion3 = findBaseVersion(originalPath);
        System.out.println("baseVersion3 => " + baseVersion3);
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

    protected Node makeVersionable( String absPath ) throws RepositoryException {
        Node node = session.getNode(absPath);
        if (!node.isNodeType(MIX_VERSIONABLE)) {
            node.addMixin(MIX_VERSIONABLE);
        }
        return node;
    }

    protected Version checkin( String absPath ) throws RepositoryException {
        return session.getWorkspace().getVersionManager().checkin(absPath);
    }

    protected void checkout( String absPath ) throws RepositoryException {
        session.getWorkspace().getVersionManager().checkout(absPath);
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

    protected Node findBaseVersion( Node node ) throws RepositoryException {
        String baseVersionUuid = node.getProperty(JCR_BASEVERSION).getString();
        return session.getNodeByIdentifier(baseVersionUuid);
    }

    protected Node findBaseVersion( String path ) throws RepositoryException {
        Node node = session.getNode(path);
        return findBaseVersion(node);
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

    @SuppressWarnings( "unchecked" )
    protected void registerCarCarrierNodeType( Session session ) throws RepositoryException {
        NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();
        try {
            ntManager.getNodeType(CAR_CARRIER_TYPENAME);
        } catch (NoSuchNodeTypeException e) {
            NodeTypeTemplate nt = ntManager.createNodeTypeTemplate();
            nt.setName(CAR_CARRIER_TYPENAME);
            // Children ...
            NodeDefinitionTemplate carChildType = ntManager.createNodeDefinitionTemplate();
            carChildType.setRequiredPrimaryTypeNames(new String[] {CAR_TYPENAME});
            nt.getNodeDefinitionTemplates().add(carChildType);
            ntManager.registerNodeType(nt, true);
        }
        // Verify it was registered ...
        ntManager.getNodeType(CAR_CARRIER_TYPENAME);
    }

}
