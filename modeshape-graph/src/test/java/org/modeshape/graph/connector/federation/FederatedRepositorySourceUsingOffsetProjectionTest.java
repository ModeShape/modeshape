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
package org.modeshape.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Set;
import org.modeshape.graph.Graph;
import org.modeshape.graph.request.UnsupportedRequestException;
import org.junit.Before;
import org.junit.Test;

/**
 * An integration test that verifies the behavior of a {@link FederatedRepositorySource} configured with a federated workspace
 * using a single mirror projection to an underlying source.
 */
public class FederatedRepositorySourceUsingOffsetProjectionTest extends AbstractFederatedRepositorySourceIntegrationTest {

    private String offsetSourceName;
    private String offsetWorkspaceName;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Set up the projection ...
        offsetSourceName = "Offset Source";
        offsetWorkspaceName = "Offset Workspace";
        addProjection("fedSpace", "Offset Projection", offsetSourceName, offsetWorkspaceName, "/a/b => /");

        // Add some data to the source ...
        Graph source = graphFor(offsetSourceName, offsetWorkspaceName);
        source.importXmlFrom(getClass().getClassLoader().getResource("cars.xml").toURI()).into("/");
    }

    /**
     * Assert that the node in the source repository given by the supplied path represents the equivalent offset node in the
     * federated repository given by a path offset from the supplied path.
     * 
     * @param pathToSourceNode the path to the node in the source repository
     */
    protected void assertSameNode( String pathToSourceNode ) {
        String pathToFedNode = "/a/b" + pathToSourceNode;
        assertSameNode(pathToFedNode, pathToSourceNode, offsetSourceName, offsetWorkspaceName);
    }

    /**
     * Assert that the node in the source repository given by the supplied path does not exist in the source and that the
     * equivalent offset node in the federated repository also does not exist.
     * 
     * @param pathToSourceNode the path to the node in the source repository
     */
    protected void assertNoNode( String pathToSourceNode ) {
        String pathToFedNode = "/a/b" + pathToSourceNode;
        assertNoNode(pathToFedNode, pathToSourceNode, offsetSourceName, offsetWorkspaceName);
    }

    @Test
    public void shouldListAllFederatedWorkspaces() {
        Set<String> workspaces = federated.getWorkspaces();
        assertThat(workspaces.contains("fedSpace"), is(true));
        assertThat(workspaces.size(), is(1));
    }

    @Test
    public void shouldFederateNodesInOffsetSource() {
        assertSameNode("/");
        assertSameNode("/Cars");
        assertSameNode("/Cars/Hybrid");
        assertSameNode("/Cars/Hybrid/Toyota Prius");
        assertSameNode("/Cars/Hybrid/Toyota Highlander");
        assertSameNode("/Cars/Hybrid/Nissan Altima");
        assertSameNode("/Cars/Sports/Aston Martin DB9");
        assertSameNode("/Cars/Sports/Infiniti G37");
        assertSameNode("/Cars/Luxury/Cadillac DTS");
        assertSameNode("/Cars/Luxury/Bentley Continental");
        assertSameNode("/Cars/Luxury/Lexus IS350");
        assertSameNode("/Cars/Utility/Land Rover LR2");
        assertSameNode("/Cars/Utility/Land Rover LR3");
        assertSameNode("/Cars/Utility/Hummer H3");
        assertSameNode("/Cars/Utility/Ford F-150");
    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNodeAllowCreatingNodeWithinOffset() {
        // This is not below the offset '/a/b' and should therefore fail ...
        federated.createAt("/a/Hovercraft").with("prop1", "value1").and();
    }

    @Test
    public void shouldCreateNodeUnderRootInOffsetSource() {
        // Create the node below the offset ...
        federated.createAt("/a/b/Hovercraft").with("prop1", "value1").and();
        // Verify it is the same from the federation and source ...
        assertSameNode("/Hovercraft");
        // And make sure the parent node is the same ...
        assertSameNode("/");
    }

    @Test
    public void shouldCreateNodeWellBelowRootInOffsetSource() {
        // Create the node below the offset ...
        federated.createAt("/a/b/Cars/Hybrid/MyNewHybrid").with("prop1", "value1").and();
        // Verify it is the same from the federation and source ...
        assertSameNode("/Cars/Hybrid/MyNewHybrid");
        // And make sure the parent node is the same ...
        assertSameNode("/Cars/Hybrid");
    }

    @Test
    public void shouldAllowDeletingNodeAtBottomOfOffset() {
        // This is not below the offset '/a/b' and should therefore fail ...
        federated.delete("/a/b");
        // This deletes everything, and recreates /a/b (since it is a placeholder) ...
        assertSameNode("/");
        assertThat(federated.getChildren().of("/a/b").size(), is(0));
    }

    @Test
    public void shouldNodeAllowDeletingNodeWithinOffset() {
        federated.delete("/a");
        assertSameNode("/");
        // All nodes should be removed, but the placeholders should remain ...
        assertThat(federated.getChildren().of("/a").size(), is(1));
        assertThat(federated.getChildren().of("/a/b").size(), is(0));
    }

    @Test
    public void shouldDeleteNodeUnderRootInOffsetSource() {
        // Create the node below the offset ...
        federated.createAt("/a/b/Hovercraft").with("prop1", "value1").and();
        // Verify it is the same from the federation and source ...
        assertSameNode("/Hovercraft");
        // And make sure the parent node is the same ...
        assertSameNode("/");
        // Delete the node below the offset ...
        federated.delete("/a/b/Hovercraft");
        // Verify it is the same from the federation and source ...
        assertNoNode("/Hovercraft");
        assertSameNode("/");
        // Delete the node below the offset ...
        federated.delete("/a/b/Cars");
        // Verify it is the same from the federation and source ...
        assertSameNode("/");
    }

    @Test
    public void shouldDeleteNodeWellBelowRootInOffsetSource() {
        // Delete the node below the offset ...
        federated.delete("/a/b/Cars/Luxury/Cadillac DTS");
        assertNoNode("/Cars/Luxury/Cadillac DTS");
        // And make sure the parent node is the same ...
        assertSameNode("/Cars/Luxury");
    }
}
