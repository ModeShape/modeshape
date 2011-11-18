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
import java.net.URL;
import java.security.Principal;
import java.util.Properties;
import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import org.apache.jackrabbit.test.RepositoryStub;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * Concrete implementation of {@link RepositoryStub} based on ModeShape-specific configuration.
 */
public class ModeShapeRepositoryStub extends RepositoryStub {

    public static final String MODE_SHAPE_SKIP_IMPORT = "javax.jcr.tck.modeSkipImport";
    public static final String MODE_SHAPE_NODE_TYPE_PATH = "javax.jcr.tck.modeNodeTypePath";

    /**
     * System views can distinguis multi-valued properties that happen to have only one value. Document views cannot experss this,
     * so import using the system view.
     */
    // public static final String MODE_SHAPE_IMPORTED_CONTENT = "/tck/documentViewForTckTests.xml";
    public static final String MODE_SHAPE_IMPORTED_CONTENT = "/tck/systemViewForTckTests.xml";

    private static String currentConfigurationName = "default";
    private static boolean reloadRepositoryInstance = false;

    private Properties configProps;
    private String repositoryConfigurationName;
    private JcrRepository repository;
    private JcrEngine engine;

    static {
        // Initialize the JAAS configuration to allow for an admin login later
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    public ModeShapeRepositoryStub( Properties env ) {
        super(env);

        configureRepository();
    }

    private void configureRepository() {
        repositoryConfigurationName = currentConfigurationName;

        // Clean up from a previous invocation ...
        if (engine != null) {
            CacheContainer container = null;
            if (repository != null) {
                container = repository.database().getCache().getCacheManager();
            }
            try {
                // Terminate any existing engine, and no need to block ...
                engine.shutdown();
            } finally {
                engine = null;
                if (container != null) {
                    try {
                        TestingUtil.killCacheManagers(container);
                    } finally {
                        container = null;
                    }
                }
            }
        }
        // Read the configuration file and setup the engine ...
        RepositoryConfiguration configuration = null;

        try {
            configProps = new Properties();
            String propsFileName = "/tck/" + repositoryConfigurationName + "/repositoryOverlay.properties";
            InputStream propsStream = getClass().getResourceAsStream(propsFileName);
            configProps.load(propsStream);

            String configFileName = "/tck/" + repositoryConfigurationName + "/repo-config.json";
            InputStream configStream = getClass().getResourceAsStream(configFileName);
            configuration = RepositoryConfiguration.read(configStream, configFileName);
            if (configuration == null) {
                throw new IllegalStateException(
                                                "Problems starting JCR repository: unable to find ModeShape configuration file \""
                                                + configFileName + "\" on the classpath");
            }

            engine = new JcrEngine();
            engine.start();

            // Deploy and start the repository, and block until started...
            repository = engine.deploy(configuration);
            engine.startRepository(repository.getName()).get();

            // Set up the repository content (for all workspaces) ...
            Session session = repository.login(superuser);
            Credentials superuser = getSuperuserCredentials();
            try {
                // Register the test namespaces in the repository ...
                NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
                registry.registerNamespace(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

                // Register the node types needed in our tests ...
                String cndFileName = "/tck/tck_test_types.cnd";
                URL cndUrl = getClass().getResource(cndFileName);
                NodeTypeManager nodeTypeManager = (NodeTypeManager)session.getWorkspace().getNodeTypeManager();
                nodeTypeManager.registerNodeTypeDefinitions(cndUrl);

                // This needs to check configProps directly to avoid an infinite loop
                String skipImport = (String)configProps.get(MODE_SHAPE_SKIP_IMPORT);
                if (!Boolean.valueOf(skipImport)) {

                    // Set up some sample nodes in the "default" workspace to match the expected test configuration ...
                    InputStream xmlStream = getClass().getResourceAsStream(MODE_SHAPE_IMPORTED_CONTENT);
                    session.getWorkspace().importXML("/", xmlStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

                    // Switch workspaces ...
                    session.logout();
                    session = repository.login(superuser, "otherWorkspace");

                    // Set up some sample nodes in the "otherWorkspace" workspace to match the expected test configuration ...
                    xmlStream = getClass().getResourceAsStream(MODE_SHAPE_IMPORTED_CONTENT);
                    session.getWorkspace().importXML("/", xmlStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
                    session.save();
                }
            } finally {
                session.logout();
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
    @Override
    public Principal getKnownPrincipal( Session session ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.jackrabbit.test.RepositoryStub#getUnknownPrincipal(javax.jcr.Session)
     */
    @Override
    public Principal getUnknownPrincipal( Session session ) {
        return null;
    }

}
