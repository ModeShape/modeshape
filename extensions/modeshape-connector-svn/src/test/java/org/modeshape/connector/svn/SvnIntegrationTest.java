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
package org.modeshape.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

public class SvnIntegrationTest {

    private static boolean SVN_SERVER_IS_WRITABLE = false;

    private ExecutionContext context;
    private SvnRepositorySource source;
    private String repositoryUrl;
    private String[] predefinedWorkspaceNames;

    @Before
    public void beforeEach() {
        repositoryUrl = "http://anonsvn.jboss.org/repos/modeshape/";
        // 'sudo svnserve -d -r /usr/local/ --foreground' to start this SVN repo
        // repositoryUrl = "svn://localhost/repo";
        // 'sudo apachectl start' to start this SVN repo
        // repositoryUrl = "http://localhost/repo";

        predefinedWorkspaceNames = new String[] {"trunk", "tags", "branches"};
        context = new ExecutionContext();
        source = new SvnRepositorySource();
        source.setName("svn repository source");
        source.setRepositoryRootUrl(repositoryUrl);
        source.setCreatingWorkspacesAllowed(true);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);

        if (SVN_SERVER_IS_WRITABLE) {
            source.setUpdatesAllowed(true);
            source.setUsername("harry");
            source.setPassword("harryssecret");
        }
        else {
            source.setUsername("anonymous");
            source.setPassword("");
        }

        source.initialize(new RepositoryContext() {

            @Override
            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            @Override
            @SuppressWarnings( "synthetic-access" )
            public ExecutionContext getExecutionContext() {
                return context;
            }

            @Override
            public Observer getObserver() {
                return null;
            }

            @Override
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return new RepositoryConnectionFactory() {

                    @Override
                    public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                        return null;
                    }

                };
            }

        });
    }

    @Test
    public void shouldConnectAndReadRootNode() {
        if (SVN_SERVER_IS_WRITABLE) return;

        Graph graph = Graph.create(source, context);
        Map<Name, Property> properties = graph.getPropertiesByName().on("/");
        assertThat(properties, is(notNullValue()));

        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root.getLocation(), is(notNullValue()));
        assertThat(root.getChildren().isEmpty(), is(false));
        for (Location childLocation : root.getChildren()) {
            assertThat(childLocation.getPath().getParent().isRoot(), is(true));
            // Node child = graph.getNodeAt(childLocation);
            // assertThat(child.getLocation(), is(childLocation));
            // assertThat(child.getLocation().getPath().getParent().isRoot(), is(true));
        }
    }

    /*
     * This test will only pass if the SVN URL above points to a writable URL.
     */
    @Test
    public void shouldConnectAndWriteTwoNodesDeep() {
        if (!SVN_SERVER_IS_WRITABLE) return;

        Graph graph = Graph.create(source, context);
        Map<Name, Property> properties = graph.getPropertiesByName().on("/");
        assertThat(properties, is(notNullValue()));

        Graph.Batch batch = graph.batch();

        batch.create("/testFolder").and();
        batch.create("/testFolder/childFolder").and();
        batch.create("/testFolder/foo.text").with("jcr:primaryType", "nt:file").and();
        batch.create("/testFolder/foo.text/jcr:content").with("jcr:primaryType", "nt:resource").with("jcr:data", "foo").and();

        batch.execute();

        graph.getNodeAt("/testFolder/childFolder");

    }
}
