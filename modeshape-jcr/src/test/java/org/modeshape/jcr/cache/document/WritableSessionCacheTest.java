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
package org.modeshape.jcr.cache.document;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;

/**
 * Tests that operate against a {@link WritableSessionCache}. Each test method starts with a clean slate of content
 */
public class WritableSessionCacheTest extends AbstractSessionCacheTest {

    private DocumentOptimizer optimizer;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.optimizer = new DocumentOptimizer(workspaceCache.documentStore());
    }

    @Override
    protected SessionCache createSessionCache( ExecutionContext context,
                                               WorkspaceCache cache,
                                               RepositoryEnvironment repositoryEnvironment ) {
        return new WritableSessionCache(context, workspaceCache, repositoryEnvironment);
    }

    @Test
    public void shouldWarmUpSystem() {
    }

    @Test
    public void shouldAllowSessionToCreateAndAccessToNewPropertyOnExistingNodeBeforeSave() {
        // Make sure the property does not exist ...
        check(cache).noProperty("/childB", "p1");

        // Set property on existing node ...
        MutableCachedNode nodeB = check(cache).mutableNode("/childB");
        nodeB.setProperty(session(), property("p1", "value1"));
        check(cache).property(nodeB, property("p1", "value1"));

        // Make sure the other session doesn't see the new property ...
        check(session2).noProperty("/childB", "p1");
    }

    @Test
    public void shouldAllowSessionToCreateAndAccessNewChildNodeOnExistingNodeBeforeSave() {
        // Make sure the property does not exist ...
        check(cache).noNode("/childB/newChild");

        print(false);

        // Set property on existing node ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        NodeKey newKey = session1.createNodeKeyWithIdentifier("newChild");
        long nanos = System.nanoTime();
        MutableCachedNode newChild = nodeB.createChild(session(), newKey, name("newChild"), property("p1a", 344),
                                                       property("p2", false));
        print("Time (createChild): " + millis(Math.abs(System.nanoTime() - nanos)) + " ms");
        assertThat(newChild.getPath(session1), is(path("/childB/newChild")));
        check(session1).children(nodeB, "childC", "childD", "newChild");
        check(session1).property("/childB/newChild", property("p1a", 344));
        check(session1).property("/childB/newChild", property("p2", false));

        // Make sure the other session doesn't see the new child ...
        check(session2).children(nodeB.getKey(), "childC", "childD");

        print(false);
        nanos = System.nanoTime();
        session1.save();
        print(false);
        print("Time (save): " + millis(Math.abs(System.nanoTime() - nanos)) + " ms");

        // Both sessions should see all 3 children ...
        check(session1).children(nodeB, "childC", "childD", "newChild");
        check(session2).children(nodeB, "childC", "childD", "newChild");
        check(session2).property("/childB/newChild", property("p1a", 344));
        check(session2).property("/childB/newChild", property("p2", false));
    }

    @Test
    public void shouldAllowSessionToCreateManyChildrenWithSameNameAndThenSave() {
        // Make sure the property does not exist ...
        check(cache).noNode("/childB/newChild");

        print(false);

        // Set property on existing node ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        Stopwatch create = new Stopwatch();
        Stopwatch total = new Stopwatch();
        Stopwatch save = new Stopwatch();
        total.start();
        for (int i = 0; i != 1000; ++i) {
            create.start();
            NodeKey newKey = session1.createNodeKey();
            nodeB.createChild(session(), newKey, name("newChild"), property("p1a", 344), property("p2", false));
            create.stop();
        }

        // And save ...
        save.start();
        session1.save();
        save.stop();
        total.stop();

        // Find node B again after the save ...
        nodeB = check(session1).mutableNode("/childB");

        print(true);
        print("Number of children: " + nodeB.getChildReferences(session1).size());
        print("Time (create): " + create.getSimpleStatistics());
        print("Time (save): " + save.getSimpleStatistics());
        print("Time (total): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 9450);
        total.stop();
        print("Time (getchild#9450): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 10);
        total.stop();
        print("Time (getchild#10): " + total.getTotalDuration());
    }

    @Test
    public void shouldAllowSessionToCreateChildrenWithSameNameWithMultipleSaves() throws Exception {
        // Make sure the property does not exist ...
        check(cache).noNode("/childB/newChild");

        print(false);

        // Set property on existing node ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        NodeKey key = nodeB.getKey();
        Stopwatch create = new Stopwatch();
        Stopwatch total = new Stopwatch();
        Stopwatch save = new Stopwatch();
        Stopwatch opt = new Stopwatch();
        txnManager().begin();
        optimizer.optimizeChildrenBlocks(key, null, 1000, 500); // will merge two into a single block ...
        txnManager().commit();
        print(true);
        print("Creating nodes ...");
        total.start();
        for (int i = 0; i != 10000; ++i) {
            create.start();
            NodeKey newKey = key.withId("child" + i);
            // NodeKey newKey = session1.createNodeKey();
            nodeB.createChild(session1, newKey, name("newChild"), property("p1a", 344), property("p2", false));
            create.stop();
            if (i != 0 && i % 1000 == 0) {
                print(false);
                print("Saving...");
                // print(false);
                save.start();
                session1.save();
                save.stop();
                print(false);
                print("Optimizing...");
                print(false);
                opt.start();
                txnManager().begin();
                optimizer.optimizeChildrenBlocks(key, null, 1000, 500); // will split into blocks ...
                txnManager().commit();
                opt.stop();
                // Find node B again after the save ...
                nodeB = check(session1).mutableNode("/childB");
            }
        }
        total.stop();

        print(true);
        print("Time (create): " + create.getSimpleStatistics());
        print("Time (save): " + save.getSimpleStatistics());
        print("Time (optimize): " + opt.getTotalDuration());
        print("Time (total): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 9450);
        total.stop();
        print("Time (getchild#9450): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 10);
        total.stop();
        print("Time (getchild#10): " + total.getTotalDuration());
    }

    @Ignore( "Usually ignored because of memory requirements" )
    @Test
    public void shouldAllowSessionToCreate100KChildrenWithSameNameWithMultipleSaves() {
        // Make sure the property does not exist ...
        check(cache).noNode("/childB/newChild");

        print(false);

        // Set property on existing node ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        NodeKey key = nodeB.getKey();
        Stopwatch create = new Stopwatch();
        Stopwatch total = new Stopwatch();
        Stopwatch save = new Stopwatch();
        Stopwatch opt = new Stopwatch();
        optimizer.optimizeChildrenBlocks(key, null, 1000, 500); // will merge two into a single block ...
        print(true);
        print("Creating nodes ...");
        total.start();
        for (int i = 0; i != 100000; ++i) {
            create.start();
            NodeKey newKey = key.withId("child" + i);
            // NodeKey newKey = session1.createNodeKey();
            nodeB.createChild(session1, newKey, name("newChild"), property("p1a", 344), property("p2", false));
            create.stop();
            if (i != 0 && i % 1000 == 0) {
                print(false);
                print("Saving...");
                // print(false);
                save.start();
                session1.save();
                save.stop();
                print(false);
                print("Optimizing...");
                print(false);
                opt.start();
                optimizer.optimizeChildrenBlocks(key, null, 1000, 500); // will split into blocks ...
                opt.stop();
                // Find node B again after the save ...
                nodeB = check(session1).mutableNode("/childB");
            }
        }
        total.stop();

        print(true);
        print("Time (create): " + create.getSimpleStatistics());
        print("Time (save): " + save.getSimpleStatistics());
        print("Time (optimize): " + opt.getTotalDuration());
        print("Time (total): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 49450);
        total.stop();
        print("Time (getchild#49450): " + total.getTotalDuration());

        session1.clear();

        total.reset();
        total.start();
        nodeB.getChildReferences(session1).getChild(name("newChild"), 10);
        total.stop();
        print("Time (getchild#10): " + total.getTotalDuration());
    }

    @Test
    public void shouldAllowTransientlyRenamingChildNode() {
        MutableCachedNode root = session1.mutable(session1.getRootKey());
        MutableCachedNode node = root.createChild(session(), newKey("node"), name("node"), property("p1", "value"));
        NodeKey childAKey = node.createChild(session(), newKey("x-childA"), name("childA"), property("p1", "value A")).getKey();
        NodeKey childBKey = node.createChild(session(), newKey("x-childB"), name("childB"), property("p1", "value B")).getKey();
        NodeKey childCKey = node.createChild(session(), newKey("x-childC"), name("childC"), property("p1", "value C")).getKey();
        session1.save();

        // Check the children ...
        node = check(session1).mutableNode(node.getKey(), "/node");
        check(session1).node(childAKey, "/node/childA");
        check(session1).node(childBKey, "/node/childB");
        check(session1).node(childCKey, "/node/childC");
        check(session1).children(node.getKey(), "childA", "childB", "childC");

        // Now transiently rename child b ...
        node.renameChild(session1, childBKey, name("childD"));

        // Check that the session uses the new name ...
        CachedNode renamed = session1.getNode(childBKey);
        assertThat(renamed.getSegment(session1), is(segment("childD")));

        check(session1).node("/node");
        check(session1).node("/node/childA");
        check(session1).node("/node/childC");
        check(session1).node("/node/childD");
        check(session1).noNode("/node/childB");
        check(session1).children(node.getKey(), "childA", "childD", "childC");
    }

    @Test
    public void shouldAllowAccessingRenamedChildNodeAfterPersisting() {
        MutableCachedNode root = session1.mutable(session1.getRootKey());
        MutableCachedNode node = root.createChild(session(), newKey("node"), name("node"), property("p1", "value"));
        NodeKey childAKey = node.createChild(session(), newKey("x-childA"), name("childA"), property("p1", "value A")).getKey();
        NodeKey childBKey = node.createChild(session(), newKey("x-childB"), name("childB"), property("p1", "value B")).getKey();
        NodeKey childCKey = node.createChild(session(), newKey("x-childC"), name("childC"), property("p1", "value C")).getKey();
        session1.save();

        // Check the children ...
        node = check(session1).mutableNode(node.getKey(), "/node");
        check(session1).node(childAKey, "/node/childA");
        check(session1).node(childBKey, "/node/childB");
        check(session1).node(childCKey, "/node/childC");
        check(session1).children(node.getKey(), "childA", "childB", "childC");

        // Now transiently rename child b ...
        node.renameChild(session1, childBKey, name("childD"));

        // Now save ...
        session1.save();

        check(session1).node("/node");
        check(session1).node("/node/childA");
        check(session1).node("/node/childC");
        check(session1).node("/node/childD");
        check(session1).noNode("/node/childB");
        check(session1).children(node.getKey(), "childA", "childD", "childC");
    }

    @Test
    public void shouldReturnAllTransientNodeKeys() {
        NodeKey rootKey = session1.getRootKey();
        MutableCachedNode root = session1.mutable(rootKey);
        NodeKey childAKey = root.createChild(session(), newKey("x-childA"), name("childA"), property("p1", "value A")).getKey();
        NodeKey childBKey = root.createChild(session(), newKey("x-childB"), name("childB"), property("p1", "value B")).getKey();

        Set<NodeKey> transientNodeKeys = session1.getChangedNodeKeys();
        assertEquals(new HashSet<NodeKey>(Arrays.asList(rootKey, childAKey, childBKey)), transientNodeKeys);
    }

    @Test
    public void shouldReturnTransientKeysAtOrBelowNode() {
        NodeKey rootKey = session1.getRootKey();
        MutableCachedNode root = session1.mutable(rootKey);
        // root/childA
        MutableCachedNode childA = root.createChild(session(), newKey("x-childA"), name("childA"), property("p1", "value A"));
        // root/childA/childB
        MutableCachedNode childB = childA.createChild(session(), newKey("x-childB"), name("childB"), property("p1", "value B"));
        // root/childC
        MutableCachedNode childC = root.createChild(session(), newKey("x-childC"), name("childC"), property("p1", "value C"));

        assertEquals(new HashSet<NodeKey>(Arrays.asList(childA.getKey(), childB.getKey())),
                     session1.getChangedNodeKeysAtOrBelow(childA));
        assertEquals(new HashSet<NodeKey>(Arrays.asList(rootKey, childA.getKey(), childB.getKey(), childC.getKey())),
                     session1.getChangedNodeKeysAtOrBelow(root));
        assertEquals(new HashSet<NodeKey>(Arrays.asList(childC.getKey())), session1.getChangedNodeKeysAtOrBelow(childC));
    }

    @Test
    public void shouldReturnTransientKeysAtOrBelowNodeWithRemovedChild() {
        NodeKey rootKey = session1.getRootKey();
        MutableCachedNode root = session1.mutable(rootKey);

        SessionCache sessionCache = session();
        NodeKey childKey = newKey("x-childA");
        MutableCachedNode child = root.createChild(sessionCache, childKey, name("childA"), property("p1", "value A"));
        session1.destroy(child.getKey());
        assertEquals(new HashSet<NodeKey>(Arrays.asList(rootKey, childKey)), session1.getChangedNodeKeysAtOrBelow(root));
    }

    @Test
    public void shouldAllowTransientlyMovingNode() {

    }

    @Test
    public void shouldAllowAccessingRenamedMovedNodeAfterPersisting() {

    }
}
