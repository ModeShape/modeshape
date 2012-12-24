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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import org.junit.Before;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

public abstract class AbstractJcrRepositoryTest extends AbstractTransactionalTest {

    protected boolean print;

    @Before
    public void beforeEach() throws Exception {
        print = false;
    }

    protected abstract JcrRepository repository();

    protected abstract JcrSession session();

    protected Path path( String path ) {
        return session().context().getValueFactories().getPathFactory().create(path);
    }

    protected String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected String asString( Object value ) {
        return session().context().getValueFactories().getStringFactory().create(value);
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat("Did not expect to find '" + path + "'", session().getRootNode().hasNode(relativePath(path)), is(false));
        try {
            session().getNode(path);
            fail("Did not expect to find node at \"" + path + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected Node assertNode( String path ) throws RepositoryException {
        if (print && !session().getRootNode().hasNode(path)) {
            // We won't find the node, so print out the information ...
            Node parent = session().getRootNode();
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }

        Node node = session().getNode(path);
        assertThat(node, is(notNullValue()));
        // Verify that the path can be found via navigating ...
        if (path.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(session().getRootNode(), is(notNullValue()));
        } else {

        }
        return node;
    }

    protected Node assertNode( String path,
                               String primaryType ) throws RepositoryException {
        Node node = assertNode(path);
        assertEquals(primaryType, node.getPrimaryNodeType().getName());
        return node;
    }

    protected void assertSameProperties( Node node1,
                                         Node node2,
                                         String... excludedPropertyNames ) throws RepositoryException {
        Set<String> excludedNames = new HashSet<String>(Arrays.asList(excludedPropertyNames));
        Set<String> node2Names = new HashSet<String>();

        // Find the names of all (non-excluded) proeprties in node 2 ...
        PropertyIterator iter = node2.getProperties();
        while (iter.hasNext()) {
            Property prop2 = iter.nextProperty();
            node2Names.add(prop2.getName());
        }
        node2Names.removeAll(excludedNames);

        iter = node1.getProperties();
        while (iter.hasNext()) {
            Property prop1 = iter.nextProperty();
            String name = prop1.getName();
            if (excludedNames.contains(name)) continue;
            Property prop2 = node2.getProperty(prop1.getName());
            assertThat(prop1.isMultiple(), is(prop2.isMultiple()));
            if (prop1.isMultiple()) {
                Value[] values1 = prop1.getValues();
                Value[] values2 = prop2.getValues();
                assertThat(values1, is(values2));
            } else {
                assertThat(prop1.getValue().getString(), is(prop2.getValue().getString()));
            }
            node2Names.remove(name);
        }

        // There should be no more properties left ...
        if (!node2Names.isEmpty()) {
            fail("Found extra properties in node2: " + node2Names);
        }
    }

    protected void addMixinRecursively( String path,
                                        String... nodeTypes ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        addMixin(node, true, nodeTypes);
    }

    protected Node addMixin( String path,
                             String... nodeTypes ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        return addMixin(node, false, nodeTypes);
    }

    protected Node addMixin( Node node,
                             boolean recursive,
                             String... nodeTypes ) throws RepositoryException {
        assertThat(node, is(notNullValue()));
        for (String nodeType : nodeTypes) {
            if (!hasMixin(node, nodeType)) {
                node.addMixin(nodeType);
            }
        }
        if (recursive) {
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                addMixin(children.nextNode(), true, nodeTypes);
            }
        }
        return node;
    }

    protected boolean hasMixin( Node node,
                                String mixinNodeType ) throws RepositoryException {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.getName().equals(mixinNodeType)) return true;
        }
        return false;
    }

    protected void printMessage( String message ) {
        if (print) {
            System.out.println(message);
        }
    }

    protected void printDetails( Node node ) throws RepositoryException {
        if (print) {
            new JcrTools().printNode(node);
        }
    }

    protected void print() throws RepositoryException {
        print(session().getRootNode(), true);
    }

    private Node nodeFor( String path ) throws RepositoryException {
        if (path.equals("/")) {
            return session().getRootNode();
        }
        return session().getRootNode().getNode(relativePath(path));
    }

    protected void print( String path ) throws RepositoryException {
        print(nodeFor(path), true);
    }

