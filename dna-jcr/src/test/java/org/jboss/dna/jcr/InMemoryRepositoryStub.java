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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.io.GraphImporter;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.JcrRepository.Options;
import org.jboss.security.config.IDTrustConfiguration;

/**
 * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
 */
public class InMemoryRepositoryStub extends RepositoryStub {
    private JcrRepository repository;

    static {

        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public InMemoryRepositoryStub( Properties env ) {
        super(env);

        // Create the in-memory (DNA) repository
        final InMemoryRepositorySource source = new InMemoryRepositorySource();

        // Various calls will fail if you do not set a non-null name for the source
        source.setName("TestRepositorySource");

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) {
                return source.getConnection();
            }
        };

        // Wrap a connection to the in-memory (DNA) repository in a (JCR) repository
        Map<Options, String> options = Collections.singletonMap(Options.PROJECT_NODE_TYPES, "false");

        repository = new JcrRepository(executionContext, connectionFactory, source.getName(), null, options);
        RepositoryNodeTypeManager nodeTypes = repository.getRepositoryTypeManager();

        // Set up some sample nodes in the graph to match the expected test configuration
        Graph graph = Graph.create(source.getName(), connectionFactory, executionContext);
        GraphImporter importer = new GraphImporter(graph);
        Path destinationPath = executionContext.getValueFactories().getPathFactory().createRootPath();

        try {
            CndNodeTypeSource nodeTypeSource = new CndNodeTypeSource("/tck_test_types.cnd");

            for (Problem problem : nodeTypeSource.getProblems()) {
                System.err.println(problem);
            }
            if (!nodeTypeSource.isValid()) {
                throw new IllegalStateException("Problems loading TCK test node types");
            }

            nodeTypes.registerNodeTypes(nodeTypeSource);

            URI xmlContent = new File("src/test/resources/repositoryForTckTests.xml").toURI();
            importer.importXml(xmlContent, Location.create(destinationPath)).execute();

        } catch (Exception ex) {
            // The TCK tries to quash this exception. Print it out to be more obvious.
            ex.printStackTrace();
            throw new IllegalStateException("Repository initialization failed.", ex);
        }
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
}
