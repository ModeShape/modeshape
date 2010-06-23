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

import java.io.InputStream;
import java.security.Principal;
import java.util.Properties;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.security.config.IDTrustConfiguration;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.property.Path;

/**
 * Concrete implementation of {@link RepositoryStub} based on ModeShape-specific configuration.
 */
public class ModeShapeRepositoryStub extends RepositoryStub {

    public static final String MODE_SHAPE_SKIP_IMPORT = "javax.jcr.tck.modeSkipImport";
    public static final String MODE_SHAPE_NODE_TYPE_PATH = "javax.jcr.tck.modeNodeTypePath";

    private static final String REPOSITORY_SOURCE_NAME = "Test Repository Source";

    private static String currentConfigurationName = "default";
    private static boolean reloadRepositoryInstance = false;

    private Properties configProps;
    private String repositoryConfigurationName;
    private JcrRepository repository;
    private JcrEngine engine;

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

    public ModeShapeRepositoryStub( Properties env ) {
        super(env);

        configureRepository();
    }

    private void configureRepository() {
        repositoryConfigurationName = currentConfigurationName;

        // Create the in-memory (ModeShape) repository
        JcrConfiguration configuration = new JcrConfiguration();
        try {
            configProps = new Properties();
            String propsFileName = "/tck/" + repositoryConfigurationName + "/repositoryOverlay.properties";
            InputStream propsStream = getClass().getResourceAsStream(propsFileName);
            configProps.load(propsStream);

            String configFileName = "/tck/" + repositoryConfigurationName + "/configRepository.xml";
            configuration.loadFrom(getClass().getResourceAsStream(configFileName));

            if (engine != null) {
                try {
                    // Terminate any existing engine ...
                    engine.shutdown();
                } finally {
                    engine = null;
                }
            }

            engine = configuration.build();
            engine.start();

            Problems problems = engine.getProblems();
            // Print all of the problems from the engine configuration ...
            for (Problem problem : problems) {
                System.err.println(problem);
            }
            if (problems.hasErrors()) {
                throw new IllegalStateException("Problems starting JCR repository");
            }

            ExecutionContext executionContext = engine.getExecutionContext();
            executionContext.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

            repository = engine.getRepository(REPOSITORY_SOURCE_NAME);

            // This needs to check configProps directly to avoid an infinite loop
            String skipImport = (String)configProps.get(MODE_SHAPE_SKIP_IMPORT);
            if (!Boolean.valueOf(skipImport)) {

                // Set up some sample nodes in the graph to match the expected test configuration
                Graph graph = Graph.create(repository.getRepositorySourceName(),
                                           engine.getRepositoryConnectionFactory(),
                                           executionContext);
                Path destinationPath = executionContext.getValueFactories().getPathFactory().createRootPath();

                InputStream xmlStream = getClass().getResourceAsStream("/tck/repositoryForTckTests.xml");
                graph.importXmlFrom(xmlStream).into(destinationPath);

                graph.createWorkspace().named("otherWorkspace");
                graph.useWorkspace("otherWorkspace");
                graph.clone("/testroot").fromWorkspace("default").as("testroot").into("/").failingIfAnyUuidsMatch();
            }
        } catch (Exception ex) {
            // The TCK tries to quash this exception. Print it out to be more obvious.
            ex.printStackTrace();
            throw new IllegalStateException("Failed to initialize the repository with text content.", ex);
        }

    }

    public static void setCurrentConfigurationName( String newConfigName ) {
        ModeShapeRepositoryStub.currentConfigurationName = newConfigName;
    }

    public static void reloadRepositoryInstance() {
        reloadRepositoryInstance = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getRepository()
     */
    @Override
    public JcrRepository getRepository() {
        if (!currentConfigurationName.equals(repositoryConfigurationName) || reloadRepositoryInstance) {
            reloadRepositoryInstance = false;
            configureRepository();
        }
        return repository;
    }

    @Override
    public String getProperty( String name ) {
        String value = configProps.getProperty(name);
        if (value != null) return value;

        return super.getProperty(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getKnownPrincipal(javax.jcr.Session)
     */
    @SuppressWarnings( "unused" )
    @Override
    public Principal getKnownPrincipal( Session session ) throws RepositoryException {
        // TODO: initial implementation
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getUnknownPrincipal(javax.jcr.Session)
     */
    @SuppressWarnings( "unused" )
    @Override
    public Principal getUnknownPrincipal( Session session ) throws RepositoryException, NotExecutableException {
        // TODO: initial implementation
        return null;
    }

}
