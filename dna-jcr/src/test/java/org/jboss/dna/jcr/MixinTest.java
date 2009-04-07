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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.basic.BasicName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

public class MixinTest {

    /*
     * Declares the following node types:
     * mixinTypeA with child node nodeA and property propertyA
     * mixinTypeB with child node nodeB and property propertyB
     * mixinTypeC with child node nodeA and property propertyB
     * mixinTypeWithAutoCreatedProperty with auto-created property propertyB
     * mixinTypeWithAutoCreatedChildNode with auto-created child node nodeB
     * mixinTypeC with child node nodeA and property propertyB
     * primaryTypeA with child node nodeA and property propertyA
     */
    static final Name MIXIN_TYPE_A = new BasicName("", "mixinTypeA");
    static final Name MIXIN_TYPE_B = new BasicName("", "mixinTypeB");
    static final Name MIXIN_TYPE_C = new BasicName("", "mixinTypeC");
    static final Name MIXIN_TYPE_WITH_AUTO_PROP = new BasicName("", "mixinTypeWithAutoCreatedProperty");
    static final Name MIXIN_TYPE_WITH_AUTO_CHILD = new BasicName("", "mixinTypeWithAutoCreatedChildNode");
    static final Name PRIMARY_TYPE_A = new BasicName("", "mixinTypeA");

    static final Name PROPERTY_A = new BasicName("", "propertyA");
    static final Name PROPERTY_B = new BasicName("", "propertyB");

    static final Name CHILD_NODE_A = new BasicName("", "nodeA");
    static final Name CHILD_NODE_B = new BasicName("", "nodeB");

    private String workspaceName;
    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private JcrSession session;
    private Graph graph;
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryNodeTypeManager repoTypeManager;
    private Map<String, Object> sessionAttributes;
    private Map<JcrRepository.Options, String> options;
    private NamespaceRegistry registry;
    @Mock
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspaceName = "workspace1";
        final String repositorySourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();
        // Register the test namespace
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        // Set up the initial content ...
        graph = Graph.create(source, context);

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system"); // .and().create("/jcr:system/dna:namespaces");
        graph.set("jcr:primaryType").on("/jcr:system").to(DnaLexicon.SYSTEM);

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

        // Stub out the repository, since we only need a few methods ...
        JcrNodeTypeSource source = null;
        source = new JcrBuiltinNodeTypeSource(this.context, source);
        source = new DnaBuiltinNodeTypeSource(this.context, source);
        source = new TestNodeTypeSource(this.context, source);
        repoTypeManager = new RepositoryNodeTypeManager(context, source);
        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        // Stub out the repository options ...
        options = new EnumMap<JcrRepository.Options, String>(JcrRepository.Options.class);
        options.put(JcrRepository.Options.PROJECT_NODE_TYPES, Boolean.FALSE.toString());
        stub(repository.getOptions()).toReturn(options);

        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();

        // Now create the workspace ...
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();
        registry = session.getExecutionContext().getNamespaceRegistry();
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullMixinTypeName() throws Exception {
        graph.create("/a");
        graph.set("jcr:primaryType").on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyMixinTypeName() throws Exception {
        graph.create("/a");
        graph.set("jcr:primaryType").on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("");
    }

    @Test( expected = NoSuchNodeTypeException.class )
    public void shouldNotAllowInvalidMixinTypeName() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("foo");
    }

