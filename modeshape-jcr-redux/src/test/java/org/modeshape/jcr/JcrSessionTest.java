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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;

public class JcrSessionTest extends SingleUseAbstractTest {

    @Test
    public void shouldHaveRootNode() throws Exception {
        JcrRootNode node = session.getRootNode();
        assertThat(node, is(notNullValue()));
        assertThat(node.getPath(), is("/"));
    }

    @Test
    public void shouldHaveJcrSystemNodeUnderRoot() throws Exception {
        JcrRootNode node = session.getRootNode();
        Node system = node.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
        assertThat(system.getPath(), is("/jcr:system"));
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithSameNameSiblings() throws Exception {
        JcrRootNode node = session.getRootNode();
        int count = 10000;
        long start1 = System.nanoTime();
        for (int i = 0; i != count; ++i) {
            node.addNode("childNode");
        }
        long millis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start1, TimeUnit.NANOSECONDS);
        System.out.println("Time to create " + count + " nodes under root: " + millis + " ms");

        long start2 = System.nanoTime();
        session.save();
        millis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start2, TimeUnit.NANOSECONDS);
        System.out.println("Time to save " + count + " new nodes: " + millis + " ms");
        millis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start1, TimeUnit.NANOSECONDS);
        System.out.println("Total time to create " + count + " new nodes and save: " + millis + " ms");

        NodeIterator iter = node.getNodes("childNode");
        assertThat(iter.getSize(), is((long)count));
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            assertThat(child.getPrimaryNodeType().getName(), is("nt:unstructured"));
        }

        // Now add another node ...
        start1 = System.nanoTime();
        node.addNode("oneMore");
        session.save();
        millis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start1, TimeUnit.NANOSECONDS);
        System.out.println("Time to create " + (count + 1) + "th node and save: " + millis + " ms");
    }

    @Test
    public void shouldAllowCreatingNodeUnderUnsavedNode() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithNoSameNameSiblings() throws Exception {
        Stopwatch sw = new Stopwatch();
        for (int i = 0; i != 15; ++i) {
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            int count = 100;
            if (i > 2) sw.start();
            for (int j = 0; j != count; ++j) {
                node.addNode("childNode" + j);
            }

            session.save();
            if (i > 2) sw.stop();

            // Now add another node ...
            node.addNode("oneMore");
            session.save();

            session.getRootNode().getNode("testNode").remove();
            session.save();
        }
        System.out.println(sw.getDetailedStatistics());
    }
}
