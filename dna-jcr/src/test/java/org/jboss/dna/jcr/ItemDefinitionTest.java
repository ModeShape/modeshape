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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.basic.BasicName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * BDD test cases for property and child node definition inheritance. Could be part of RepositoryNodeTypeManagerTest, but split
 * off to isolate tests for this behavior vs. projection/inference and registration/unregistration behavior.
 */
public class ItemDefinitionTest {

    private String workspaceName;
    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private JcrSession session;
    private Graph graph;
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryNodeTypeManager repoTypeManager;
    private Map<String, Object> sessionAttributes;
    private ValueFactory<Name> nameFactory;
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

        // Stub out the repository, since we only need a few methods ...
        JcrNodeTypeSource source = null;
        source = new JcrBuiltinNodeTypeSource(this.context, source);
        source = new DnaBuiltinNodeTypeSource(this.context, source);
        source = new TestNodeTypeSource(this.context, source);
        repoTypeManager = new RepositoryNodeTypeManager(context, source);
        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();

        // Now create the workspace ...
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();

        nameFactory = session.getExecutionContext().getValueFactories().getNameFactory();
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test
    public void shouldNotFindInvalidPropertyDefinition() throws Exception {
        // This property name is not defined for any of our test types
        Name badName = nameFactory.create("undefinedName");
        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_D, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));
    }

    @Test
    public void shouldUseNearestPropertyDefinition() {
        // If a property is defined at multiple points in the type hierarchy, the property definition closest to the given type
        // should be used.

        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.BOOLEAN);
    }

    @Test
    public void shouldPreferLeftmostSupertypeForDefinition() {
        /*
         * This specification doesn't mandate this, but since DNA supports multiple inheritance, we
         * have imposed the rule that property or child-node definitions should be preferred based on
         * distance from the given node type in the hierarchy with the order of the supertypes in the declaration
         * being the tiebreaker.
         */
        JcrPropertyDefinition propDef;

        // Should prefer the inherited definition from NODE_TYPE_A since it was declared before NODE_TYPE_C in NODE_TYPE_D's list
        // of supertypes
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_D,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

    }
    
    @Test
    public void shouldFindBestMatchDefinition() {
        /*
         * In cases where there is more than one valid definition for the same property,
         * the best match should be returned.
         */
        Value doubleValue = session.getValueFactory().createValue(0.7);
        Value longValue = session.getValueFactory().createValue(10);
        Value stringValue = session.getValueFactory().createValue("Should not work");
        
        JcrPropertyDefinition propDef;

        // Should prefer the double definition from NODE_TYPE_C since the value is of type double
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_D,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         doubleValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        // Should prefer the long definition from NODE_TYPE_C since the value is of type long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_D,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         longValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);

        // Should not allow a string though, since the NODE_TYPE_C definition narrows the acceptable types to double and long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_D,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         stringValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(nullValue()));
        
    }

    static final Name NODE_TYPE_A = new BasicName(TestLexicon.Namespace.URI, "nodeA");
    static final Name NODE_TYPE_B = new BasicName(TestLexicon.Namespace.URI, "nodeB");
    static final Name NODE_TYPE_C = new BasicName(TestLexicon.Namespace.URI, "nodeC");
    static final Name NODE_TYPE_D = new BasicName(TestLexicon.Namespace.URI, "nodeD");

    static final Name SINGLE_PROP1 = new BasicName(TestLexicon.Namespace.URI, "singleProp1");
    static final Name SINGLE_PROP2 = new BasicName(TestLexicon.Namespace.URI, "singleProp2");

    class TestNodeTypeSource extends AbstractJcrNodeTypeSource {

        /** The list of primary node types. */
        private final List<JcrNodeType> nodeTypes;

        /*
         * Build a hierarchy of node types with the following relationships:
         *  
         *   dnatest:nodeA extends nt:base
         *   dnatest:nodeB extends nt:base
         *   dnatest:nodeC extends dnatest:nodeB
         *   dnatest:nodeD extends dnatest:nodeA and dnatest:nodeC
         *   
         * And the following single-valued property definitions
         * 
         *   dnatest:nodeA defines properties:
         *      dnatest:singleProp1 of type STRING
         *   dnatest:nodeB defines properties:
         *      dnatest:singleProp1 of type LONG
         *      dnatest:singleProp2 of type UNDEFINED
         *   dnatest:nodeC defines properties:
         *      dnatest:singleProp1 of type BOOLEAN
         *      dnatest:singleProp2 of type DOUBLE     
         *      dnatest:singleProp2 of type LONG (note the double-definition)
         *   dnatest:nodeD defines properties:
         *      < NO PROPERTIES DEFINED IN THIS TYPE >
         */

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

            // Stubbing in child node and property definitions for now
            JcrNodeType nodeA = new JcrNodeType(
                                                context,
                                                NO_NODE_TYPE_MANAGER,
                                                NODE_TYPE_A,
                                                Arrays.asList(new JcrNodeType[] {base}),
                                                NO_PRIMARY_ITEM_NAME,
                                                NO_CHILD_NODES,
                                                Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                     context,
                                                                                                                     null,
                                                                                                                     SINGLE_PROP1,
                                                                                                                     OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                                                     false,
                                                                                                                     false,
                                                                                                                     false,
                                                                                                                     NO_DEFAULT_VALUES,
                                                                                                                     PropertyType.STRING,
                                                                                                                     NO_CONSTRAINTS,
                                                                                                                     false),}),
                                                NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType nodeB = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, NODE_TYPE_B,
                                                Arrays.asList(new JcrNodeType[] {base}), NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES,
                                                Arrays.asList(new JcrPropertyDefinition[] {
                                                    new JcrPropertyDefinition(context, null, SINGLE_PROP1,
                                                                              OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                              false, false, false, NO_DEFAULT_VALUES,
                                                                              PropertyType.LONG, NO_CONSTRAINTS, false),
                                                    new JcrPropertyDefinition(context, null, SINGLE_PROP2,
                                                                              OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                              false, false, false, NO_DEFAULT_VALUES,
                                                                              PropertyType.UNDEFINED, NO_CONSTRAINTS, false),}),
                                                NOT_MIXIN, UNORDERABLE_CHILD_NODES);
            JcrNodeType nodeC = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, NODE_TYPE_C,
                                                Arrays.asList(new JcrNodeType[] {nodeB}), NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES,
                                                Arrays.asList(new JcrPropertyDefinition[] {
                                                    new JcrPropertyDefinition(context, null, SINGLE_PROP1,
                                                                              OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                              false, false, false, NO_DEFAULT_VALUES,
                                                                              PropertyType.BOOLEAN, NO_CONSTRAINTS, false),
                                                    new JcrPropertyDefinition(context, null, SINGLE_PROP2,
                                                                              OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                              false, false, false, NO_DEFAULT_VALUES,
                                                                              PropertyType.DOUBLE, NO_CONSTRAINTS, false),
                                                    new JcrPropertyDefinition(context, null, SINGLE_PROP2,
                                                                              OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                              false, false, false, NO_DEFAULT_VALUES,
                                                                              PropertyType.LONG, NO_CONSTRAINTS, false),

                                                }), NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType nodeD = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, NODE_TYPE_D, Arrays.asList(new JcrNodeType[] {
                nodeA, nodeC}), NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES, NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            nodeTypes.addAll(Arrays.asList(new JcrNodeType[] {nodeA, nodeB, nodeC, nodeD}));
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