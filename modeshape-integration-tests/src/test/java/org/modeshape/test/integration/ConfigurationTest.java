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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

public class ConfigurationTest {

    private JcrConfiguration configuration;
    private JcrEngine engine;

    @Before
    public void beforeEach() {
        configuration = new JcrConfiguration();
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

        // Initialize IDTrust and a policy file (which defines the "modeshape-jcr" login config name)
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("magnolia");
        assertThat(repository, is(notNullValue()));

        // Get the predefined workspaces on the 'magnolia' repository source ...
        Set<String> magnoliaWorkspaces = engine.getGraph("magnolia").getWorkspaces();
        Set<String> diskWorkspaces = engine.getGraph("disk").getWorkspaces();
        Set<String> dataWorkspaces = engine.getGraph("data").getWorkspaces();

        assertThat(magnoliaWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
            "usergroups", "mgnlSystem", "mgnlVersion", "downloads"})));
        assertThat(dataWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
            "usergroups", "mgnlSystem", "mgnlVersion", "modeSystem"})));
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
                assertThat(jcrWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
                    "usergroups", "mgnlSystem", "mgnlVersion", "downloads"})));
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

        // Initialize IDTrust and a policy file (which defines the "modeshape-jcr" login config name)
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("data");
        assertThat(repository, is(notNullValue()));

        // Get the predefined workspaces on the 'magnolia' repository source ...
        Set<String> magnoliaWorkspaces = engine.getGraph("magnolia").getWorkspaces();
        Set<String> diskWorkspaces = engine.getGraph("disk").getWorkspaces();
        Set<String> dataWorkspaces = engine.getGraph("data").getWorkspaces();

        assertThat(magnoliaWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
            "usergroups", "mgnlSystem", "mgnlVersion", "downloads"})));
        assertThat(dataWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
            "usergroups", "mgnlSystem", "mgnlVersion", "modeSystem"})));
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
                assertThat(jcrWorkspaces, is(Collections.unmodifiableSet(new String[] {"config", "website", "users", "userroles",
                    "usergroups", "mgnlSystem", "mgnlVersion"})));
            } finally {
                if (session != null) session.logout();
            }
        }
    }

}
