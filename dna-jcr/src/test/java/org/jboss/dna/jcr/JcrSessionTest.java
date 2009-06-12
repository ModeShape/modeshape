/*
* JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JaasSecurityContext;
import org.jboss.dna.graph.MockSecurityContext;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrSessionTest {

    private static final String MULTI_LINE_VALUE = "Line\t1\nLine 2\rLine 3\r\nLine 4";

    private String workspaceName;
    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private JcrSession session;
    private Graph graph;
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryNodeTypeManager repoTypeManager;
    private Map<String, Object> sessionAttributes;
    @Mock
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        workspaceName = "workspace1";
        final String repositorySourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();

        // Set up the initial content ...
        graph = Graph.create(source, context);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.set("booleanProperty").on("/a/b").to(true);
        graph.set("stringProperty").on("/a/b/c").to("value");
        graph.set("jcr:mixinTypes").on("/a").to("mix:lockable");
        graph.set("jcr:mixinTypes").on("/a/b").to("mix:referenceable");
        graph.set("multiLineProperty").on("/a/b/c").to(MULTI_LINE_VALUE);

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return repositorySourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Set up the repo type manager
        repoTypeManager = new RepositoryNodeTypeManager(context);
        try {
            this.repoTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {"/org/jboss/dna/jcr/jsr_170_builtins.cnd",
                "/org/jboss/dna/jcr/dna_builtins.cnd"}));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

        // Stub out the repository, since we only need a few methods ...
        MockitoAnnotations.initMocks(this);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);
        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);

        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();
        sessionAttributes.put("attribute1", "value1");

        // Now create the workspace ...
        SecurityContext mockSecurityContext = new MockSecurityContext(null, Collections.singleton(JcrSession.DNA_WRITE_PERMISSION));
        workspace = new JcrWorkspace(repository, workspaceName, context.with(mockSecurityContext), sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();
    }

    @After
    public void after() throws Exception {
        if (session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullRepository() throws Exception {
        new JcrSession(null, workspace, context, sessionAttributes);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullWorkspace() throws Exception {
        new JcrSession(repository, null, context, sessionAttributes);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullExecutionContext() throws Exception {
        new JcrSession(repository, workspace, null, sessionAttributes);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullSessionAttributesMap() throws Exception {
        new JcrSession(repository, workspace, context, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddLockToken() throws Exception {
        session.addLockToken(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithNoPath() throws Exception {
        session.checkPermission((String) null, "read");
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
        assertThat(session.getUserID(), nullValue());
        Principal principal = Mockito.mock(Principal.class);
        stub(principal.getName()).toReturn("name");
        Subject subject = new Subject(false, Collections.singleton(principal), Collections.EMPTY_SET, Collections.EMPTY_SET);
        LoginContext loginContext = mock(LoginContext.class);
        stub(loginContext.getSubject()).toReturn(subject);
        Session session = new JcrSession(repository, workspace, context.with(new JaasSecurityContext(loginContext)), sessionAttributes);
        try {
            assertThat(session.getUserID(), is("name"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void shouldProvideRootNode() throws Exception {
        Node root = session.getRootNode();
        assertThat(root, notNullValue());
        UUID uuid = ((JcrRootNode)root).internalUuid();
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
    public void shouldProvidePropertiesByPath() throws Exception {
        Item item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));
    }

    @Test
    public void shouldProvideValueFactory() throws Exception {
        InputStream stream = new ByteArrayInputStream("something".getBytes());
        ValueFactory factory = session.getValueFactory();
        assertThat(factory, notNullValue());
        assertThat(factory.createValue(false), notNullValue());
        assertThat(factory.createValue(Calendar.getInstance()), notNullValue());
        assertThat(factory.createValue(0.0), notNullValue());
        assertThat(factory.createValue(stream), notNullValue());
        assertThat(factory.createValue(0L), notNullValue());
        Node node = Mockito.mock(Node.class);
        stub(node.getUUID()).toReturn(UUID.randomUUID().toString());
        stub(node.isNodeType("mix:referenceable")).toReturn(true);
        assertThat(factory.createValue(node), notNullValue());
        assertThat(factory.createValue(""), notNullValue());
        assertThat(factory.createValue("", PropertyType.BINARY), notNullValue());
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotCreateValueForNonReferenceableNode() throws Exception {
        ValueFactory factory = session.getValueFactory();
        Node node = Mockito.mock(Node.class);
        stub(node.getUUID()).toReturn(UUID.randomUUID().toString());
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
        assertThat(session.getNamespacePrefix("http://www.jboss.org/dna/1.0"), is("dna"));
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
        assertThat(session.getNamespaceURI("dna"), is("http://www.jboss.org/dna/1.0"));
        assertThat(session.getNamespaceURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(session.getNamespaceURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(session.getNamespaceURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(session.getNamespaceURI("sv"), is("http://www.jcp.org/jcr/sv/1.0"));
        // assertThat(session.getNamespaceURI("xml"), is("http://www.w3.org/XML/1998/namespace"));
    }

    /**
     * DNA JCR implementation is supposed to have root type named {@link DnaLexicon#ROOT}.
     * 
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void rootNodeShouldHaveProperType() throws Exception {
        Node rootNode = session.getRootNode();

        NodeType rootNodePrimaryType = rootNode.getPrimaryNodeType();
        NodeType dnaRootType = session.nodeTypeManager().getNodeType(DnaLexicon.ROOT);

        assertThat(rootNodePrimaryType.getName(), is(dnaRootType.getName()));

    }

    /**
     * DNA JCR implementation is supposed to have a referenceable root.
     * 
     * @throws RepositoryException if an error occurs during the test
     */
    @Test
    public void rootNodeShouldBeReferenceable() throws RepositoryException {
        Node rootNode = session.getRootNode();

        assertTrue(rootNode.getPrimaryNodeType().isNodeType(JcrMixLexicon.REFERENCEABLE.getString(context.getNamespaceRegistry())));
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

        // Check that the underlying DNA property has one value - this is a test of the testing data
        assertThat(((AbstractJcrProperty)mixinTypes).property().isMultiple(), is(false));
        // Check that the JCR property is a MultiProperty - this call will throw an exception if the property is not.
        mixinTypes.getValues();

    }

    /*
     * Moved these three tests over from AbstractJcrNode as they require more extensive scaffolding that is already implemented in
     * this test.
     */

    @Test
    public void shouldProvideUuidIfReferenceable() throws Exception {
        // The root node is referenceable in DNA
        Node rootNode = session.getRootNode();

        UUID uuid = ((AbstractJcrNode)rootNode).internalUuid();
        assertThat(rootNode.getUUID(), is(uuid.toString()));
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNotReferenceable() throws Exception {
        // The b node was not set up to be referenceable in this test, but does have a mixin type
        Node node = session.getRootNode().getNode("a");

        node.getUUID();
    }

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
}
