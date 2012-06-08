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
package org.modeshape.test.integration.performance;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.test.ModeShapeSingleUseTest;

public class InMemoryPerformanceTest extends ModeShapeSingleUseTest {

    private static final String LARGE_STRING_VALUE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed fermentum iaculis placerat. Mauris condimentum dapibus pretium. Vestibulum gravida sodales tellus vitae porttitor. Nunc dictum, eros vel adipiscing pellentesque, sem mi iaculis dui, a aliquam neque magna non turpis. Maecenas imperdiet est eu lorem placerat mattis. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum scelerisque molestie tristique. Mauris nibh diam, vestibulum eu condimentum at, facilisis at nisi. Maecenas vehicula accumsan lacus in venenatis. Nulla nisi eros, fringilla at dapibus mollis, pharetra at urna. Praesent in risus magna, at iaculis sapien. Fusce id velit id dui tempor hendrerit semper a nunc. Nam eget mauris tellus.";
    private static final String SMALL_STRING_VALUE = "The quick brown fox jumped over the moon. What? ";

    @Test
    public void shouldAllowSmallSubgraphUsingInMemoryConfig() throws Exception {
        startEngineUsing("config/configRepository.xml");
        sessionTo("My repository");
        assertNode("/", "mode:root");
        repeatedlyCreateSubgraph(5, 2, 10, 7, false, true);
    }

    @Test
    public void shouldAllowSmallSubgraphUsingDiskStorageWithCaching() throws Exception {
        startEngineUsing("config/configRepositoryForDiskStorage.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        repeatedlyCreateSubgraph(5, 2, 10, 7, false, true);
    }

    protected void repeatedlyCreateSubgraph( int samples,
                                             int depth,
                                             int numberOfChildrenPerNode,
                                             int numberOfPropertiesPerNode,
                                             boolean useSns,
                                             boolean print ) throws Exception {
        Session session = session();
        Node node = session.getRootNode().addNode("testArea");
        session.save();

        Stopwatch sw = new Stopwatch();
        if (print) System.out.print("Iterating ");
        int numNodesEach = 0;
        for (int i = 0; i != samples; ++i) {
            System.out.print(".");
            sw.start();
            numNodesEach = createSubgraph(session, node, depth, numberOfChildrenPerNode, numberOfPropertiesPerNode, useSns, 1);
            sw.stop();
            session.save();
        }

        // session.getRootNode().getNode("testArea").remove();
        // session.save();
        // assertThat(session.getRootNode().getNodes().getSize(), is(1L)); // only '/jcr:system'
        if (print) {
            System.out.println();
            System.out.println("Create subgraphs with " + numNodesEach + " nodes each: " + sw.getSimpleStatistics());
        }

        // Now try getting a node at one level down ...
        String name = "childNode";
        int index = numberOfChildrenPerNode / 2;
        String path = useSns ? (name + "[" + index + "]") : (name + index);
        sw.reset();
        sw.start();
        Node randomNode = node.getNode(path);
        sw.stop();
        assertThat(randomNode, is(notNullValue()));
        if (print) {
            System.out.println("Find " + randomNode.getPath() + ": " + sw.getTotalDuration());
        }
    }

    /**
     * Create a structured subgraph by generating nodes with the supplied number of properties and children, to the supplied
     * maximum subgraph depth.
     * 
     * @param session the session that should be used; may not be null
     * @param parentNode the parent node under which the subgraph is to be created
     * @param depthRemaining the depth of the subgraph; must be a positive number
     * @param numberOfChildrenPerNode the number of child nodes to create under each node
     * @param numberOfPropertiesPerNode the number of properties to create on each node; must be 0 or more
     * @param useSns true if the child nodes under a parent should be same-name-siblings, or false if they should each have their
     *        own unique name
     * @param depthToSave
     * @return the number of nodes created in the subgraph
     * @throws RepositoryException if there is a problem
     */
    protected int createSubgraph( Session session,
                                  Node parentNode,
                                  int depthRemaining,
                                  int numberOfChildrenPerNode,
                                  int numberOfPropertiesPerNode,
                                  boolean useSns,
                                  int depthToSave ) throws RepositoryException {
        int numberCreated = 0;
        for (int i = 0; i != numberOfChildrenPerNode; ++i) {
            Node child = parentNode.addNode(useSns ? "childNode" : ("childNode" + i));
            for (int j = 0; j != numberOfPropertiesPerNode; ++j) {
                String value = (i % 10 == 0) ? LARGE_STRING_VALUE : SMALL_STRING_VALUE;
                child.setProperty("property" + j, value);
            }
            numberCreated += numberOfChildrenPerNode;
            if (depthRemaining > 1) {
                numberCreated += createSubgraph(session,
                                                child,
                                                depthRemaining - 1,
                                                numberOfChildrenPerNode,
                                                numberOfPropertiesPerNode,
                                                useSns,
                                                depthToSave);
            }
        }
        if (depthRemaining == depthToSave) {
            session.save();
        }
        return numberCreated;
    }

    protected int calculateTotalNumberOfNodesInTree( int numberOfChildrenPerNode,
                                                     int depth,
                                                     boolean countRoot ) {
        assert depth > 0;
        assert numberOfChildrenPerNode > 0;
        int totalNumber = 0;
        for (int i = 0; i <= depth; ++i) {
            totalNumber += (int)Math.pow(numberOfChildrenPerNode, i);
        }
        return countRoot ? totalNumber : totalNumber - 1;
    }

    protected String getTotalAndAverageDuration( Stopwatch stopwatch,
                                                 long numNodes ) {
        long totalDurationInMilliseconds = TimeUnit.NANOSECONDS.toMillis(stopwatch.getTotalDuration().longValue());
        long avgDuration = totalDurationInMilliseconds / numNodes;
        String units = " millisecond(s)";
        if (avgDuration < 1L) {
            long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotalDuration().longValue());
            avgDuration = totalDurationInMicroseconds / numNodes;
            units = " microsecond(s)";
        }
        return "total = " + stopwatch.getTotalDuration() + "; avg = " + avgDuration + units;
    }

}