    protected void print( String path,
                          boolean includeSystem ) throws RepositoryException {
        print(nodeFor(path), includeSystem, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected void print( String path,
                          boolean includeSystem,
                          int maxNumberOfChildren ) throws RepositoryException {
        print(nodeFor(path), includeSystem, maxNumberOfChildren, Integer.MAX_VALUE);
    }

    protected void print( Node node ) throws RepositoryException {
        print(node, true);
    }

    protected void print( Node node,
                          boolean includeSystem ) throws RepositoryException {
        print(node, includeSystem, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected void print( Node node,
                          boolean includeSystem,
                          int maxNumberOfChildren ) throws RepositoryException {
        print(node, includeSystem, maxNumberOfChildren, Integer.MAX_VALUE);
    }

    protected void print( Node node,
                          boolean includeSystem,
                          int maxNumberOfChildren,
                          int depth ) throws RepositoryException {
        if (print && depth > 0) {
            if (!includeSystem && node.getPath().equals("/jcr:system")) return;
            if (node.getDepth() != 0) {
                int snsIndex = node.getIndex();
                String segment = node.getName() + (snsIndex > 1 ? ("[" + snsIndex + "]") : "");
                System.out.println(StringUtil.createString(' ', 2 * node.getDepth()) + '/' + segment);
            }
            int nextDepth = depth - 1;
            if (nextDepth <= 0) return;
            NodeIterator children = node.getNodes();
            int count = 0;
            while (children.hasNext()) {
                if (count >= maxNumberOfChildren) {
                    System.out.println(StringUtil.createString(' ', 2 * (node.getDepth() + 1)) + "...");
                    break;
                }
                print(children.nextNode(), includeSystem, maxNumberOfChildren, nextDepth);
                ++count;
            }
        }
    }

    protected void navigate( String path ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        navigate(node, true);
    }

    protected void navigate( Node node ) throws RepositoryException {
        navigate(node, true);
    }

    protected void navigate( Node node,
                             boolean includeSystem ) throws RepositoryException {
        navigate(node, includeSystem, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected void navigate( Node node,
                             boolean includeSystem,
                             int maxNumberOfChildren ) throws RepositoryException {
        navigate(node, includeSystem, maxNumberOfChildren, Integer.MAX_VALUE);
    }

    protected void navigate( Node node,
                             boolean includeSystem,
                             int maxNumberOfChildren,
                             int depth ) throws RepositoryException {
        if (depth > 0) {
            if (!includeSystem && node.getPath().equals("/jcr:system")) return;
            if (node.getDepth() != 0) {
                int snsIndex = node.getIndex();
                String segment = node.getName() + (snsIndex > 1 ? ("[" + snsIndex + "]") : "");
                if (print) System.out.println(StringUtil.createString(' ', 2 * node.getDepth()) + '/' + segment);
            }
            int nextDepth = depth - 1;
            if (nextDepth <= 0) return;
            NodeIterator children = node.getNodes();
            int count = 0;
            while (children.hasNext()) {
                if (count >= maxNumberOfChildren) {
                    if (print) System.out.println(StringUtil.createString(' ', 2 * (node.getDepth() + 1)) + "...");
                    break;
                }
                navigate(children.nextNode(), includeSystem, maxNumberOfChildren, nextDepth);
                ++count;
            }
        }
    }

    private static String getRandomString( int length ) {
        StringBuffer buff = new StringBuffer(length);

        for (int i = 0; i < length; i++) {
            buff.append((char)((Math.random() * 26) + 'a'));
        }

        return buff.toString();
    }

    private static int createChildren( Node parent,
                                       int numProperties,
                                       int width,
                                       int depth ) throws Exception {
        if (depth < 1) {
            return 0;

        }

        int count = width;

        for (int i = 0; i < width; i++) {
            Node newNode = parent.addNode(getRandomString(9), "nt:unstructured");

            for (int j = 0; j < numProperties; j++) {
                newNode.setProperty(getRandomString(8), getRandomString(16));
            }

            count += createChildren(newNode, numProperties, width, depth - 1);
        }
        return count;
    }

    protected static int createSubgraph( JcrSession session,
                                         String initialPath,
                                         int depth,
                                         int numberOfChildrenPerNode,
                                         int numberOfPropertiesPerNode,
                                         boolean oneBatch,
                                         Stopwatch stopwatch,
                                         PrintStream output,
                                         String description ) throws Exception {
        // Calculate the number of nodes that we'll created, but subtract 1 since it doesn't create the root
        long totalNumber = calculateTotalNumberOfNodesInTree(numberOfChildrenPerNode, depth, false);
        if (initialPath == null) initialPath = "";
        if (description == null) {
            description = "" + numberOfChildrenPerNode + "x" + depth + " tree with " + numberOfPropertiesPerNode
                          + " properties per node";
        }

        if (output != null) output.println(description + " (" + totalNumber + " nodes):");
        long totalNumberCreated = 0;

        Node parentNode = session.getNode(initialPath);

        if (stopwatch != null) stopwatch.start();

        totalNumberCreated += createChildren(parentNode, numberOfPropertiesPerNode, numberOfChildrenPerNode, depth);

        assertThat(totalNumberCreated, is(totalNumber));

        session.save();

        if (stopwatch != null) {
            stopwatch.stop();
            if (output != null) {
                output.println("    " + getTotalAndAverageDuration(stopwatch, totalNumberCreated));
            }
        }
        return (int)totalNumberCreated;

    }

    protected static int traverseSubgraph( JcrSession session,
                                           String initialPath,
                                           int depth,
                                           int numberOfChildrenPerNode,
                                           int numberOfPropertiesPerNode,
                                           boolean oneBatch,
                                           Stopwatch stopwatch,
                                           PrintStream output,
                                           String description ) throws Exception {
        // Calculate the number of nodes that we'll created, but subtract 1 since it doesn't create the root
        long totalNumber = calculateTotalNumberOfNodesInTree(numberOfChildrenPerNode, depth, false);
        if (initialPath == null) initialPath = "";
        if (description == null) {
            description = "" + numberOfChildrenPerNode + "x" + depth + " tree with " + numberOfPropertiesPerNode
                          + " properties per node";
        }

        if (output != null) output.println(description + " (" + totalNumber + " nodes):");
        long totalNumberTraversed = 0;

        Node parentNode = session.getNode(initialPath);

        if (stopwatch != null) stopwatch.start();

        totalNumberTraversed += traverseChildren(parentNode);

        assertThat(totalNumberTraversed, is(totalNumber));

        session.save();

        if (stopwatch != null) {
            stopwatch.stop();
            if (output != null) {
                output.println("    " + getTotalAndAverageDuration(stopwatch, totalNumberTraversed));
            }
        }
        return (int)totalNumberTraversed;

    }

    protected static int traverseChildren( Node parentNode ) throws Exception {

        int childCount = 0;
        NodeIterator children = parentNode.getNodes();

        while (children.hasNext()) {
            childCount++;

            childCount += traverseChildren(children.nextNode());
        }

        return childCount;
    }

    protected static String getTotalAndAverageDuration( Stopwatch stopwatch,
                                                        long numNodes ) {
        long totalDurationInMilliseconds = TimeUnit.NANOSECONDS.toMillis(stopwatch.getTotalDuration().longValue());
        if (numNodes == 0) numNodes = 1;
        long avgDuration = totalDurationInMilliseconds / numNodes;
        String units = " millisecond(s)";
        if (avgDuration < 1L) {
            long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotalDuration().longValue());
            avgDuration = totalDurationInMicroseconds / numNodes;
            units = " microsecond(s)";
        }
        return "total = " + stopwatch.getTotalDuration() + "; avg = " + avgDuration + units;
    }

    protected static int calculateTotalNumberOfNodesInTree( int numberOfChildrenPerNode,
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

    protected void assertChildrenInclude( Node parentNode,
                                          String... minimalChildNamesWithSns ) throws RepositoryException {
        assertChildrenInclude(null, parentNode, minimalChildNamesWithSns);
    }

    protected void assertChildrenInclude( String errorMessage,
                                          Node parentNode,
                                          String... minimalChildNamesWithSns
                                        ) throws RepositoryException {
        Set<String> childNames = new HashSet<String>();
        childNames.addAll(Arrays.asList(minimalChildNamesWithSns));

        NodeIterator iter = parentNode.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            String name = child.getName();
            if (child.getIndex() > 1) {
                name = name + "[" + child.getIndex() + "]";
            }
            childNames.remove(name);
            // If we've found all of the expected child names, then return immediately
            // (in case there are a very large number of children) ...
            if (childNames.isEmpty()) {
                return;
            }
        }

        String message = "Names of children not found under node: " + childNames;
        if (!StringUtil.isBlank(errorMessage)) {
            message += ". " + errorMessage;
        }
        assertThat(message, childNames.isEmpty(), is(true));
    }

}
