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

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.modeshape.common.FixFor;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.api.AnonymousCredentials;
import org.modeshape.jcr.value.Path;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JcrSessionTest extends SingleUseAbstractTest {

    private static final String MULTI_LINE_VALUE = "Line\t1\nLine 2\rLine 3\r\nLine 4";

    protected void initializeData() throws Exception {
        Node root = session.getRootNode();
        Node a = root.addNode("a");
        Node b = a.addNode("b");
        Node c = b.addNode("c");
        a.addMixin("mix:lockable");
        a.setProperty("stringProperty", "value");

        b.addMixin("mix:referenceable");
        b.setProperty("booleanProperty", true);

        c.setProperty("stringProperty", "value");
        c.setProperty("multiLineProperty", MULTI_LINE_VALUE);
        session.save();
    }

    @Test
    public void shouldHaveRootNode() throws Exception {
        JcrRootNode node = session.getRootNode();
        assertThat(node, is(notNullValue()));
        assertThat(node.getPath(), is("/"));
    }

    @Test
    public void shouldHaveJcrSystemNodeUnderRoot() throws Exception {
        JcrRootNode node = session.getRootNode();
        Node system = node.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
        assertThat(system.getPath(), is("/jcr:system"));
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithSameNameSiblings() throws Exception {
        JcrRootNode node = session.getRootNode();
        int count = 10000;
        long start1 = System.nanoTime();
        for (int i = 0; i != count; ++i) {
            node.addNode("childNode");
        }
        long millis = TimeUnit.MILLISECONDS.convert(Math.abs(System.nanoTime() - start1), TimeUnit.NANOSECONDS);
        System.out.println("Time to create " + count + " nodes under root: " + millis + " ms");

        long start2 = System.nanoTime();
        session.save();
        millis = TimeUnit.MILLISECONDS.convert(Math.abs(System.nanoTime() - start2), TimeUnit.NANOSECONDS);
        System.out.println("Time to save " + count + " new nodes: " + millis + " ms");
        millis = TimeUnit.MILLISECONDS.convert(Math.abs(System.nanoTime() - start1), TimeUnit.NANOSECONDS);
        System.out.println("Total time to create " + count + " new nodes and save: " + millis + " ms");

        NodeIterator iter = node.getNodes("childNode");
        assertThat(iter.getSize(), is((long)count));
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            assertThat(child.getPrimaryNodeType().getName(), is("nt:unstructured"));
        }

        // Now add another node ...
        start1 = System.nanoTime();
        node.addNode("oneMore");
        session.save();
        millis = TimeUnit.MILLISECONDS.convert(Math.abs(System.nanoTime() - start1), TimeUnit.NANOSECONDS);
        System.out.println("Time to create " + (count + 1) + "th node and save: " + millis + " ms");
    }

    @Test
    public void shouldAllowCreatingNodeUnderUnsavedNode() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithNoSameNameSiblings() throws Exception {
        Stopwatch sw = new Stopwatch();
        for (int i = 0; i != 15; ++i) {
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            int count = 100;
            if (i > 2) {
                sw.start();
            }
            for (int j = 0; j != count; ++j) {
                node.addNode("childNode" + j);
            }

            session.save();
            if (i > 2) {
                sw.stop();
            }

            // Now add another node ...
            node.addNode("oneMore");
            session.save();

            session.getRootNode().getNode("testNode").remove();
            session.save();
        }
        System.out.println(sw.getDetailedStatistics());
    }

    @Test
    public void shouldAllowCreatingNodesTwoLevelsBelowRoot() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        session.save();
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowDeletingNodeWithNoChildren() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        session.save();
        // session.getRootNode().getNodes();
        // System.out.println("Root: " + session.getRootNode().getNodes().getSize() + " children");
        node.remove();
        session.save();
    }

    @Test
    public void shouldAllowDeletingTransientNodeWithNoChildren() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.remove();
        session.save();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowAddLockToken() throws Exception {
        session.addLockToken(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithNoPath() throws Exception {
        session.checkPermission((String)null, "read");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithEmptyPath() throws Exception {
        session.checkPermission("", "read");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithNoActions() throws Exception {
        session.checkPermission("/", null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithEmptyActions() throws Exception {
        session.checkPermission("/", "");
    }

    @Test
    public void shouldReturnNullValueForNullAttributeName() throws Exception {
        assertThat(session.getAttribute(null), nullValue());
    }

    @Test
    public void shouldReturnNullValueForEmptyOrBlankAttributeName() throws Exception {
        assertThat(session.getAttribute(""), nullValue());
        assertThat(session.getAttribute("  "), nullValue());
    }

    @Test
    public void shouldReturnNullValueForNonExistantAttributeName() throws Exception {
        assertThat(session.getAttribute("something else entirely"), nullValue());
    }

    @Test
    public void shouldReturnPropertyAttributeValueGivenNameOfExistingAttribute() throws Exception {
        session = repository.login(new AnonymousCredentials("attribute1", "value1"));
        assertThat(session.getAttribute("attribute1"), is((Object)"value1"));
    }

    @Test
    public void shouldProvideAttributeNames() throws Exception {
        session = repository.login(new AnonymousCredentials("attribute1", "value1"));
        String[] names = session.getAttributeNames();
        assertThat(names, notNullValue());
        assertThat(names.length, is(1));
        assertThat(names[0], is("attribute1"));
    }

    @Test
    public void shouldProvideEmptyAttributeNames() throws Exception {
        session = repository.login(new AnonymousCredentials());
        // Get get the attribute names (there should be none) ...
        String[] names = session.getAttributeNames();
        assertThat(names, notNullValue());
        assertThat(names.length, is(0));
    }

    @Test
    public void shouldProvideAccessToRepository() throws Exception {
        assertThat(session.getRepository(), is((Repository)repository));
    }

    @Test
    public void shouldProvideAccessToWorkspace() throws Exception {
        assertThat(session.getWorkspace(), notNullValue());
    }

    @Test
    public void shouldIndicateLiveBeforeLogout() throws Exception {
        assertThat(session.isLive(), is(true));
    }

    @Test
    public void shouldAllowLogout() throws Exception {
        session.logout();
    }

    @Test
    public void shouldIndicateNotLiveAfterLogout() throws Exception {
        session.logout();
        assertThat(session.isLive(), is(false));
    }

    @Test
    public void shouldProvideUserId() throws Exception {
        assertThat(session.getUserID(), notNullValue());
        try {
            assertThat(session.getUserID(), is("<anonymous>"));
        } finally {
            session.logout();
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldProvideRootNode() throws Exception {
        Node root = session.getRootNode();
        assertThat(root, notNullValue());
        String uuid = root.getIdentifier();
        assertThat(root.isNodeType("mix:referenceable"), is(true));
        assertThat(root.getUUID(), is(uuid));
        assertThat(uuid, notNullValue());
    }

    @Test
    public void shouldProvideChildrenByPath() throws Exception {
        initializeData();
        Item item = session.getItem("/a");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));
    }

    @Test
    public void shouldGetItemByIdentifierPath() throws Exception {
        initializeData();
        // Look up the node by the identifier path ...
        Item item = session.getItem(identifierPathFor("/a"));
        assertThat(item, instanceOf(Node.class));
        assertThat(item.getPath(), is("/a"));

        item = session.getItem(identifierPathFor("/a/b"));
        assertThat(item, instanceOf(Node.class));
        assertThat(item.getPath(), is("/a/b"));

        item = session.getItem(identifierPathFor("/"));
        assertThat(item, instanceOf(Node.class));
        assertThat(item.getPath(), is("/"));
    }

    @Test
    public void shouldGetNodeByIdentifierPath() throws Exception {
        initializeData();
        // Look up the node by the identifier path ...
        Node node = session.getNode(identifierPathFor("/a"));
        assertThat(node.getPath(), is("/a"));

        node = session.getNode(identifierPathFor("/a/b"));
        assertThat(node.getPath(), is("/a/b"));

        node = session.getNode(identifierPathFor("/"));
        assertThat(node.getPath(), is("/"));
    }

    @Test
    public void shouldCorrectlyDetermineIfItemExistsUsingPath() throws Exception {
        initializeData();
        assertThat(session.itemExists("/"), is(true));
        assertThat(session.itemExists("/a"), is(true));
        assertThat(session.itemExists("/a/b"), is(true));
    }

    @Test
    public void shouldCorrectlyDetermineIfItemExistsUsingIdentifierPath() throws Exception {
        initializeData();
        assertThat(session.itemExists(identifierPathFor("/")), is(true));
        assertThat(session.itemExists(identifierPathFor("/a")), is(true));
        assertThat(session.itemExists(identifierPathFor("/a/b")), is(true));
    }

    @Test
    public void shouldProvidePropertiesByPath() throws Exception {
        initializeData();
        Item item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));

        Property property = session.getProperty("/a/b/booleanProperty");
        assertThat(property, instanceOf(Property.class));
    }

    @Test
    public void shouldProvideNodesByPath() throws Exception {
        initializeData();
        Node node = session.getNode("/a");
        assertThat(node, instanceOf(Node.class));
        node = session.getNode("/a/b");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnPropertyAsNode() throws Exception {
        initializeData();
        assertThat(session.nodeExists("/a/b/booleanProperty"), is(false));
        session.getNode("/a/b/booleanProperty");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnNonExistantNode() throws Exception {
        initializeData();
        assertThat(session.nodeExists("/a/b/argleBargle"), is(false));
        session.getNode("/a/b/argleBargle");
    }

    @Test
    public void shoulReturnPropertyDoesExistAtPathForExistingProperty() throws Exception {
        initializeData();
        assertThat(session.propertyExists("/a/jcr:primaryType"), is(true));
        assertThat(session.propertyExists("/a/jcr:mixinTypes"), is(true));
        assertThat(session.propertyExists("/a/b/booleanProperty"), is(true));
        assertThat(session.getProperty("/a/b/booleanProperty"), is(notNullValue()));
    }

    @Test
    public void shoulReturnPropertyDoesNotExistAtPathForNode() throws Exception {
        initializeData();
        assertThat(session.propertyExists("/a/b"), is(false));
        try {
            assertThat(session.getProperty("/a/b"), is(notNullValue()));
            fail("Expected an exception");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    @Test
    public void shouldReturnNoPropertyExistsWhenPathIncludesNonExistantNode() throws Exception {
        initializeData();
        assertThat(session.propertyExists("/a/foo/bar/non-existant"), is(false));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnNonExistantProperty() throws Exception {
        initializeData();
        try {
            assertThat(session.propertyExists("/a/b/argleBargle"), is(false));
        } catch (RepositoryException e) {
            fail("Unexpected exception");
        }
        // This will throw a PathNotFoundException ...
        session.getProperty("/a/b/argleBargle");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldProvideValueFactory() throws Exception {
        InputStream stream = new ByteArrayInputStream("something".getBytes());
        ValueFactory factory = session.getValueFactory();
        Binary binary = factory.createBinary(new ByteArrayInputStream("something".getBytes()));
        assertThat(factory, notNullValue());
        assertThat(factory.createValue(false), notNullValue());
        assertThat(factory.createValue(Calendar.getInstance()), notNullValue());
        assertThat(factory.createValue(0.0), notNullValue());
        assertThat(factory.createValue(binary), notNullValue());
        assertThat(factory.createValue(stream), notNullValue());
        assertThat(factory.createValue(0L), notNullValue());

        Node node = session.getRootNode().addNode("testNode");
        node.addMixin(JcrMixLexicon.REFERENCEABLE.toString());

        assertThat(factory.createValue(node), notNullValue());
        assertThat(factory.createValue(""), notNullValue());
        assertThat(factory.createValue("", PropertyType.BINARY), notNullValue());
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = RepositoryException.class )
    public void shouldNotCreateValueForNonReferenceableNode() throws Exception {
        ValueFactory factory = session.getValueFactory();
        Node node = Mockito.mock(Node.class);
        String uuid = UUID.randomUUID().toString();
        when(node.getUUID()).thenReturn(uuid);
        when(node.getIdentifier()).thenReturn(uuid);
        when(node.isNodeType("mix:referenceable")).thenReturn(false);
        factory.createValue(node);
    }

    @Test
    public void shouldNotHavePendingChanges() throws Exception {
        assertThat(session.hasPendingChanges(), is(false));
    }

    @Test
    public void shouldProvideItemExists() throws Exception {
        initializeData();
        assertThat(session.itemExists("/a/b"), is(true));
        assertThat(session.itemExists("/a/c"), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowItemExistsWithNoPath() throws Exception {
        session.itemExists(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowItemExistsWithEmptyPath() throws Exception {
        session.itemExists("");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoNamespaceUri() throws Exception {
        session.getNamespacePrefix(null);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotProvidePrefixForUnknownUri() throws Exception {
        session.getNamespacePrefix("bogus");
    }

    @Test
    public void shouldProvideNamespacePrefix() throws Exception {
        assertThat(session.getNamespacePrefix("http://www.modeshape.org/1.0"), is("mode"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/1.0"), is("jcr"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/mix/1.0"), is("mix"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/nt/1.0"), is("nt"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/sv/1.0"), is("sv"));
        // assertThat(session.getNamespacePrefix("http://www.w3.org/XML/1998/namespace"), is("xml"));
    }

    @Test
    public void shouldProvideNamespacePrefixes() throws Exception {
        String[] prefixes = session.getNamespacePrefixes();
        assertThat(prefixes, notNullValue());
        assertThat(prefixes.length, is(not(0)));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoNamespacePrefix() throws Exception {
        session.getNamespaceURI(null);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotProvideUriForUnknownPrefix() throws Exception {
        session.getNamespaceURI("bogus");
    }

    @Test
    public void shouldProvideNamespaceUri() throws Exception {
        assertThat(session.getNamespaceURI("mode"), is("http://www.modeshape.org/1.0"));
        assertThat(session.getNamespaceURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(session.getNamespaceURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(session.getNamespaceURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(session.getNamespaceURI("sv"), is("http://www.jcp.org/jcr/sv/1.0"));
        // assertThat(session.getNamespaceURI("xml"), is("http://www.w3.org/XML/1998/namespace"));
    }

    /**
     * ModeShape JCR implementation is supposed to have root type named {@link ModeShapeLexicon#ROOT}.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void rootNodeShouldHaveProperType() throws Exception {
        Node rootNode = session.getRootNode();

        NodeType rootNodePrimaryType = rootNode.getPrimaryNodeType();
        NodeType dnaRootType = session.nodeTypeManager().getNodeType(ModeShapeLexicon.ROOT);

        assertThat(rootNodePrimaryType.getName(), is(dnaRootType.getName()));

    }

    /**
     * ModeShape JCR implementation is supposed to have a referenceable root.
     *
     * @throws RepositoryException if an error occurs during the test
     */
    @Test
    public void rootNodeShouldBeReferenceable() throws RepositoryException {
        Node rootNode = session.getRootNode();

        assertTrue(rootNode.getPrimaryNodeType().isNodeType(JcrMixLexicon.REFERENCEABLE.getString(session.namespaces())));
    }

    @Test
    public void shouldExportMultiLinePropertiesInSystemView() throws Exception {
        initializeData();

        OutputStream os = new ByteArrayOutputStream();
        session.exportSystemView("/a/b/c", os, false, true);

        String fileContents = os.toString();
        assertTrue(fileContents.contains(MULTI_LINE_VALUE));
    }

    @Test
    public void shouldUseJcrCardinalityPerPropertyDefinition() throws Exception {
        initializeData();

        // Verify that the node does exist in the source ...
        Path pathToNode = session.context().getValueFactories().getPathFactory().create("/a/b");
        Node carsNode = session.node(pathToNode);

        String mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(session.context().getNamespaceRegistry());
        Property mixinTypes = carsNode.getProperty(mixinTypesName);

        // Check that the JCR property is a MultiProperty - this call will throw an exception if the property is not.
        mixinTypes.getValues();
    }

    /*
     * Moved these three tests over from AbstractJcrNode as they require more extensive scaffolding that is already implemented in
     * this test.
     */

    @Test
    public void shouldProvideIdentifierEvenIfNotReferenceable() throws Exception {
        initializeData();
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        assertThat(node.getIdentifier(), is(notNullValue()));
    }

    @Test
    public void shouldProvideIdentifierEvenIfNoMixinTypes() throws Exception {
        initializeData();
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        assertThat(node.getIdentifier(), is(notNullValue()));
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNotReferenceable() throws Exception {
        initializeData();
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        node.getUUID();
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNoMixinTypes() throws Exception {
        initializeData();
        // The c node was not set up to be referenceable in this test and has no mixin types
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        node.getUUID();
    }

    @Test
    public void shouldMoveToNewName() throws Exception {
        initializeData();
        session.move("/a/b/c", "/a/b/d");

        session.getRootNode().getNode("a").getNode("b").getNode("d");
        try {
            session.getRootNode().getNode("a").getNode("b").getNode("c");

            fail("Node still exists at /a/b/c after move");
        } catch (PathNotFoundException e) {
            // Expected
        }
    }

    @FixFor( { "MODE-694", "MODE-1525" } )
    @Test
    public void shouldAddCreatedPropertyForHierarchyNodes() throws Exception {
        Node folderNode = session.getRootNode().addNode("folderNode", "nt:folder");
        assertThat(folderNode.hasProperty("jcr:created"), is(false));

        Node fileNode = folderNode.addNode("fileNode", "nt:file");
        Node resource = null;
        try {
            resource = fileNode.addNode("jcr:content");
            fail("Should not be able to add this child without specifying the primary type, as there is no default");
        } catch (ConstraintViolationException e) {
            resource = fileNode.addNode("jcr:content", "nt:resource");
        }
        assertThat(fileNode.hasProperty("jcr:created"), is(false));

        // Save the changes ...
        try {
            session.save();
            fail("Should not be able to save this; 'jcr:content' is missing the mandatory 'jcr:data' property");
        } catch (ConstraintViolationException e) {
            Binary binary = session.getValueFactory().createBinary("Some binary value".getBytes());
            resource.setProperty("jcr:data", binary);
            session.save();
        }

        assertThat(folderNode.hasProperty("jcr:created"), is(true));
        assertThat(fileNode.hasProperty("jcr:created"), is(true));
    }

    @Test
    public void shouldHaveCapabilityToPerformValidAddNode() throws Exception {
        assertTrue(session.hasCapability("addNode", session.getRootNode(), new String[] { "someNewNode" }));
        assertTrue(session.hasCapability("addNode", session.getRootNode(), new String[] { "someNewNode", "nt:unstructured" }));
    }

    @Test
    public void shouldNotHaveCapabilityToPerformInvalidAddNode() throws Exception {
        assertTrue(!session.hasCapability("addNode", session.getRootNode(), new String[] { "someNewNode[2]" }));
        assertTrue(!session.hasCapability("addNode", session.getRootNode(), new String[] { "someNewNode", "nt:invalidType" }));
    }

    @Test
    public void shouldCheckReferentialIntegrityWhenRemovingNodes() throws Exception {
        Node referenceableNode = session.getRootNode().addNode("referenceable");
        referenceableNode.addMixin(JcrMixLexicon.REFERENCEABLE.toString());

        Node node1 = session.getRootNode().addNode("node1");
        JcrValueFactory valueFactory = session.getValueFactory();
        node1.setProperty("ref1", valueFactory.createValue(referenceableNode, false));
        node1.setProperty("ref2", valueFactory.createValue(referenceableNode, false));
        node1.setProperty("wref1", valueFactory.createValue(referenceableNode, true));
        node1.setProperty("wref2", valueFactory.createValue(referenceableNode, true));

        session.save();

        // there are 2 strong refs
        referenceableNode.remove();
        expectReferentialIntegrityException();

        // remove the first strong ref
        node1.setProperty("ref1", (Node)null);
        referenceableNode.remove();
        expectReferentialIntegrityException();

        // remove the second strong ref (we should be able to remove the node now)
        assertEquals(2, referenceableNode.getWeakReferences().getSize());
        node1.setProperty("ref1", (Node)null);
        node1.setProperty("ref2", (Node)null);
        referenceableNode.remove();
        session.save();

        // check the node was actually deleted
        assertFalse(session.getRootNode().hasNode("referenceable"));
    }

    @Test
    @FixFor( "MODE-1613" )
    public void shouldMoveLotsOfSNS() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/transactional-repo-config.json"));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        int nodeCount = 100;

        session = repository.login();
        Node testRoot = session.getRootNode().addNode("testRoot");
        Node parentWithSNS = testRoot.addNode("parent");
        List<String> futureParentsIds = new ArrayList<String>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            futureParentsIds.add(testRoot.addNode("parent" + i).getIdentifier());
        }

        int iterationsCount = 5;
        for (int it = 0; it < iterationsCount; it++) {
//            System.out.println("Iteration " + it);

            List<String> childrenIDs = new ArrayList<String>(nodeCount);
            for (int i = 0; i < nodeCount; i++) {
                childrenIDs.add(parentWithSNS.addNode("child").getIdentifier());
            }
            session.save();

            for (int i = 0; i < nodeCount; i++) {
                String childId = childrenIDs.get(i);
                String futureParentId = futureParentsIds.get(i);

                MoveOperation task = new MoveOperation(childId, futureParentId);
                executorService.submit(task).get();
            }
        }
        executorService.shutdown();
    }

    private class MoveOperation implements Runnable {
        private String sourceId;
        private String targetId;

        private MoveOperation( String sourceId,
                               String targetId ) {
            this.sourceId = sourceId;
            this.targetId = targetId;
        }

        public void run() {
            try {
                JcrSession moveSession = repository.login();

                Node targetNode = moveSession.getNodeByIdentifier(targetId);
                String targetNodePath = targetNode.getPath() + "/";
                targetNode = moveSession.getNode(targetNodePath);

                Node sourceNode = moveSession.getNodeByIdentifier(sourceId);
                String sourceNodePath = sourceNode.getPath();
                sourceNode = moveSession.getNode(sourceNodePath);

                moveSession.move(sourceNodePath, targetNodePath);
                moveSession.save();
                moveSession.logout();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void expectReferentialIntegrityException() throws RepositoryException {
        try {
            session.save();
            fail("Expected a referential integrity exception");
        } catch (ReferentialIntegrityException e) {
            // expected
            session.refresh(false);
        }
    }

    @SuppressWarnings( "deprecation" )
    protected String identifierPathFor( String pathToNode ) throws Exception {
        AbstractJcrNode node = session.getNode(pathToNode);
        if (node.isNodeType("mix:referenceable")) {
            // Make sure that the identifier matches the UUID ...
            assertThat(node.getUUID(), is(node.getIdentifier()));
        } else {
            try {
                node.getUUID();
                fail("Should have thrown an UnsupportedRepositoryOperationException if the node " + pathToNode + " is not referenceable");
            } catch (UnsupportedRepositoryOperationException e) {
                // expected
            }
        }
        return node.identifierPath();
    }
}
