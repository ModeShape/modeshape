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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Path;
import org.jboss.security.config.IDTrustConfiguration;
import org.xml.sax.SAXException;

/**
 * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
 */
public class DnaRepositoryStub extends RepositoryStub {
    private static final String REPOSITORY_SOURCE_NAME = "Test Repository Source";

    private static String currentConfigurationName = "default";

    private final Properties configProps;
    private final JcrRepository repository;

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

    public DnaRepositoryStub( Properties env ) {
        super(env);

        // Create the in-memory (DNA) repository
        JcrConfiguration configuration = new JcrConfiguration();
        try {
            configProps = new Properties();
            String propsFileName = "/tck/" + currentConfigurationName + "/repositoryOverlay.properties";
            InputStream propsStream = getClass().getResourceAsStream(propsFileName);
            configProps.load(propsStream);

            String configFileName = "/tck/" + currentConfigurationName + "/configRepository.xml";
            configuration.loadFrom(getClass().getResourceAsStream(configFileName));

            // Add the the node types for the source ...
            configuration.repository(REPOSITORY_SOURCE_NAME).addNodeTypes(getClass().getResourceAsStream("/tck/tck_test_types.cnd"));
        } catch (SAXException se) {
            se.printStackTrace();
            throw new IllegalStateException(se);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException(ioe);
        }

        JcrEngine engine = configuration.build();
        engine.start();

        // Problems problems = engine.getRepositoryService().getStartupProblems();
        Problems problems = engine.getProblems();
        // Print all of the problems from the engine configuration ...
        for (Problem problem : problems) {
            System.err.println(problem);
        }
        if (problems.hasErrors()) {
            throw new IllegalStateException("Problems starting JCR repository");
        }

        repository = getAndLoadRepository(engine, REPOSITORY_SOURCE_NAME);
    }

    private JcrRepository getAndLoadRepository( JcrEngine engine,
                                                String repositoryName ) {

        ExecutionContext executionContext = engine.getExecutionContext();
        executionContext.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        try {
            JcrRepository repository = engine.getRepository(REPOSITORY_SOURCE_NAME);

            // Set up some sample nodes in the graph to match the expected test configuration
            Graph graph = Graph.create(repository.getRepositorySourceName(),
                                       engine.getRepositoryConnectionFactory(),
                                       executionContext);
            Path destinationPath = executionContext.getValueFactories().getPathFactory().createRootPath();

            InputStream xmlStream = getClass().getResourceAsStream("/tck/repositoryForTckTests.xml");
            graph.importXmlFrom(xmlStream).into(destinationPath);

            graph.createWorkspace().named("otherWorkspace");
            return repository;

        } catch (Exception ex) {
            // The TCK tries to quash this exception. Print it out to be more obvious.
            ex.printStackTrace();
            throw new IllegalStateException("Failed to initialize the repository with text content.", ex);
        }

    }

    public static void setCurrentConfigurationName( String newConfigName ) {
        DnaRepositoryStub.currentConfigurationName = newConfigName;
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

    @Override
    public String getProperty( String name ) {
        String value = configProps.getProperty(name);
        if (value != null) return value;

        return super.getProperty(name);
    }

}
