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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Set;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.junit.Before;
import org.junit.Test;

/**
 * An integration test that verifies the behavior of a {@link FederatedRepositorySource} configured with a federated workspace
 * using a single mirror projection to an underlying source.
 */
public class FederatedRepositorySourceUsingMirrorAndBranchProjectionsTest
    extends AbstractFederatedRepositorySourceIntegrationTest {

    private String mirrorSourceName;
    private String mirrorWorkspaceName;
    private String branchSourceName;
    private String branchWorkspaceName;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Set up the mirror projection ...
        mirrorSourceName = "Mirror Source";
        mirrorWorkspaceName = "Mirror Workspace";
        addProjection("fedSpace", "Mirror Projection", mirrorSourceName, mirrorWorkspaceName, "/ => /");

        // Set up the branch projection ...
        branchSourceName = mirrorSourceName;
        branchWorkspaceName = "Branch Workspace";
        addProjection("fedSpace", "Branch Projection", branchSourceName, branchWorkspaceName, "/Aircraft => /Aircraft");

        // Add some data to the mirror source ...
        Graph source = graphFor(mirrorSourceName, mirrorWorkspaceName);
        source.importXmlFrom(getClass().getClassLoader().getResource("cars.xml").toURI()).into("/");

        // Add some data to the mirror source ...
        Graph branch = graphFor(branchSourceName, branchWorkspaceName);
        branch.importXmlFrom(getClass().getClassLoader().getResource("aircraft.xml").toURI()).into("/");
    }

    /**
     * Assert that the node in the federated repository given by the supplied path represents the same node in the underlying
     * mirror source given by the same path.
     * 
     * @param pathToNode the path to the node in the federated repository and in the underlying source
     */
    protected void assertMirrorNode( String pathToNode ) {
        assertSameNode(pathToNode, pathToNode, mirrorSourceName, mirrorWorkspaceName);
    }

    /**
     * Assert that the node in the federated repository given by the supplied path represents the same node in the underlying
     * branch source given by the same path.
     * 
     * @param pathToNode the path to the node in the federated repository and in the underlying source
     */
    protected void assertBranchNode( String pathToNode ) {
        assertThat(pathToNode.startsWith("/Aircraft"), is(true));
        assertSameNode(pathToNode, pathToNode, branchSourceName, branchWorkspaceName);
    }

    /**
     * Assert that the node does not exist in the federated repository nor in the mirror source or branch source.
     * 
     * @param pathToNode the path to the node in the federated repository and in the underlying source
     */
    protected void assertNoNode( String pathToNode ) {
        assertNoNode(pathToNode, pathToNode, mirrorSourceName, mirrorWorkspaceName);
        assertNoNode(pathToNode, pathToNode, branchSourceName, branchWorkspaceName);
    }

    @Test
    public void shouldListAllFederatedWorkspaces() {
        Set<String> workspaces = federated.getWorkspaces();
        assertThat(workspaces.contains("fedSpace"), is(true));
        assertThat(workspaces.size(), is(1));
    }

    @Test
    public void shouldReturnRootNodeInMirrorProjection() {
        // Get the root node of the mirror projection using the actual graph ...
        Graph mirrorGraph = graphFor(mirrorSourceName, mirrorWorkspaceName);
        Node mirrorRoot = mirrorGraph.getNodeAt("/");
        Location mirrorRootLocation = mirrorRoot.getLocation();
        assertThat(mirrorRootLocation.hasIdProperties(), is(true));
        assertThat(mirrorRootLocation.getUuid(), is(notNullValue()));

        // Get the same node through the federation source ...
        Node fedRoot = federated.getNodeAt("/");
        Location fedRootLocation = fedRoot.getLocation();
        assertThat(fedRootLocation.hasIdProperties(), is(true));
        assertThat(fedRootLocation.getUuid(), is(notNullValue()));

        // The UUIDs of the nodes must be the same ...
        assertThat(fedRootLocation.getUuid(), is(mirrorRootLocation.getUuid()));
    }

    @Test
    public void shouldReturnRootNodeInBranchProjection() {
        // Get the top node of the branch projection using the actual graph ...
        Graph branchGraph = graphFor(branchSourceName, branchWorkspaceName);
        Node branchNode = branchGraph.getNodeAt("/Aircraft");
        Location branchNodeLocation = branchNode.getLocation();
        assertThat(branchNodeLocation.hasIdProperties(), is(true));
        assertThat(branchNodeLocation.getUuid(), is(notNullValue()));

        // Get the same node through the federation source ...
        Node fedNode = federated.getNodeAt("/Aircraft");
        Location fedNodeLocation = fedNode.getLocation();
        assertThat(fedNodeLocation.hasIdProperties(), is(true));
        assertThat(fedNodeLocation.getUuid(), is(notNullValue()));

        // The UUIDs of the nodes must be the same ...
        assertThat(fedNodeLocation.getUuid(), is(branchNodeLocation.getUuid()));
    }

    @Test
    public void shouldFederateRootNodeFromMirrorAndBranch() {
        // The root of the federated repository should have the content from the mirror but should have
        // an additional child that is the root of the branch ...
        assertSameNode("/", "/", mirrorSourceName, mirrorWorkspaceName, "Aircraft");
    }

    @Test
    public void shouldFederateNodesInMirrorSource() {
        assertMirrorNode("/Cars");
        assertMirrorNode("/Cars/Hybrid");
        assertMirrorNode("/Cars/Hybrid/Toyota Prius");
        assertMirrorNode("/Cars/Hybrid/Toyota Highlander");
        assertMirrorNode("/Cars/Hybrid/Nissan Altima");
        assertMirrorNode("/Cars/Sports/Aston Martin DB9");
        assertMirrorNode("/Cars/Sports/Infiniti G37");
        assertMirrorNode("/Cars/Luxury/Cadillac DTS");
        assertMirrorNode("/Cars/Luxury/Bentley Continental");
        assertMirrorNode("/Cars/Luxury/Lexus IS350");
        assertMirrorNode("/Cars/Utility/Land Rover LR2");
        assertMirrorNode("/Cars/Utility/Land Rover LR3");
        assertMirrorNode("/Cars/Utility/Hummer H3");
        assertMirrorNode("/Cars/Utility/Ford F-150");
    }

    @Test
    public void shouldFederateNodesInBranchSource() {
        assertBranchNode("/Aircraft");
        assertBranchNode("/Aircraft/Business");
        assertBranchNode("/Aircraft/Business/Gulfstream V");
        assertBranchNode("/Aircraft/Business/Learjet 45");
    }

    @Test
    public void shouldPerformBatchOperationsAgainstFederatedSource() {
        assertReadUsingBatch("/Aircraft", "/Aircraft/Business", "/Cars");
    }

    @Test
    public void shouldCreateNodeUnderRootInMirrorSource() {
        federated.createAt("/Hovercraft").with("prop1", "value1").and();
        assertMirrorNode("/Hovercraft");
        // And make sure the parent node is the same ...
        assertSameNode("/", "/", mirrorSourceName, mirrorWorkspaceName, "Aircraft");
    }

    @Test
    public void shouldCreateNodeWellBelowRootInMirrorSource() {
        federated.createAt("/Cars/Hybrid/MyNewHybrid").with("prop1", "value1").and();
        assertMirrorNode("/Cars/Hybrid/MyNewHybrid");
        // And make sure the parent node is the same ...
        assertMirrorNode("/Cars/Hybrid");
    }

    @Test
    public void shouldCreateNodeUnderRootInBranchSource() {
        federated.createAt("/Aircraft/Hovercraft").with("prop1", "value1").and();
        assertBranchNode("/Aircraft/Hovercraft");
        // And make sure the parent node is the same ...
        assertBranchNode("/Aircraft");
    }

    @Test
    public void shouldCreateNodeWellBelowRootInBranchSource() {
        federated.createAt("/Aircraft/Business/HondaJet").with("prop1", "value1").and();
        assertBranchNode("/Aircraft/Business/HondaJet");
        // And make sure the parent node is the same ...
        assertBranchNode("/Aircraft/Business");
    }

    @Test
    public void shouldDeleteNodeUnderRootInMirrorSource() {
        // Create a new node (since there's only one node under the root) ...
        federated.createAt("/Hovercraft").with("prop1", "value1").and();
        assertMirrorNode("/Hovercraft");
        assertSameNode("/", "/", mirrorSourceName, mirrorWorkspaceName, "Aircraft");
        // Now delete it ...
        federated.delete("/Hovercraft");
        assertNoNode("/Hovercraft");
    }

    @Test
    public void shouldDeleteNodeWellBelowRootInMirrorSource() {
        federated.delete("/Cars/Luxury/Cadillac DTS");
        assertNoNode("/Cars/Luxury/Cadillac DTS");
        assertMirrorNode("/Cars/Luxury/Lexus IS350");
        assertMirrorNode("/Cars/Luxury");
    }

    @Test
    public void shouldDeleteNodeUnderRootInBranchSource() {
        federated.delete("/Aircraft/Business");
        assertNoNode("/Aircraft/Business");
        assertBranchNode("/Aircraft");
        assertBranchNode("/Aircraft/Commercial");
    }

    @Test
    public void shouldDeleteNodeWellBelowRootInBranchSource() {
        federated.delete("/Aircraft/Business/Learjet 45");
        assertNoNode("/Aircraft/Business/Learjet 45");
        assertBranchNode("/Aircraft/Business/Gulfstream V");
        assertBranchNode("/Aircraft/Business");
        assertBranchNode("/Aircraft/Commercial");
    }

    @Test
    public void shouldDeleteEverythingInMirrorAndOffsetIfDeletingRoot() {
        federated.delete("/");
        // The branch node should still exist, since it's a placeholder ...
        assertThat(federated.getChildren().of("/").size(), is(1));
        assertBranchNode("/Aircraft");
    }
}
