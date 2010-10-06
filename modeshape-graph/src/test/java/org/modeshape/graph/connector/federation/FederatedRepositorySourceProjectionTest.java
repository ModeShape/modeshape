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

import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.Graph;

/**
 * An integration test that verifies the behavior of a {@link FederatedRepositorySource} configured with a federated workspace
 * using various projections to an underlying source.
 */
public class FederatedRepositorySourceProjectionTest extends AbstractFederatedRepositorySourceIntegrationTest {

    private String offsetSourceName;
    private String offsetWorkspaceName;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Set up the projection ...
        offsetSourceName = "Offset Source";
        offsetWorkspaceName = "Offset Workspace";

        // Add some data to the source ...
        Graph source = graphFor(offsetSourceName, offsetWorkspaceName);
        source.importXmlFrom(getClass().getClassLoader().getResource("cars.xml").toURI()).into("/");
    }

    /**
     * Assert that the node in the source repository given by the supplied path represents the equivalent offset node in the
     * federated repository given by a path offset from the supplied path.
     * 
     * @param federatedPath the path in the federated repository
     * @param pathToSourceNode the path to the node in the source repository
     */
    protected void assertSameNode( String federatedPath,
                                   String pathToSourceNode ) {
        assertSameNode(federatedPath, pathToSourceNode, offsetSourceName, offsetWorkspaceName);
    }

    /**
     * Assert that the node in the source repository given by the supplied path does not exist in the source or in the federated
     * repository.
     * 
     * @param federatedPath the path in the federated repository
     * @param pathToSourceNode the path to the node in the source repository
     */
    protected void assertNotFederated( String federatedPath,
                                       String pathToSourceNode ) {
        assertNotFederated(federatedPath, pathToSourceNode, offsetSourceName, offsetWorkspaceName);
    }

    @Test
    public void shouldProjectRootNodeInSourceIntoFederatedUsingOffset() throws Exception {
        // Set up the projection ...
        addProjection("fedSpace", "Offset Projection", offsetSourceName, offsetWorkspaceName, "/v1/v2 => /");
        assertSameNode("/v1/v2/Cars", "/Cars");
        assertSameNode("/v1/v2/Cars/Hybrid", "/Cars/Hybrid");
        assertSameNode("/v1/v2/Cars/Hybrid/Toyota Prius", "/Cars/Hybrid/Toyota Prius");
        assertSameNode("/v1/v2/Cars/Hybrid/Toyota Highlander", "/Cars/Hybrid/Toyota Highlander");
        assertSameNode("/v1/v2/Cars/Hybrid/Nissan Altima", "/Cars/Hybrid/Nissan Altima");
        assertSameNode("/v1/v2/Cars/Sports/Aston Martin DB9", "/Cars/Sports/Aston Martin DB9");
        assertSameNode("/v1/v2/Cars/Sports/Infiniti G37", "/Cars/Sports/Infiniti G37");
        assertSameNode("/v1/v2/Cars/Luxury/Cadillac DTS", "/Cars/Luxury/Cadillac DTS");
        assertSameNode("/v1/v2/Cars/Luxury/Bentley Continental", "/Cars/Luxury/Bentley Continental");
        assertSameNode("/v1/v2/Cars/Luxury/Lexus IS350", "/Cars/Luxury/Lexus IS350");
        assertSameNode("/v1/v2/Cars/Utility/Land Rover LR2", "/Cars/Utility/Land Rover LR2");
        assertSameNode("/v1/v2/Cars/Utility/Land Rover LR3", "/Cars/Utility/Land Rover LR3");
        assertSameNode("/v1/v2/Cars/Utility/Hummer H3", "/Cars/Utility/Hummer H3");
        assertSameNode("/v1/v2/Cars/Utility/Ford F-150", "/Cars/Utility/Ford F-150");
    }

    @Test
    public void shouldProjectNonRootNodeInSourceIntoFederatedUsingOffset() throws Exception {
        // Set up the projection ...
        addProjection("fedSpace", "Offset Projection", offsetSourceName, offsetWorkspaceName, "/v1/v2/Cars => /Cars");
        assertSameNode("/v1/v2/Cars", "/Cars");
        assertSameNode("/v1/v2/Cars/Hybrid", "/Cars/Hybrid");
        assertSameNode("/v1/v2/Cars/Hybrid/Toyota Prius", "/Cars/Hybrid/Toyota Prius");
        assertSameNode("/v1/v2/Cars/Hybrid/Toyota Highlander", "/Cars/Hybrid/Toyota Highlander");
        assertSameNode("/v1/v2/Cars/Hybrid/Nissan Altima", "/Cars/Hybrid/Nissan Altima");
        assertSameNode("/v1/v2/Cars/Sports/Aston Martin DB9", "/Cars/Sports/Aston Martin DB9");
        assertSameNode("/v1/v2/Cars/Sports/Infiniti G37", "/Cars/Sports/Infiniti G37");
        assertSameNode("/v1/v2/Cars/Luxury/Cadillac DTS", "/Cars/Luxury/Cadillac DTS");
        assertSameNode("/v1/v2/Cars/Luxury/Bentley Continental", "/Cars/Luxury/Bentley Continental");
        assertSameNode("/v1/v2/Cars/Luxury/Lexus IS350", "/Cars/Luxury/Lexus IS350");
        assertSameNode("/v1/v2/Cars/Utility/Land Rover LR2", "/Cars/Utility/Land Rover LR2");
        assertSameNode("/v1/v2/Cars/Utility/Land Rover LR3", "/Cars/Utility/Land Rover LR3");
        assertSameNode("/v1/v2/Cars/Utility/Hummer H3", "/Cars/Utility/Hummer H3");
        assertSameNode("/v1/v2/Cars/Utility/Ford F-150", "/Cars/Utility/Ford F-150");
    }

    @Test
    public void shouldProjectNonRootNodeWithSiblingsInSourceIntoFederatedUsingOffset() throws Exception {
        // Set up the projection ...
        addProjection("fedSpace", "Offset Projection", offsetSourceName, offsetWorkspaceName, "/v1/v2/Lux => /Cars/Luxury");
        assertSameNode("/v1/v2/Lux/Cadillac DTS", "/Cars/Luxury/Cadillac DTS");
        assertSameNode("/v1/v2/Lux/Bentley Continental", "/Cars/Luxury/Bentley Continental");
        assertSameNode("/v1/v2/Lux/Lexus IS350", "/Cars/Luxury/Lexus IS350");
        // Car should not be projected ...
        assertNotFederated("/v1/Car", "/Cars");
        assertNotFederated("/v1/v2/Car", "/Cars");
        // The other children of Car should not be projected ...
        assertNotFederated("/v1/v2/Hybrid", "/Cars/Hybrid");
        assertNotFederated("/v1/v2/Sports", "/Cars/Sports");
        assertNotFederated("/v1/v2/Utility", "/Cars/Utility");
    }
}
