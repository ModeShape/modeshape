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

import java.io.File;
import java.net.URI;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrMixLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.io.GraphImporter;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.JcrRepository.Options;

/**
 * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
 */
public class InMemoryRepositoryStub extends RepositoryStub {
    private JcrRepository repository;
    protected InMemoryRepositorySource source;
    protected AccessControlContext accessControlContext = AccessController.getContext();

    private Credentials superUserCredentials = new Credentials() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unused" )
        public AccessControlContext getAccessControlContext() {
            return accessControlContext;
        }
    };

    private Credentials readWriteCredentials = new Credentials() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unused" )
        public AccessControlContext getAccessControlContext() {
            return accessControlContext;
        }
    };

    private Credentials readOnlyCredentials = new Credentials() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unused" )
        public AccessControlContext getAccessControlContext() {
            return accessControlContext;
        }
    };

    protected ExecutionContext executionContext = new ExecutionContext();

    protected RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
        public RepositoryConnection createConnection( String sourceName ) {
            return source.getConnection();
        }
    };

    public InMemoryRepositoryStub( Properties env ) {
        super(env);

        // Create the in-memory (DNA) repository
        source = new InMemoryRepositorySource();

        // Various calls will fail if you do not set a non-null name for the source
        source.setName("TestRepositorySource");

        // Make sure the path to the namespaces exists ...
        Graph graph = Graph.create(source.getName(), connectionFactory, executionContext);
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

        // Wrap a connection to the in-memory (DNA) repository in a (JCR) repository
        Map<Options, String> options = Collections.singletonMap(Options.PROJECT_NODE_TYPES, "false");

        repository = new JcrRepository(executionContext.create(accessControlContext), connectionFactory, source.getName(), null,
                                       options);
        RepositoryNodeTypeManager nodeTypes = repository.getRepositoryTypeManager();

        // Set up some sample nodes in the graph to match the expected test configuration
        try {
            nodeTypes.registerNodeTypes(new TckTestNodeTypeSource(executionContext, nodeTypes));

            executionContext.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

            Path destinationPath = executionContext.getValueFactories().getPathFactory().create("/");
            GraphImporter importer = new GraphImporter(graph);

            URI xmlContent = new File("src/test/resources/repositoryForTckTests.xml").toURI();
            Graph.Batch batch = importer.importXml(xmlContent, Location.create(destinationPath));
            batch.execute();

        } catch (Exception ex) {
            // The TCK tries to quash this exception. Print it out to be more obvious.
            ex.printStackTrace();
            throw new IllegalStateException("Repository initialization failed.", ex);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getSuperuserCredentials()
     */
    @Override
    public Credentials getSuperuserCredentials() {
        return superUserCredentials;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getReadOnlyCredentials()
     */
    @Override
    public Credentials getReadOnlyCredentials() {
        return readOnlyCredentials;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getReadOnlyCredentials()
     */
    @Override
    public Credentials getReadWriteCredentials() {
        return readWriteCredentials;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getRepository()
     */
    @Override
    public JcrRepository getRepository() {
        return repository;
    }

    class TckTestNodeTypeSource extends AbstractJcrNodeTypeSource {
        /** The list of node types. */
        private final List<JcrNodeType> nodeTypes;

        TckTestNodeTypeSource( ExecutionContext context,
                               RepositoryNodeTypeManager nodeTypeManager ) {
            super(null);

            nodeTypes = new ArrayList<JcrNodeType>();

            JcrNodeType base = nodeTypeManager.getNodeType(JcrNtLexicon.BASE);

            if (base == null) {
                String baseTypeName = JcrNtLexicon.BASE.getString(context.getNamespaceRegistry());
                String namespaceTypeName = TestLexicon.NO_SAME_NAME_SIBS.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            JcrNodeType referenceable = nodeTypeManager.getNodeType(JcrMixLexicon.REFERENCEABLE);

            if (referenceable == null) {
                String baseTypeName = JcrMixLexicon.REFERENCEABLE.getString(context.getNamespaceRegistry());
                String namespaceTypeName = TestLexicon.REFERENCEABLE_UNSTRUCTURED.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            JcrNodeType unstructured = nodeTypeManager.getNodeType(JcrNtLexicon.UNSTRUCTURED);

            if (unstructured == null) {
                String baseTypeName = JcrNtLexicon.UNSTRUCTURED.getString(context.getNamespaceRegistry());
                String namespaceTypeName = TestLexicon.REFERENCEABLE_UNSTRUCTURED.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            // Stubbing in child node and property definitions for now
            JcrNodeType noSameNameSibs = new JcrNodeType(
                                                         context,
                                                         NO_NODE_TYPE_MANAGER,
                                                         TestLexicon.NO_SAME_NAME_SIBS,
                                                         Arrays.asList(new JcrNodeType[] {base}),
                                                         NO_PRIMARY_ITEM_NAME,
                                                         Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                      context,
                                                                                                                      null,
                                                                                                                      ALL_NODES,
                                                                                                                      OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                      false,
                                                                                                                      false,
                                                                                                                      false,
                                                                                                                      false,
                                                                                                                      JcrNtLexicon.UNSTRUCTURED,
                                                                                                                      new JcrNodeType[] {base}),}),
                                                         NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            JcrNodeType referenceableUnstructured = new JcrNodeType(
                                                                    context,
                                                                    NO_NODE_TYPE_MANAGER,
                                                                    TestLexicon.REFERENCEABLE_UNSTRUCTURED,
                                                                    Arrays.asList(new JcrNodeType[] {unstructured, referenceable}),
                                                                    NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES, NO_PROPERTIES,
                                                                    NOT_MIXIN, ORDERABLE_CHILD_NODES);

            JcrNodeType nodeWithMandatoryProperty = new JcrNodeType(
                                                                    context,
                                                                    NO_NODE_TYPE_MANAGER,
                                                                    TestLexicon.NODE_WITH_MANDATORY_PROPERTY,
                                                                    Arrays.asList(new JcrNodeType[] {unstructured, referenceable}),
                                                                    NO_PRIMARY_ITEM_NAME,
                                                                    NO_CHILD_NODES,
                                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                                         context,
                                                                                                                                         null,
                                                                                                                                         TestLexicon.MANDATORY_STRING,
                                                                                                                                         OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                                                                         false,
                                                                                                                                         true,
                                                                                                                                         false,
                                                                                                                                         NO_DEFAULT_VALUES,
                                                                                                                                         PropertyType.UNDEFINED,
                                                                                                                                         NO_CONSTRAINTS,
                                                                                                                                         false)}),
                                                                    NOT_MIXIN, ORDERABLE_CHILD_NODES);
            JcrNodeType nodeWithMandatoryChild = new JcrNodeType(
                                                                 context,
                                                                 NO_NODE_TYPE_MANAGER,
                                                                 TestLexicon.NODE_WITH_MANDATORY_CHILD,
                                                                 Arrays.asList(new JcrNodeType[] {unstructured, referenceable}),
                                                                 NO_PRIMARY_ITEM_NAME,
                                                                 Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                              context,
                                                                                                                              null,
                                                                                                                              TestLexicon.MANDATORY_CHILD,
                                                                                                                              OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                              false,
                                                                                                                              true,
                                                                                                                              false,
                                                                                                                              false,
                                                                                                                              JcrNtLexicon.UNSTRUCTURED,
                                                                                                                              new JcrNodeType[] {base}),}),
                                                                 NO_PROPERTIES, NOT_MIXIN, ORDERABLE_CHILD_NODES);

            JcrNodeType unorderableUnstructured = new JcrNodeType(
                                                                  context,
                                                                  NO_NODE_TYPE_MANAGER,
                                                                  TestLexicon.UNORDERABLE_UNSTRUCTURED,
                                                                  Arrays.asList(new JcrNodeType[] {base}),
                                                                  NO_PRIMARY_ITEM_NAME,
                                                                  Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                               context,
                                                                                                                               null,
                                                                                                                               ALL_NODES,
                                                                                                                               OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                               false,
                                                                                                                               false,
                                                                                                                               false,
                                                                                                                               true,
                                                                                                                               TestLexicon.UNORDERABLE_UNSTRUCTURED,
                                                                                                                               new JcrNodeType[] {base}),}),
                                                                  Arrays.asList(new JcrPropertyDefinition[] {
                                                                      new JcrPropertyDefinition(
                                                                                                context,
                                                                                                null,
                                                                                                ALL_NODES,
                                                                                                OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                                false, false, false,
                                                                                                NO_DEFAULT_VALUES,
                                                                                                PropertyType.UNDEFINED,
                                                                                                NO_CONSTRAINTS, false),
                                                                      new JcrPropertyDefinition(
                                                                                                context,
                                                                                                null,
                                                                                                ALL_NODES,
                                                                                                OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                                false, false, false,
                                                                                                NO_DEFAULT_VALUES,
                                                                                                PropertyType.UNDEFINED,
                                                                                                NO_CONSTRAINTS, true),}),
                                                                  NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            nodeTypes.addAll(Arrays.asList(new JcrNodeType[] {referenceableUnstructured, noSameNameSibs,
                nodeWithMandatoryProperty, nodeWithMandatoryChild, unorderableUnstructured,}));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.JcrNodeTypeSource#getNodeTypes()
         */
        @Override
        public Collection<JcrNodeType> getDeclaredNodeTypes() {
            return nodeTypes;
        }
    }
}
