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
package org.jboss.dna.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Map;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;
import org.junit.Before;
import org.junit.Test;

public class SVNIntegrationTest {

    private ExecutionContext context;
    private SVNRepositorySource source;
    private String repositoryUrl;
    private String[] predefinedWorkspaceNames;

    @Before
    public void beforeEach() {
        repositoryUrl = "http://anonsvn.jboss.org/repos/dna/";
        predefinedWorkspaceNames = new String[] {repositoryUrl + "trunk", repositoryUrl + "tags", repositoryUrl + "branches"};
        context = new ExecutionContext();
        source = new SVNRepositorySource();
        source.setName("svn repository source");
        source.setRepositoryRootURL(repositoryUrl);
        source.setUsername("anonymous");
        source.setPassword("");
        source.setCreatingWorkspacesAllowed(true);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDirectoryForDefaultWorkspace(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
    }

    @Test
    public void shouldConnectAndReadRootNode() {
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
}
