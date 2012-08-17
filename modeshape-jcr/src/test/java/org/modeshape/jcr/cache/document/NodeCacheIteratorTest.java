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
package org.modeshape.jcr.cache.document;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;

public class NodeCacheIteratorTest {

    private final static String SOURCE_KEY = NodeKey.keyForSourceName("source");
    private final static String WORKSPACE_KEY = NodeKey.keyForSourceName("workspace");

    protected NodeCache cache;
    private List<NodeKey> allKeys;

    @Before
    public void beforeEach() {
        allKeys = new ArrayList<NodeKey>();
        cache = Mockito.mock(NodeCache.class);

        addNode("root", "node1", "node2", "node3");
        addNode("node1", "node11", "node12", "node13");
        addNode("node2", "node21", "node22", "node23");
        addNode("node3", "node31", "node32", "node33");
        addNodeWithLeafChildren("node11", "node111", "node112", "node113");
        addNodeWithLeafChildren("node12", "node121", "node122", "node123");
        addNodeWithLeafChildren("node13", "node131", "node132", "node133");
        addNodeWithLeafChildren("node21", "node211", "node212", "node213");
        addNodeWithLeafChildren("node22", "node221", "node222", "node223");
        addNodeWithLeafChildren("node23", "node231", "node232", "node233");
        addNodeWithLeafChildren("node31", "node311", "node312", "node313");
        addNodeWithLeafChildren("node32", "node321", "node322", "node323");
        addNodeWithLeafChildren("node33", "node331", "node332", "node333");

        when(cache.getAllNodeKeys()).thenAnswer(new Answer<Iterator<NodeKey>>() {
            @Override
            public Iterator<NodeKey> answer( InvocationOnMock invocation ) throws Throwable {
                // Need to create a new iterator every time ...
                return new NodeCacheIterator(cache, nodeKey("root"));
            }
        });
        when(cache.getAllNodeKeysAtAndBelow((NodeKey)anyObject())).thenAnswer(new Answer<Iterator<NodeKey>>() {
            @Override
            public Iterator<NodeKey> answer( InvocationOnMock invocation ) throws Throwable {
                // Need to create a new iterator every time ...
                NodeKey nodeKey = (NodeKey)invocation.getArguments()[0];
                return new NodeCacheIterator(cache, nodeKey);
            }
        });
    }

    @After
    public void afterEach() {
        cache = null;
    }

    @Test
    public void shouldIterateOverAllNodeKeysInTheCacheWhenUsingHasNextAndNext() {
        Set<NodeKey> expected = new HashSet<NodeKey>(allKeys);
        Iterator<NodeKey> iter = cache.getAllNodeKeys();
        while (iter.hasNext()) {
            NodeKey key = iter.next();
            assertThat(key, is(notNullValue()));
            assertThat(expected.remove(key), is(true));
        }
        assertThat(expected.isEmpty(), is(true));
    }

    @Test
    public void shouldIterateOverAllNodeKeysInTheCacheWhenUsingOnlyNext() {
        Set<NodeKey> expected = new HashSet<NodeKey>(allKeys);
        Iterator<NodeKey> iter = cache.getAllNodeKeys();
        for (int i = 0; i != allKeys.size(); ++i) {
            NodeKey key = iter.next();
            assertThat(key, is(notNullValue()));
            assertThat(expected.remove(key), is(true));
        }
        assertThat(expected.isEmpty(), is(true));
        try {
            iter.next();
            fail("Should have thrown a NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void shouldIterateOverAllNodeKeysInTheCacheBelowBranchWhenUsingHasNextAndNext() {
        assertIterateOverSubtreeWhenUsingHasNextAndNext(nodeKey("node1"));
    }

    @Test
    public void shouldIterateOverAllNodeKeysInTheCacheBelowBranchWhenUsingOnlyNext() {
        assertIterateOverSubtreeWhenUsingOnlyNext(nodeKey("node1"));
    }

    protected void assertIterateOverSubtreeWhenUsingHasNextAndNext( NodeKey startingKey ) {
        Set<NodeKey> expected = findAllNodesAtOrBelow(startingKey);
        Iterator<NodeKey> iter = cache.getAllNodeKeysAtAndBelow(startingKey);
        while (iter.hasNext()) {
            NodeKey key = iter.next();
            assertThat(key, is(notNullValue()));
            assertThat(expected.remove(key), is(true));
        }
        assertThat(expected.isEmpty(), is(true));
    }

    protected void assertIterateOverSubtreeWhenUsingOnlyNext( NodeKey startingKey ) {
        Set<NodeKey> expected = findAllNodesAtOrBelow(startingKey);
        Iterator<NodeKey> iter = cache.getAllNodeKeysAtAndBelow(startingKey);
        long size = expected.size();
        for (int i = 0; i != size; ++i) {
            NodeKey key = iter.next();
            assertThat(key, is(notNullValue()));
            assertThat(expected.remove(key), is(true));
        }
        assertThat(expected.isEmpty(), is(true));
        try {
            iter.next();
            fail("Should have thrown a NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    protected Set<NodeKey> findAllNodesAtOrBelow( NodeKey key ) {
        Set<NodeKey> foundKeys = new HashSet<NodeKey>();
        findAllNodesAtOrBelow(key, foundKeys);
        return foundKeys;
    }

    protected void findAllNodesAtOrBelow( NodeKey key,
                                          Set<NodeKey> foundKeys ) {
        CachedNode node = cache.getNode(key);
        assertThat(node, is(notNullValue()));
        foundKeys.add(key);
        Iterator<NodeKey> iter = node.getChildReferences(cache).getAllKeys();
        while (iter.hasNext()) {
            findAllNodesAtOrBelow(iter.next(), foundKeys);
        }
    }

    // ----------------------------------------------------------------
    // Utility methods to construct the NodeCache ...
    // ----------------------------------------------------------------

    protected NodeKey nodeKey( String id ) {
        return new NodeKey(SOURCE_KEY, WORKSPACE_KEY, id);
    }

    protected void addNode( String key,
                            String... childKeys ) {
        // Mock the ChildReferences and stub out the only method we'll call ...
        final List<NodeKey> childKeyList = new ArrayList<NodeKey>();
        for (String childKey : childKeys) {
            childKeyList.add(nodeKey(childKey));
        }
        ChildReferences childRefs = Mockito.mock(ChildReferences.class);
        when(childRefs.getAllKeys()).thenAnswer(new Answer<Iterator<NodeKey>>() {
            @Override
            public Iterator<NodeKey> answer( InvocationOnMock invocation ) throws Throwable {
                // Need to create a new iterator every time ...
                return childKeyList.iterator();
            }
        });

        // Mock the CachedNode and stub out the only method we'll call ...
        NodeKey nodeKey = nodeKey(key);
        CachedNode node = Mockito.mock(CachedNode.class);
        when(node.getChildReferences(cache)).thenReturn(childRefs);

        // Stub the cache invocation ...
        when(cache.getNode(nodeKey)).thenReturn(node);

        // Add the key to our master list ...
        allKeys.add(nodeKey);
    }

    protected void addNodeWithLeafChildren( String key,
                                            String... childKeys ) {
        addNode(key, childKeys);
        for (String childKey : childKeys) {
            addNode(childKey);
        }
    }
}
