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
import org.junit.Before;
import org.junit.Test;

/**
 * An integration test that verifies the behavior of a {@link FederatedRepositorySource} configured with a federated workspace
 * using a single mirror projection to an underlying source.
 */
public class FederatedRepositorySourceUsingMirrorProjectionTest extends AbstractFederatedRepositorySourceIntegrationTest {

    private String mirrorSourceName;
    private String mirrorWorkspaceName;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Set up the projection ...
        mirrorSourceName = "Mirror Source";
        mirrorWorkspaceName = "Mirror Workspace";
        addProjection("fedSpace", "Mirror Projection", mirrorSourceName, mirrorWorkspaceName, "/ => /");

        // Add some data to the source ...
        Graph source = graphFor(mirrorSourceName, mirrorWorkspaceName);
        source.importXmlFrom(getClass().getClassLoader().getResource("cars.xml").toURI()).into("/");
    }

    /**
     * Assert that the node in the federated repository given by the supplied path represents the same node in the underlying
     * mirror source given by the same path.
     * 
     * @param pathToNode the path to the node in the federated repository and in the underlying source
     */
    protected void assertSameNode( String pathToNode ) {
        assertSameNode(pathToNode, pathToNode, mirrorSourceName, mirrorWorkspaceName);
    }

    /**
     * Assert that the node in the source repository given by the supplied path does not exist in the source or in the federated
     * repository.
     * 
     * @param pathToNode the path to the node in the federated repository and in the underlying source
     */
    protected void assertNoNode( String pathToNode ) {
        assertNoNode(pathToNode, pathToNode, mirrorSourceName, mirrorWorkspaceName);
    }

    @Test
    public void shouldListAllFederatedWorkspaces() {
        Set<String> workspaces = federated.getWorkspaces();
        assertThat(workspaces.contains("fedSpace"), is(true));
        assertThat(workspaces.size(), is(1));
    }

    @Test
    public void shouldFederateNodesInMirrorSource() {
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

    @Test
    public void shouldCreateNodeUnderRootInMirrorSource() {
        federated.createAt("/Hovercraft").with("prop1", "value1").and();
        assertSameNode("/Hovercraft");
        // And make sure the parent node is the same ...
        assertSameNode("/");
    }

    @Test
    public void shouldCreateNodeWellBelowRootInMirrorSource() {
        federated.createAt("/Cars/Hybrid/MyNewHybrid").with("prop1", "value1").and();
        assertSameNode("/Cars/Hybrid/MyNewHybrid");
        // And make sure the parent node is the same ...
        assertSameNode("/Cars/Hybrid");
    }

    @Test
    public void shouldDeleteNodeUnderRootFromMirrorSource() {
        // Create a node that we can delete ...
        federated.createAt("/Hovercraft").with("prop1", "value1").and();
        assertSameNode("/Hovercraft");
        federated.delete("/Hovercraft");
        assertNoNode("/Hovercraft");
        // And make sure the parent node is the same ...
        assertSameNode("/");
        // Delete the cars node (which is everything) ...
        federated.delete("/Cars");
        // And make sure the parent node is the same ...
        assertSameNode("/");
    }

    @Test
    public void shouldDeleteNodeWellBelowRootFromMirrorSource() {
        federated.delete("/Cars/Luxury/Cadillac DTS");
        assertNoNode("/Cars/Luxury/Cadillac DTS");
        // And make sure the parent node is the same ...
        assertSameNode("/Cars/Luxury");
    }
}
