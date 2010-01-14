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
import org.junit.Test;

/**
 * Test performance reading graph subtrees of various sizes with varying number of properties
 */
public class JcrReadingTest extends AbstractJcrAccessTest {

    @Test
    public void testReadingTrees() throws Exception {
        int[] breadths = new int[] {10,};
        int[] depths = new int[] {1, 2, 3,};
        int[] properties = new int[] {0, 7, 50};

        for (int i = 0; i < breadths.length; i++) {
            for (int j = 0; j < depths.length; j++) {
                for (int k = 0; k < properties.length; k++) {
                    String testName = "/" + breadths[i] + "x" + depths[j] + "x" + properties[k] + "test";
                    session().getRootNode().addNode(testName, "nt:unstructured");
                    createSubgraph(session(), testName, depths[j], breadths[i], properties[k], false, null, null, null);

                    traverseSubgraph(session(),
                                     testName,
                                     depths[j],
                                     breadths[i],
                                     properties[k],
                                     false,
                                     new Stopwatch(),
                                     System.out,
                                     null);
                }
            }
        }
    }
}