    @Test
    public void shouldNotAllowAddingMixinToProtectedNodes() throws Exception {
        Node rootNode = session.getRootNode();
        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));

        assertThat(systemNode.getDefinition().isProtected(), is(true));
        assertThat(systemNode.canAddMixin(JcrMixLexicon.VERSIONABLE.getString(registry)), is(false));
    }

    @Test
    public void shouldAllowAddingMixinIfNoConflict() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
    }

    @Test
    public void shouldNotAllowAddingMixinIfPrimaryTypeConflicts() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C.getString(registry)), is(false));
    }

    @Test
    public void shouldNotAllowAddingMixinIfMixinTypeConflicts() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C.getString(registry)), is(false));
    }

    @Test
    public void shouldAutoCreateAutoCreatedPropertiesOnAddition() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
        Property prop = nodeA.getProperty(PROPERTY_B.getString(registry));

        assertThat(prop, is(notNullValue()));
        assertThat(prop.getLong(), is(10L));
    }

    @Test
    public void shouldAutoCreateAutoCreatedChildNodesOnAddition() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
        Node childNode = nodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(childNode, is(notNullValue()));
        assertThat(childNode.getPrimaryNodeType().getName(), is(JcrNtLexicon.UNSTRUCTURED.getString(registry)));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualPropertyConflicts() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B.getString(registry), "Not a boolean");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
    }

    @Test
    public void shouldAllowAdditionIfResidualPropertyDoesNotConflict() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B.getString(registry), 10L);

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualChildNodeConflicts() throws Exception {
        graph.create("/a").and().create("/a/" + CHILD_NODE_B);
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a/" + CHILD_NODE_B).to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
    }

    @Test
    public void shouldAllowAdditionIfResidualChildNodeDoesNotConflict() throws Exception {
        graph.create("/a").and().create("/a/" + CHILD_NODE_B);
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a/" + CHILD_NODE_B).to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
        nodeA.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");

        Node newNodeB = newNodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(newNodeA.isNodeType(MIXIN_TYPE_WITH_AUTO_CHILD.getLocalName()), is(true));
        assertThat(newNodeB.isNodeType("nt:unstructured"), is(true));
        assertThat(newNodeB, is(notNullValue()));

        // Uncomment this to see the problem with the session cache not refreshing the immutable info for /a after a save
        // assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B.getString(registry)));

    }

    @Test
    public void shouldAllowSettingNewPropertyAfterAddingMixin() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_B.getString(registry));

        nodeA.setProperty(PROPERTY_B.getString(registry), "some string");
        nodeA.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");
        Property propB = nodeA.getProperty(PROPERTY_B.getString(registry));

        assertThat(propB, is(notNullValue()));
        assertThat(propB.getValue().getString(), is("some string"));
    }

    @Test
    public void shouldAllowAddingNewChildNodeAfterAddingMixin() throws Exception {
        graph.create("/a");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_B.getString(registry));

        nodeA.addNode(CHILD_NODE_B.getString(registry));
        nodeA.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");
        Node newNodeB = newNodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(newNodeB, is(notNullValue()));
        assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B.getString(registry)));
    }

    @Test
    public void shouldAllowRemovalIfNoConflict() throws Exception {
        graph.create("/a").and().create("/a/nodeB");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        nodeA.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test
    public void shouldAllowRemovalIfExistingPropertyWouldHaveDefinition() throws Exception {
        graph.create("/a").and().create("/a");
        graph.set(PROPERTY_B).on("/a").to("true");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        nodeA.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingPropertyWouldHaveNoDefinition() throws Exception {
        graph.create("/a").and().create("/a");
        graph.set(PROPERTY_B).on("/a").to("true");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        nodeA.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test
    public void shouldAllowRemovalIfExistingChildNodeWouldHaveDefinition() throws Exception {
        graph.create("/a").and().create("/a/nodeB");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingChildNodeWouldHaveNoDefinition() throws Exception {
        graph.create("/a").and().create("/a/nodeB");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
    }

    class TestNodeTypeSource extends AbstractJcrNodeTypeSource {

        /** The list of primary node types. */
        private final List<JcrNodeType> nodeTypes;

        TestNodeTypeSource( ExecutionContext context,
                            JcrNodeTypeSource predecessor ) {
            super(predecessor);

            nodeTypes = new ArrayList<JcrNodeType>();

            JcrNodeType base = findType(JcrNtLexicon.BASE);

            if (base == null) {
                String baseTypeName = JcrNtLexicon.BASE.getString(context.getNamespaceRegistry());
                String namespaceTypeName = DnaLexicon.NAMESPACE.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            JcrNodeType unstructured = findType(JcrNtLexicon.UNSTRUCTURED);

            if (unstructured == null) {
                String baseTypeName = JcrNtLexicon.UNSTRUCTURED.getString(context.getNamespaceRegistry());
                String namespaceTypeName = DnaLexicon.NAMESPACE.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            Value tenValue = new JcrValue(context.getValueFactories(), null, PropertyType.LONG, 10);

            // Stubbing in child node and property definitions for now
            JcrNodeType mixinTypeA = new JcrNodeType(
                                                     context,
                                                     NO_NODE_TYPE_MANAGER,
                                                     MIXIN_TYPE_A,
                                                     Arrays.asList(new JcrNodeType[] {base}),
                                                     NO_PRIMARY_ITEM_NAME,
                                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                  context,
                                                                                                                  null,
                                                                                                                  CHILD_NODE_A,
                                                                                                                  OnParentVersionAction.IGNORE,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  JcrNtLexicon.BASE,
                                                                                                                  new JcrNodeType[] {base}),}),
                                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                          context,
                                                                                                                          null,
                                                                                                                          PROPERTY_A,
                                                                                                                          OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          NO_DEFAULT_VALUES,
                                                                                                                          PropertyType.STRING,
                                                                                                                          NO_CONSTRAINTS,
                                                                                                                          false),}),
                                                     IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType mixinTypeB = new JcrNodeType(
                                                     context,
                                                     NO_NODE_TYPE_MANAGER,
                                                     MIXIN_TYPE_B,
                                                     Arrays.asList(new JcrNodeType[] {base}),
                                                     NO_PRIMARY_ITEM_NAME,
                                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                  context,
                                                                                                                  null,
                                                                                                                  CHILD_NODE_B,
                                                                                                                  OnParentVersionAction.IGNORE,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  JcrNtLexicon.BASE,
                                                                                                                  new JcrNodeType[] {base}),}),
                                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                          context,
                                                                                                                          null,
                                                                                                                          PROPERTY_B,
                                                                                                                          OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          NO_DEFAULT_VALUES,
                                                                                                                          PropertyType.BINARY,
                                                                                                                          NO_CONSTRAINTS,
                                                                                                                          false),}),
                                                     IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType mixinTypeC = new JcrNodeType(
                                                     context,
                                                     NO_NODE_TYPE_MANAGER,
                                                     MIXIN_TYPE_C,
                                                     Arrays.asList(new JcrNodeType[] {base}),
                                                     NO_PRIMARY_ITEM_NAME,
                                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                  context,
                                                                                                                  null,
                                                                                                                  CHILD_NODE_A,
                                                                                                                  OnParentVersionAction.IGNORE,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  JcrNtLexicon.BASE,
                                                                                                                  new JcrNodeType[] {base}),}),
                                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                          context,
                                                                                                                          null,
                                                                                                                          PROPERTY_B,
                                                                                                                          OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          false,
                                                                                                                          NO_DEFAULT_VALUES,
                                                                                                                          PropertyType.STRING,
                                                                                                                          NO_CONSTRAINTS,
                                                                                                                          false),}),
                                                     IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType mixinTypeWithAutoChild = new JcrNodeType(
                                                                 context,
                                                                 NO_NODE_TYPE_MANAGER,
                                                                 MIXIN_TYPE_WITH_AUTO_CHILD,
                                                                 Arrays.asList(new JcrNodeType[] {base}),
                                                                 NO_PRIMARY_ITEM_NAME,
                                                                 Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                              context,
                                                                                                                              null,
                                                                                                                              CHILD_NODE_B,
                                                                                                                              OnParentVersionAction.IGNORE,
                                                                                                                              true,
                                                                                                                              true,
                                                                                                                              false,
                                                                                                                              false,
                                                                                                                              JcrNtLexicon.UNSTRUCTURED,
                                                                                                                              new JcrNodeType[] {unstructured}),}),
                                                                 NO_PROPERTIES, IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType mixinTypeWithAutoProperty = new JcrNodeType(
                                                                    context,
                                                                    NO_NODE_TYPE_MANAGER,
                                                                    MIXIN_TYPE_WITH_AUTO_PROP,
                                                                    Arrays.asList(new JcrNodeType[] {base}),
                                                                    NO_PRIMARY_ITEM_NAME,
                                                                    NO_CHILD_NODES,
                                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                                         context,
                                                                                                                                         null,
                                                                                                                                         PROPERTY_B,
                                                                                                                                         OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                                         true,
                                                                                                                                         true,
                                                                                                                                         false,
                                                                                                                                         new Value[] {tenValue},
                                                                                                                                         PropertyType.LONG,
                                                                                                                                         NO_CONSTRAINTS,
                                                                                                                                         false),}),
                                                                    IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType primaryTypeA = new JcrNodeType(
                                                       context,
                                                       NO_NODE_TYPE_MANAGER,
                                                       PRIMARY_TYPE_A,
                                                       Arrays.asList(new JcrNodeType[] {base}),
                                                       NO_PRIMARY_ITEM_NAME,
                                                       Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                    context,
                                                                                                                    null,
                                                                                                                    CHILD_NODE_A,
                                                                                                                    OnParentVersionAction.IGNORE,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    JcrNtLexicon.BASE,
                                                                                                                    new JcrNodeType[] {base}),}),
                                                       Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                            context,
                                                                                                                            null,
                                                                                                                            PROPERTY_A,
                                                                                                                            OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            NO_DEFAULT_VALUES,
                                                                                                                            PropertyType.STRING,
                                                                                                                            NO_CONSTRAINTS,
                                                                                                                            false),}),
                                                       NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            nodeTypes.addAll(Arrays.asList(new JcrNodeType[] {mixinTypeA, mixinTypeB, mixinTypeC, mixinTypeWithAutoChild,
                mixinTypeWithAutoProperty, primaryTypeA,}));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.AbstractJcrNodeTypeSource#getDeclaredNodeTypes()
         */
        @Override
        public Collection<JcrNodeType> getDeclaredNodeTypes() {
            return nodeTypes;
        }

    }

}