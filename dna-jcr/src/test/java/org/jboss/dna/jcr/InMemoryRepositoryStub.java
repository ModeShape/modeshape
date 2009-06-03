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

import java.net.URI;
import java.util.Properties;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.security.config.IDTrustConfiguration;

/**
 * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
 */
public class InMemoryRepositoryStub extends RepositoryStub {
    private static final String REPOSITORY_SOURCE_NAME = "Test Repository Source";

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
        JcrConfiguration configuration = new JcrConfiguration();
        // Define the single in-memory repository source ...
        configuration.repositorySource("Store")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("JCR Repository persistent store");
        // Define the JCR repository for the source ...
        configuration.repository(REPOSITORY_SOURCE_NAME)
                     .setSource("Store")
                     .setOption(Option.PROJECT_NODE_TYPES, "false")
                     .addNodeTypes(getClass().getClassLoader().getResource("tck_test_types.cnd"));
        // Save and build the engine ...
        configuration.save();
        JcrEngine engine = configuration.build();
        engine.start();

        // Print all of the problems from the engine configuration ...
        for (Problem problem : engine.getProblems()) {
            System.err.println(problem);
        }
        if (engine.getProblems().hasErrors()) {
            throw new IllegalStateException("Problems starting JCR repository");
        }

        ExecutionContext executionContext = engine.getExecutionContext();
        executionContext.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        try {
            repository = engine.getRepository(REPOSITORY_SOURCE_NAME);

            // Set up some sample nodes in the graph to match the expected test configuration
            Graph graph = Graph.create(repository.getRepositorySourceName(),
                                       engine.getRepositoryConnectionFactory(),
                                       executionContext);
            Path destinationPath = executionContext.getValueFactories().getPathFactory().createRootPath();
            // URI xmlContent = new File("src/test/resources/repositoryForTckTests.xml").toURI();
            URI xmlContent = getClass().getClassLoader().getResource("repositoryForTckTests.xml").toURI();
            graph.importXmlFrom(xmlContent).into(destinationPath);

        } catch (Exception ex) {
            // The TCK tries to quash this exception. Print it out to be more obvious.
            ex.printStackTrace();
            throw new IllegalStateException("Failed to initialize the repository with text content.", ex);
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
