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
package org.modeshape.graph.connector.inmemory;

import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.ReadableConnectorTest;

public class InMemoryConnectorReadableTest extends ReadableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("Test Repository");
        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 4;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldReturnSameStructureForRepeatedReadBranchRequestsOnLargeRepository() {
        // Initialize workspace one with some content ...
        String initialPath = "";
        int depth = 4;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Verify that the content doesn't change
        Location root = graph.getCurrentWorkspace().getRoot();
        Subgraph subgraph1 = graph.getSubgraphOfDepth(10).at(root);
        Subgraph subgraph2 = graph.getSubgraphOfDepth(10).at(root);
        assertEquivalentSubgraphs(subgraph1, subgraph2, true, true);
    }

}
