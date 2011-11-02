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
import static org.junit.Assert.assertThat;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.core.ExecutionContext;

/**
 * Tests that operate against a {@link WritableSessionCache}. Each test method starts with a clean slate of content, which is
 */
public class WritableSessionCacheTest extends AbstractSessionCacheTest {

    @Override
    protected SessionCache createSession( ExecutionContext context,
                                          WorkspaceCache cache ) {
        return new WritableSessionCache(context, workspaceCache, txnManager(), null);
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
        MutableCachedNode newChild = nodeB.createChild(session(),
                                                       newKey,
                                                       name("newChild"),
                                                       property("p1a", 344),
                                                       property("p2", false));
        print("Time (createChild): " + millis(System.nanoTime() - nanos) + " ms");
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
        print("Time (save): " + millis(System.nanoTime() - nanos) + " ms");

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
    public void shouldAllowSessionToCreateChildrenWithSameNameWithMultipleSaves() {
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
        workspaceCache.translator().optimizeChildrenBlocks(key, null, 1000, 500); // will merge two into a single block ...
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
                workspaceCache.translator().optimizeChildrenBlocks(key, null, 1000, 500); // will split into blocks ...
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
        workspaceCache.translator().optimizeChildrenBlocks(key, null, 1000, 500); // will merge two into a single block ...
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
                workspaceCache.translator().optimizeChildrenBlocks(key, null, 1000, 500); // will split into blocks ...
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
}
