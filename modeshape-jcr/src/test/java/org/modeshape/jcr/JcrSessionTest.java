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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JaasSecurityContext;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * @author jverhaeg
 */
public class JcrSessionTest extends AbstractSessionTest {

    private static final String MULTI_LINE_VALUE = "Line\t1\nLine 2\rLine 3\r\nLine 4";

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

    }

    @Override
    protected void initializeContent() {
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.set("booleanProperty").on("/a/b").to(true);
        graph.set("stringProperty").on("/a/b/c").to("value");
        graph.set("jcr:mixinTypes").on("/a").to("mix:lockable");
        graph.set("jcr:mixinTypes").on("/a/b").to("mix:referenceable");
        graph.set("multiLineProperty").on("/a/b/c").to(MULTI_LINE_VALUE);

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/mode:namespaces").and();

    }

    @After
    public void after() throws Exception {
        if (session.isLive()) {
            session.logout();
        }
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
        assertThat(session.getAttribute("attribute1"), is((Object)"value1"));
    }

    @Test
    public void shouldProvideAttributeNames() throws Exception {
        String[] names = session.getAttributeNames();
        assertThat(names, notNullValue());
        assertThat(names.length, is(1));
        assertThat(names[0], is("attribute1"));
    }

    @Test
    public void shouldProvideEmptyAttributeNames() throws Exception {
        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();

        // Now create the workspace ...
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();

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
        Principal principal = Mockito.mock(Principal.class);
        when(principal.getName()).thenReturn("name");
        Subject subject = new Subject(false, Collections.singleton(principal), Collections.EMPTY_SET, Collections.EMPTY_SET);
        LoginContext loginContext = mock(LoginContext.class);
        when(loginContext.getSubject()).thenReturn(subject);
        NamespaceRegistry globalRegistry = context.getNamespaceRegistry();
        ExecutionContext sessionContext = context.with(new JaasSecurityContext(loginContext));
        Session session = new JcrSession(repository, workspace, sessionContext, globalRegistry, sessionAttributes);
        try {
            assertThat(session.getUserID(), is("name"));
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
        Item item = session.getItem("/a");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));
    }

    @Test
    public void shouldGetItemByIdentifierPath() throws Exception {
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
        assertThat(session.itemExists("/"), is(true));
        assertThat(session.itemExists("/a"), is(true));
        assertThat(session.itemExists("/a/b"), is(true));
    }

    @Test
    public void shouldCorrectlyDetermineIfItemExistsUsingIdentifierPath() throws Exception {
        assertThat(session.itemExists(identifierPathFor("/")), is(true));
        assertThat(session.itemExists(identifierPathFor("/a")), is(true));
        assertThat(session.itemExists(identifierPathFor("/a/b")), is(true));
    }

    @Test
    public void shouldProvidePropertiesByPath() throws Exception {
        Item item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));

        Property property = session.getProperty("/a/b/booleanProperty");
        assertThat(property, instanceOf(Property.class));
    }

    @Test
    public void shouldProvideNodesByPath() throws Exception {
        Node node = session.getNode("/a");
        assertThat(node, instanceOf(Node.class));
        node = session.getNode("/a/b");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnPropertyAsNode() throws Exception {
        assertThat(session.nodeExists("/a/b/booleanProperty"), is(false));
        session.getNode("/a/b/booleanProperty");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnNonExistantNode() throws Exception {
        assertThat(session.nodeExists("/a/b/argleBargle"), is(false));
        session.getNode("/a/b/argleBargle");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnNodeAsProperty() throws Exception {
        assertThat(session.propertyExists("/a/b"), is(false));
        session.getProperty("/a/b");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotReturnNonExistantProperty() throws Exception {
        assertThat(session.propertyExists("/a/b/argleBargle"), is(false));
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
        Node node = Mockito.mock(Node.class);
        String uuid = UUID.randomUUID().toString();
        when(node.getUUID()).thenReturn(uuid);
        when(node.getIdentifier()).thenReturn(uuid);
        when(node.isNodeType("mix:referenceable")).thenReturn(true);
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

        assertTrue(rootNode.getPrimaryNodeType()
                           .isNodeType(JcrMixLexicon.REFERENCEABLE.getString(context.getNamespaceRegistry())));
    }

    @Test
    public void shouldExportMultiLinePropertiesInSystemView() throws Exception {
        OutputStream os = new ByteArrayOutputStream();

        session.exportSystemView("/a/b/c", os, false, true);

        String fileContents = os.toString();
        assertTrue(fileContents.contains(MULTI_LINE_VALUE));
    }

    @Test
    public void shouldUseJcrCardinalityPerPropertyDefinition() throws Exception {

        // Verify that the node does exist in the source ...
        Path pathToNode = context.getValueFactories().getPathFactory().create("/a/b");
        Node carsNode = session.getNode(pathToNode);

        String mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(session.getExecutionContext().getNamespaceRegistry());
        Property mixinTypes = carsNode.getProperty(mixinTypesName);

        // Check that the underlying ModeShape property has one value - this is a test of the testing data
        assertThat(((AbstractJcrProperty)mixinTypes).property().isMultiple(), is(false));
        // Check that the JCR property is a MultiProperty - this call will throw an exception if the property is not.
        mixinTypes.getValues();

    }

    /*
     * Moved these three tests over from AbstractJcrNode as they require more extensive scaffolding that is already implemented in
     * this test.
     */

    @Test
    public void shouldProvideIdentifierEvenIfNotReferenceable() throws Exception {
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        assertThat(node.getIdentifier(), is(notNullValue()));
    }

    @Test
    public void shouldProvideIdentifierEvenIfNoMixinTypes() throws Exception {
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        assertThat(node.getIdentifier(), is(notNullValue()));
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNotReferenceable() throws Exception {
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        node.getUUID();
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNoMixinTypes() throws Exception {
        // The c node was not set up to be referenceable in this test and has no mixin types
        Node node = session.getRootNode().getNode("a").getNode("b").getNode("c");
        node.getUUID();
    }

    @Test
    public void shouldMoveToNewName() throws Exception {
        session.move("/a/b/c", "/a/b/d");

        session.getRootNode().getNode("a").getNode("b").getNode("d");
        try {
            session.getRootNode().getNode("a").getNode("b").getNode("c");

            fail("Node still exists at /a/b/c after move");
        } catch (PathNotFoundException e) {
            // Expected
        }
    }

    @Test
    public void shouldAddCreatedPropertyForHierarchyNodes() throws Exception {
        // q.v. MODE-694
        Node folderNode = session.getRootNode().addNode("folderNode", "nt:folder");
        assertThat(folderNode.hasProperty("jcr:created"), is(true));

        Node fileNode = folderNode.addNode("fileNode", "nt:file");
        assertThat(fileNode.hasProperty("jcr:created"), is(true));
    }

    @Test
    public void shouldHaveCapabilityToPerformValidAddNode() throws Exception {
        assertTrue(session.hasCapability("addNode", session.getRootNode(), new String[] {"someNewNode"}));
        assertTrue(session.hasCapability("addNode", session.getRootNode(), new String[] {"someNewNode", "nt:unstructured"}));
    }

    @Test
    public void shouldNotHaveCapabilityToPerformInvalidAddNode() throws Exception {
        assertTrue(!session.hasCapability("addNode", session.getRootNode(), new String[] {"someNewNode[2]"}));
        assertTrue(!session.hasCapability("addNode", session.getRootNode(), new String[] {"someNewNode", "nt:invalidType"}));
    }

}
