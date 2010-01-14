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
package org.modeshape.connector.store.jpa;

import java.util.UUID;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.modeshape.graph.property.ReferentialIntegrityException;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JpaConnectorWritingTest extends WritableConnectorTest {

    private boolean isReferentialIntegrityEnforced = false;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);

        // Override the inherited properties ...
        source.setReferentialIntegrityEnforced(isReferentialIntegrityEnforced);
        source.setCompressData(true);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        if (!graph.getWorkspaces().contains("default")) {
            graph.createWorkspace().named("default");
        } else {
            graph.useWorkspace("default");
        }
    }

    @Test( expected = ReferentialIntegrityException.class )
    public void shouldNotCopyChildrenBetweenWorkspacesAndRemoveExistingNodesWithSameUuidIfSpecifiedIfReferentialIntegrityIsViolated()
        throws Exception {
        if (!isReferentialIntegrityEnforced) {
            throw new ReferentialIntegrityException(Location.create(UUID.randomUUID()));
        }

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/newUuids").and();
        // Copy once to get the UUID into the default workspace
        // graph.copy("/node1").replacingExistingNodesWithSameUuids().fromWorkspace(workspaceName).to("/newUuids/node1");
        graph.clone("/node1")
             .fromWorkspace(workspaceName)
             .as(name("node1"))
             .into("/newUuids")
             .replacingExistingNodesWithSameUuids();

        // Create a new child node that in the target workspace that has no corresponding node in the source workspace
        graph.create("/newUuids/node1/shouldBeRemoved").and();
        graph.create("/refererringNode").and();
        graph.set("refProp").on("/refererringNode").to(graph.getNodeAt("/newUuids/node1/shouldBeRemoved"));

        // Now create a reference to this new node

        // Copy again to test the behavior now that the UUIDs are already in the default workspace
        // This should fail because /newUuids/node1/shouldBeRemoved must be removed by the copy, but can't be removed
        // because there is a reference to it.
        // graph.copy("/node1").replacingExistingNodesWithSameUuids().fromWorkspace(workspaceName).to("/newUuids/otherNode");
        graph.clone("/node1")
             .fromWorkspace(workspaceName)
             .as(name("otherNode"))
             .into("/newUuids")
             .replacingExistingNodesWithSameUuids();
    }
}
