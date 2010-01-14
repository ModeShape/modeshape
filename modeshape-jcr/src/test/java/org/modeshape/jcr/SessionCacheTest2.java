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
package org.modeshape.jcr;

import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SessionCacheTest2 extends AbstractJcrTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrTest#getResourceNameOfXmlFileToImport()
     */
    @Override
    protected String getResourceNameOfXmlFileToImport() {
        return "vehicles.xml";
    }

    protected void walkInfosForNodesUnder( Node<JcrNodePayload, JcrPropertyPayload> node,
                                           Stopwatch sw ) throws Exception {
        for (Node<JcrNodePayload, JcrPropertyPayload> child : node.getChildren()) {
            sw.start();
            child.getPath();
            sw.stop();

            // Walk the infos for nodes under the child (this is recursive) ...
            walkInfosForNodesUnder(child, sw);
        }
    }

    @Test
    public void shouldFindInfoForAllNodesInGraph() throws Exception {
        for (int i = 0; i != 3; ++i) {
            super.numberOfConnections = 0;
            Stopwatch sw = new Stopwatch();

            // Get the root ...
            sw.start();
            Node<JcrNodePayload, JcrPropertyPayload> root = cache.findNode(null, path("/"));
            root.getPath();
            root.getPayload(); // forces a load
            sw.stop();

            // Walk the infos for nodes under the root (this is recursive) ...
            walkInfosForNodesUnder(root, sw);
            System.out.println("Statistics for walking nodes using SessionCache: " + sw.getSimpleStatistics() + "  -> "
                               + super.numberOfConnections);
        }
    }

    @Test
    public void shouldFindInfoForAllNodesInGraphWithLoadingDepthOf2() throws Exception {
        cache.graphSession().setDepthForLoadingNodes(2);
        for (int i = 0; i != 3; ++i) {
            super.numberOfConnections = 0;
            Stopwatch sw = new Stopwatch();

            // Get the root ...
            sw.start();
            Node<JcrNodePayload, JcrPropertyPayload> root = cache.findNode(null, path("/"));
            root.getPath();
            root.getPayload(); // forces a load
            sw.stop();

            // Walk the infos for nodes under the root (this is recursive) ...
            walkInfosForNodesUnder(root, sw);
            System.out.println("Statistics for walking nodes using SessionCache: " + sw.getSimpleStatistics() + "  -> "
                               + super.numberOfConnections);
        }
    }

    @Test
    public void shouldFindInfoForAllNodesInGraphWithLoadingDepthOf4() throws Exception {
        cache.graphSession().setDepthForLoadingNodes(6);
        for (int i = 0; i != 3; ++i) {
            Stopwatch sw = new Stopwatch();
            super.numberOfConnections = 0;

            // Get the root ...
            sw.start();
            Node<JcrNodePayload, JcrPropertyPayload> root = cache.findNode(null, path("/"));
            root.getPath();
            root.getPayload(); // forces a load
            sw.stop();

            // Walk the infos for nodes under the root (this is recursive) ...
            walkInfosForNodesUnder(root, sw);
            System.out.println("Statistics for walking nodes using SessionCache: " + sw.getSimpleStatistics() + "  -> "
                               + super.numberOfConnections);
        }
    }
}
