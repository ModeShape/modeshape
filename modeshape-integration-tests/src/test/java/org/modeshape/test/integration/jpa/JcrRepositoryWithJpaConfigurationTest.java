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
package org.modeshape.test.integration.jpa;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSecurityContextCredentials;

/**
 * 
 */
public class JcrRepositoryWithJpaConfigurationTest {

    @Test
    public void shouldConfigureWithInMemoryDatabase() throws RepositoryException, PrivilegedActionException {
        configureWithInMemoryDatabase(new CustomSecurityContext("bill"));
    }

    /**
     * Test some very basic functionality with an in-memory database and the supplied security context.
     * <p>
     * Note that this method is designed to be readable and serve as an example, and therefore there is a certain amount of
     * repeated code.
     * </p>
     * 
     * @param securityContext the security context, or null if JAAS should be used for authentication
     * @throws RepositoryException if there was an error with the repository
     * @throws PrivilegedActionException if there was an error executing a privileged action block as the current JAAS subject
     */
    protected void configureWithInMemoryDatabase( SecurityContext securityContext )
        throws RepositoryException, PrivilegedActionException {
        JcrConfiguration configuration = new JcrConfiguration();

        // Set up the repository source that this JCR repository will use ...
        configuration.repositorySource("MySource")
                     .setDescription("This is my repository source")
                     .usingClass("org.modeshape.connector.store.jpa.JpaSource")
                     .loadedFromClasspath()
                     // The rest of these properties are specific to the JpaSource class.

                     // Set up three workspaces that this source will make available,
                     // but for this example allow clients to create more workspaces ...
                     .setProperty("predefinedWorkspaceNames", "workspace1", "workspace2", "workspace3")
                     .setProperty("defaultWorkspaceName", "workspace1")
                     .setProperty("creatingWorkspacesAllowed", true)
                     // Now set the required JDBC properties, since we're using the 'Driver' approach ...
                     .setProperty("dialect", "org.hibernate.dialect.HSQLDialect")
                     .setProperty("driverClassName", "org.hsqldb.jdbcDriver")
                     .setProperty("username", "sa")
                     .setProperty("password", "")
                     .setProperty("url", "jdbc:hsqldb:.")
                     // Alternatively, you could instead point the source to a JDBC DataSource in JNDI ...
                     // .setProperty("dataSourceJndiName","jdbc/TestDB")

                     // Set some optional properties ...
                     .setProperty("maximumConnectionsInPool", 3)
                     .setProperty("minimumConnectionsInPool", 0)
                     .setProperty("numberOfConnectionsToAcquireAsNeeded", 1)
                     .setProperty("maximumSizeOfStatementCache", 100)
                     .setProperty("maximumConnectionIdleTimeInSeconds", 0)
                     .setProperty("largeValueSizeInBytes", 150)
                     .setProperty("compressData", true)
                     .setProperty("referentialIntegrityEnforced", true)
                     .setProperty("autoGenerateSchema", "create")
                     .setProperty("showSql", false);

        // Set up the JCR repository ...
        configuration.repository("My JCR Repository")
                     .setDescription("This is the description for my JCR repository (not really accessible through JCR though)")
                     // Tell it which repository source should be used ...
                     .setSource("MySource")
                     // Set the options (all of which have good defaults) ...
                     .setOption(JcrRepository.Option.PROJECT_NODE_TYPES, false)
                     // Load up some node types ...
                     // .addNodeTypes(fileOrUrlOrString)
                     // Register 0 or more namespaces (we'll do an example one here) ...
                     .registerNamespace("myns", "http://www.example.com/some/namespace");

        // Build the JcrEngine from the configuration...
        JcrEngine engine = configuration.build();
        try {
            // First, start the engine ...
            engine.start();

            // Obtain the JCR Repository ...
            final Repository myRepository = engine.getRepository("My JCR Repository");

            // Create a session to our JCR repository, but do this for each of the following workspaces
            // (where 'null' means the default workspace, as defined by our source) ...
            String[] workspaceNames = {"workspace2", null};

            for (final String workspaceName : workspaceNames) {
                // Log into the JCR repository ...
                Session session = null;
                if (securityContext != null) {
                    // Create a JCR Credentials with our custom security context ...
                    Credentials credentials = new JcrSecurityContextCredentials(securityContext);
                    // And then login ...
                    session = myRepository.login(credentials, workspaceName);
                } else {
                    // We don't have a custom SecurityContext, so we'll rely upon JAAS for our security.
                    // No need to provide a credentials, so just login with the name of the workspace.
                    // However, we DO need to do this from within a doPrivilege block.
                    session = AccessController.doPrivileged(new PrivilegedExceptionAction<Session>() {
                        public Session run() throws Exception {
                            return myRepository.login(workspaceName);
                        }
                    });
                }
                assertThat(session, is(notNullValue()));

                // Now do some not-terribly-interesting stuff ...
                try {
                    // Get the root node ...
                    Node root = session.getRootNode();

                    // Get the "jcr:system" node ...
                    Node jcrSystem = root.getNode("jcr:system");
                    Node namespaces = jcrSystem.getNode("mode:namespaces");
                    assert namespaces != null;

                    // Create a few children under the root node, all with the same name (but different SNS indexes) ...
                    for (int i = 0; i != 10; ++i) {
                        root.addNode("childA", "nt:unstructured");
                    }

                    // Iterate over the children of the root ...
                    int childCount = 0;
                    for (NodeIterator iter = root.getNodes("child* | nonExistant*"); iter.hasNext();) {
                        Node child = iter.nextNode();
                        assertThat(child.getName(), is("childA"));
                        ++childCount;
                    }
                    assertThat(childCount, is(10));

                } finally {
                    // Always log out of the session ...
                    session.logout();
                }
            }

        } catch (PrivilegedActionException e) {
            // Something went wrong ...
            throw e;
        } catch (RepositoryException e) {
            // Something went wrong ...
            throw e;
        } finally {
            // Shutdown the engine ...
            engine.shutdown();

            // Wait at most 5 seconds for the shutdown to complete ...
            try {
                engine.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // We were interrupted, but we don't really care.
                // But since we're eating this exception, we MUST reset the Thread's flag ...
                Thread.interrupted();
            }
        }
    }

    public static class CustomSecurityContext implements SecurityContext {
        private final String username;

        public CustomSecurityContext( String username ) {
            this.username = username;
        }

        public String getUserName() {
            return username;
        }

        public boolean hasRole( String roleName ) {
            return true;
        }

        public void logout() {
        }

    }
}
