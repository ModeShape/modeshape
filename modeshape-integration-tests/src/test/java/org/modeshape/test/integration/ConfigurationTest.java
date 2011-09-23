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
package org.modeshape.test.integration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.CndNodeTypeReader;
import org.modeshape.jcr.JaasTestUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository.Option;

public class ConfigurationTest {

    private JcrConfiguration configuration;
    private JcrEngine engine;

    @BeforeClass
    public static void beforeAll() {
        // Initialize PicketBox ...
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    @Before
    public void beforeEach() {
        configuration = new JcrConfiguration();
        FileUtil.delete("target/database/ConfigurationTest");
        FileUtil.delete("target/testConfig/modeshape/repositories/");
    }

    @After
    public void afterEach() throws Exception {
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
            }
        }
    }

    // protected ExecutionContext context() {
    // return configuration.getConfigurationDefinition().getContext();
    // }
    //
    // protected Path path( String path ) {
    // return context().getValueFactories().getPathFactory().create(path);
    // }
    //
    // protected Path.Segment segment( String segment ) {
    // return context().getValueFactories().getPathFactory().createSegment(segment);
    // }

    @Test
    public void shouldLoadFederatingConfig() throws Exception {
        File file = new File("src/test/resources/config/federatingConfigRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Verify that the configration was loaded correctly by checking a few things ...
        assertThat(configuration.repository("magnolia").getSource(), is("magnolia"));
        assertThat(configuration.repositorySource("magnolia").getName(), is("magnolia"));
        assertThat(configuration.repositorySource("disk").getName(), is("disk"));
        assertThat(configuration.repositorySource("data").getName(), is("data"));

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("magnolia");
        assertThat(repository, is(notNullValue()));

        // Get the predefined workspaces on the 'magnolia' repository source ...
        Set<String> magnoliaWorkspaces = engine.getGraph("magnolia").getWorkspaces();
        Set<String> diskWorkspaces = engine.getGraph("disk").getWorkspaces();
        Set<String> dataWorkspaces = engine.getGraph("data").getWorkspaces();

        assertThat(magnoliaWorkspaces,
                   is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                       "mgnlSystem", "mgnlVersion", "downloads"})));
        assertThat(dataWorkspaces,
                   is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                       "mgnlSystem", "mgnlVersion", "modeSystem"})));
        assertThat(diskWorkspaces, is(Collections.unmodifiableSet(new String[] {"workspace1"})));

        // Create a session, authenticating using one of the usernames defined by our JAAS policy file(s) ...
        Session session = null;
        Credentials credentials = new SimpleCredentials("superuser", "superuser".toCharArray());
        String[] workspaceNames = {"config", "website", "users", "userroles", "usergroups", "mgnlSystem", "mgnlVersion",
            "downloads"};
        for (String workspaceName : workspaceNames) {
            try {
                session = repository.login(credentials, workspaceName);
                session.getRootNode().addNode("testNode", "nt:folder");

                // Check that the workspaces are all available ...
                Set<String> jcrWorkspaces = Collections.unmodifiableSet(session.getWorkspace().getAccessibleWorkspaceNames());
                assertThat(jcrWorkspaces,
                           is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                               "mgnlSystem", "mgnlVersion", "downloads"})));
            } finally {
                if (session != null) session.logout();
            }
        }
    }

    @Test
    public void shouldCreateInMemoryRepository() throws Exception {
        File file = new File("src/test/resources/config/federatingConfigRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Verify that the configration was loaded correctly by checking a few things ...
        assertThat(configuration.repository("magnolia").getSource(), is("magnolia"));
        assertThat(configuration.repositorySource("magnolia").getName(), is("magnolia"));
        assertThat(configuration.repositorySource("disk").getName(), is("disk"));
        assertThat(configuration.repositorySource("data").getName(), is("data"));

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("data");
        assertThat(repository, is(notNullValue()));

        // Get the predefined workspaces on the 'magnolia' repository source ...
        Set<String> magnoliaWorkspaces = engine.getGraph("magnolia").getWorkspaces();
        Set<String> diskWorkspaces = engine.getGraph("disk").getWorkspaces();
        Set<String> dataWorkspaces = engine.getGraph("data").getWorkspaces();

        assertThat(magnoliaWorkspaces,
                   is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                       "mgnlSystem", "mgnlVersion", "downloads"})));
        assertThat(dataWorkspaces,
                   is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                       "mgnlSystem", "mgnlVersion", "modeSystem"})));
        assertThat(diskWorkspaces, is(Collections.unmodifiableSet(new String[] {"workspace1"})));

        // Create a session, authenticating using one of the usernames defined by our JAAS policy file(s) ...
        Session session = null;
        Credentials credentials = new SimpleCredentials("superuser", "superuser".toCharArray());
        String[] workspaceNames = {"config", "website", "users", "userroles", "usergroups", "mgnlSystem", "mgnlVersion"};
        for (String workspaceName : workspaceNames) {
            try {
                session = repository.login(credentials, workspaceName);
                session.getRootNode().addNode("testNode", "nt:folder");

                // Check that the workspaces are all available ...
                Set<String> jcrWorkspaces = Collections.unmodifiableSet(session.getWorkspace().getAccessibleWorkspaceNames());
                assertThat(jcrWorkspaces,
                           is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles", "usergroups",
                               "mgnlSystem", "mgnlVersion"})));
            } finally {
                if (session != null) session.logout();
            }
        }
    }

    @Test
    public void shouldEnableRebuildingIndexesUponStartupInBackgroundConfiguredProgrammatically() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);
        configuration.repository("My repository").setOption(Option.QUERY_INDEXES_REBUILT_SYNCHRONOUSLY, false);

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("My repository");
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldEnableRebuildingIndexesUponStartupInBackground() throws Exception {
        File file = new File("src/test/resources/config/configRepositoryWithAsynchronousIndex.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("My repository");
        assertThat(repository, is(notNullValue()));
    }

    @FixFor( "MODE-1224" )
    @Test
    public void shouldStartWithEdsConfigurationAndRestart() throws Exception {
        Logger.getLogger(getClass()).debug("Here we go...");
        File file = new File("src/test/resources/config/configRepositoryEds.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("eds");
        assertThat(repository, is(notNullValue()));
        Thread.sleep(3000L);

        // Shutdown ...
        engine.shutdownAndAwaitTermination(3, TimeUnit.SECONDS);

        Logger.getLogger(getClass()).debug("*** Restarting the engine ***");
        // Restart ...
        JcrConfiguration configuration2 = new JcrConfiguration();
        configuration2.loadFrom(file);

        engine = configuration2.build();
        engine.start();
        repository = engine.getRepository("eds");
        assertThat(repository, is(notNullValue()));
        Thread.sleep(3000L);
    }

    @Test
    public void shouldWork() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("My repository");
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldWork2() throws Exception {
        File file = new File("src/test/resources/config/configRepositoryForBrix.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(file);

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("Brix repository");
        assertThat(repository, is(notNullValue()));
    }

    @FixFor( "MODE-1216" )
    @Test
    public void shouldReadNodeTypesUponRestartUsingJpaConnector() throws Exception {
        // Delete the data files ...
        FileUtil.delete("target/database/ConfigurationTest");

        File file = new File("src/test/resources/config/configRepositoryForJpaWithDiskStorage.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        JcrConfiguration config1 = new JcrConfiguration();
        config1.loadFrom(file);

        JcrConfiguration config2 = new JcrConfiguration();
        config2.loadFrom(file);

        startEngineThenShutdownThenRestartAndTestForNodeTypes(config1, config2);
    }

    @FixFor( "MODE-1190" )
    @Test
    public void shouldReadNodeTypesUponRestartUsingDiskConnector() throws Exception {
        // Delete the data files ...
        FileUtil.delete("target/database/ConfigurationTest");

        File file = new File("src/test/resources/config/configRepositoryForDiskStorage.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        JcrConfiguration config1 = new JcrConfiguration();
        config1.loadFrom(file);

        JcrConfiguration config2 = new JcrConfiguration();
        config2.loadFrom(file);

        startEngineThenShutdownThenRestartAndTestForNodeTypes(config1, config2);
    }

    public void startEngineThenShutdownThenRestartAndTestForNodeTypes( JcrConfiguration config1,
                                                                       JcrConfiguration config2 ) throws Exception {
        // Create and start the engine ...
        JcrEngine engine = config1.build();
        engine.start();
        Repository repository = engine.getRepository("Repo");
        assertThat(repository, is(notNullValue()));

        // Import the node types ...

        // Check the node types ...
        List<String> nodeTypeNames = new ArrayList<String>();
        Session session = repository.login();
        try {
            readNodeTypes(session, "/io/drools/configuration_node_type.cnd");
            readNodeTypes(session, "/io/drools/tag_node_type.cnd");
            readNodeTypes(session, "/io/drools/state_node_type.cnd");
            readNodeTypes(session, "/io/drools/versionable_node_type.cnd");
            readNodeTypes(session, "/io/drools/versionable_asset_folder_node_type.cnd");
            readNodeTypes(session, "/io/drools/rule_node_type.cnd");
            readNodeTypes(session, "/io/drools/rulepackage_node_type.cnd");

            NodeTypeIterator nodeTypes = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();
            while (nodeTypes.hasNext()) {
                NodeType nodeType = nodeTypes.nextNodeType();
                nodeTypeNames.add(nodeType.getName());
            }
        } finally {
            session.logout();
            engine.shutdownAndAwaitTermination(3, TimeUnit.SECONDS);
        }

        java.util.Collections.sort(nodeTypeNames);

        // Restart ...
        engine = config2.build();
        engine.start();

        Repository repository2 = engine.getRepository("Repo");
        assertThat(repository2, is(notNullValue()));

        // Check the node types (again) ...
        List<String> nodeTypeNames2 = new ArrayList<String>();
        Session session2 = repository2.login();
        try {
            // Do NOT reload the node types ...

            NodeTypeIterator nodeTypes = session2.getWorkspace().getNodeTypeManager().getAllNodeTypes();
            while (nodeTypes.hasNext()) {
                NodeType nodeType = nodeTypes.nextNodeType();
                nodeTypeNames2.add(nodeType.getName());
            }
        } finally {
            session.logout();
            engine.shutdownAndAwaitTermination(3, TimeUnit.SECONDS);
        }

        java.util.Collections.sort(nodeTypeNames2);

        // System.out.println("Node types:" + nodeTypeNames);
        // System.out.println("Node types:" + nodeTypeNames2);
        assertThat(nodeTypeNames, is(nodeTypeNames2));
    }

    protected void readNodeTypes( Session session,
                                  String... cndResources ) throws IOException, RepositoryException {
        for (String cndResource : cndResources) {
            CndNodeTypeReader reader = new CndNodeTypeReader(session);
            reader.read(cndResource); // or stream or resource file
            assertThat(reader.getProblems().isEmpty(), is(true));
            // Now import ...
            session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), true);
        }
    }
}
