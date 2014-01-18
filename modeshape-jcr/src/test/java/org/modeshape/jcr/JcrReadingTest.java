/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;

/**
 * Test performance reading graph subtrees of various sizes with varying number of properties
 */
public class JcrReadingTest extends AbstractJcrAccessTest {

    @Test
    public void testReadingTrees() throws Exception {
        int[] breadths = new int[] {10,};
        int[] depths = new int[] {1, 2, 3,};
        int[] properties = new int[] {0, 7, 50};

        print = false;

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
                                     print ? System.out : null,
                                     null);
                }
            }
        }
    }
}
