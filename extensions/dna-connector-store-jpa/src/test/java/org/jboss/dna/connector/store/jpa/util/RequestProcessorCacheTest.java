/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.store.jpa.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.jboss.dna.graph.BasicExecutionContext;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class RequestProcessorCacheTest {

    private RequestProcessorCache cache;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private NamespaceRegistry namespaces;
    private Location location;
    private Location[] children;
    private LinkedList<Location> childrenList;
    private Location location2;
    private Location[] children2;
    private LinkedList<Location> childrenList2;

    @Before
    public void beforeEach() {
        ExecutionContext context = new BasicExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        nameFactory = context.getValueFactories().getNameFactory();
        namespaces = context.getNamespaceRegistry();
        cache = new RequestProcessorCache(pathFactory);

        Path parent = pathFactory.create("/a/b/c");
        location = new Location(parent, UUID.randomUUID());
        children = new Location[] {new Location(pathFactory.create(parent, "d1"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "d2"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "d3"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "d4"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("e"), 1), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("e"), 2), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("e"), 3), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("e"), 4), UUID.randomUUID())};
        childrenList = new LinkedList<Location>();
        for (Location loc : children) {
            childrenList.add(loc);
        }

        parent = pathFactory.create("/a/b/c/e[2]");
        location2 = new Location(parent, children[5].getUuid());
        children2 = new Location[] {new Location(pathFactory.create(parent, "f1"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "f2"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "f3"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, "f4"), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("g"), 1), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("g"), 2), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("g"), 3), UUID.randomUUID()),
            new Location(pathFactory.create(parent, name("g"), 4), UUID.randomUUID())};
        childrenList2 = new LinkedList<Location>();
        for (Location loc : children2) {
            childrenList2.add(loc);
        }
    }

    protected Path path( String name ) {
        return pathFactory.create(name);
    }

    protected Name name( String name ) {
        return nameFactory.create(name);
    }

    @Test
    public void shouldNotFindLocationForPathWhenEmpty() {
        assertThat(cache.getLocationFor(location.getPath()), is(nullValue()));
    }

    @Test
    public void shouldNotFindLocationForNullPath() {
        assertThat(cache.getLocationFor(null), is(nullValue()));
    }

    @Test
    public void shouldFindLocationForPathAfterAdding() {
        assertThat(cache.getLocationFor(location.getPath()), is(nullValue()));
        cache.addNewNode(location);
        assertThat(cache.getLocationFor(location.getPath()), is(sameInstance(location)));
    }

    @Test
    public void shouldNotFindChildrenForPathEvenAfterLocationForSamePathIsAdded() {
        cache.addNewNode(location);
        assertThat(cache.getLocationFor(location.getPath()), is(sameInstance(location)));
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
    }

    @Test
    public void shouldNotFindChildrenForPathWhenEmpty() {
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
    }

    @Test
    public void shouldNotFindChildrenForNullPath() {
        assertThat(cache.getAllChildren(null), is(nullValue()));
    }

    @Test
    public void shouldFindChildrenForPathAfterChildrenAreSet() {
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
        cache.setAllChildren(location.getPath(), childrenList);
        assertThat(cache.getAllChildren(location.getPath()), is(sameInstance(childrenList)));
    }

    @Test
    public void shouldRemoveChildrenForPathIfSuppliedListIsNull() {
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
        cache.setAllChildren(location.getPath(), childrenList);
        assertThat(cache.getAllChildren(location.getPath()), is(sameInstance(childrenList)));
        cache.setAllChildren(location.getPath(), null);
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
    }

    @Test
    public void shouldSetEmptyChildrenForPathIfSuppliedListIsEmpty() {
        assertThat(cache.getAllChildren(location.getPath()), is(nullValue()));
        LinkedList<Location> emptyList = new LinkedList<Location>();
        cache.setAllChildren(location.getPath(), emptyList);
        assertThat(cache.getAllChildren(location.getPath()), is(sameInstance(emptyList)));
    }

    @Test
    public void shouldUpdateCacheWhenNodeIsMoved() {
        // The cache knows about the children of "/a/b/c" and "/a/b/c/e[2]".
        // This test moves "/a/b/c/e[2]" into "/a/b/c/d3"
        Location oldLocation = location2;
        Location newLocation = new Location(pathFactory.create("/a/b/c/d3/e[1]"));
        assertThat(oldLocation.getPath().getString(namespaces), is("/a/b/c/e[2]"));
        assertThat(newLocation.getPath().getString(namespaces), is("/a/b/c/d3/e[1]"));
        cache.addNewNode(location);
        cache.addNewNode(location2);
        for (Location loc : children)
            cache.addNewNode(loc);
        for (Location loc : children2)
            cache.addNewNode(loc);
        cache.addNewNode(location);
        cache.addNewNode(location2);
        cache.setAllChildren(location.getPath(), childrenList);
        cache.setAllChildren(location2.getPath(), childrenList2);

        // Assert the information before the move ...
        assertThat(cache.getAllChildren(location.getPath()), hasItems(children));
        assertThat(cache.getAllChildren(location2.getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(oldLocation.getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[5].getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(children[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[7].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[7].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(location.getPath()), is(location));
        assertThat(cache.getLocationFor(location2.getPath()), is(location2));
        assertThat(cache.getLocationFor(oldLocation.getPath()), is(oldLocation));
        assertThat(cache.getLocationFor(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children[0].getPath()), is(children[0]));
        assertThat(cache.getLocationFor(children[1].getPath()), is(children[1]));
        assertThat(cache.getLocationFor(children[2].getPath()), is(children[2]));
        assertThat(cache.getLocationFor(children[3].getPath()), is(children[3]));
        assertThat(cache.getLocationFor(children[4].getPath()), is(children[4]));
        assertThat(cache.getLocationFor(children[5].getPath()), is(children[5]));
        assertThat(cache.getLocationFor(children[6].getPath()), is(children[6]));
        assertThat(cache.getLocationFor(children[7].getPath()), is(children[7]));
        assertThat(cache.getLocationFor(children2[0].getPath()), is(children2[0]));
        assertThat(cache.getLocationFor(children2[1].getPath()), is(children2[1]));
        assertThat(cache.getLocationFor(children2[2].getPath()), is(children2[2]));
        assertThat(cache.getLocationFor(children2[3].getPath()), is(children2[3]));
        assertThat(cache.getLocationFor(children2[4].getPath()), is(children2[4]));
        assertThat(cache.getLocationFor(children2[5].getPath()), is(children2[5]));
        assertThat(cache.getLocationFor(children2[6].getPath()), is(children2[6]));
        assertThat(cache.getLocationFor(children2[7].getPath()), is(children2[7]));

        // System.out.println("Before:");
        // System.out.println(cache.getString(namespaces));

        // Move the branch (without a known index) ...
        assertThat(cache.moveNode(oldLocation, -1, newLocation), is(true));

        // System.out.println("After moving " + oldLocation.getPath().getString(namespaces) + " to "
        // + newLocation.getPath().getString(namespaces));
        // System.out.println(cache.getString(namespaces));

        // Check the cache content, which should no longer have any content below the old and new locations ...
        LinkedList<Location> afterRemoval = cache.getAllChildren(location.getPath());
        assertThat(afterRemoval.get(0), is(children[0]));
        assertThat(afterRemoval.get(1), is(children[1]));
        assertThat(afterRemoval.get(2), is(children[2]));
        assertThat(afterRemoval.get(3), is(children[3]));
        assertThat(afterRemoval.get(4), is(children[4]));
        assertThat(afterRemoval.get(5), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(afterRemoval.get(6), is(children[7].with(path("/a/b/c/e[3]"))));

        assertThat(cache.getAllChildren(location2.getPath()), is(nullValue())); // old location
        assertThat(cache.getAllChildren(oldLocation.getPath()), is(nullValue())); // old location
        assertThat(cache.getAllChildren(newLocation.getPath()), is(nullValue())); // all children removed

        assertThat(cache.getAllChildren(children[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[7].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(location.getPath()), is(location));
        // location 2 was moved, so it's been replaced by the next SNS (children 6 with SNS index of 2) ...
        assertThat(cache.getLocationFor(location2.getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(oldLocation.getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children[0].getPath()), is(children[0]));
        assertThat(cache.getLocationFor(children[1].getPath()), is(children[1]));
        assertThat(cache.getLocationFor(children[2].getPath()), is(children[2]));
        assertThat(cache.getLocationFor(children[3].getPath()), is(children[3]));
        assertThat(cache.getLocationFor(children[4].getPath()), is(children[4]));
        // children[6] replaced children[5]'s path, and [7] replaced [6]
        assertThat(cache.getLocationFor(children[5].getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(children[6].getPath()), is(children[7].with(path("/a/b/c/e[3]"))));
        assertThat(cache.getLocationFor(children[7].getPath()), is(nullValue()));
        // The following nodes were moved, but as children they were removed from the cache
        // rather than having a non-last-segment in their paths updated.
        assertThat(cache.getLocationFor(children2[0].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[7].getPath()), is(nullValue()));
    }

    @Test
    public void shouldUpdateCacheWhenNodeIsRemoved() {
        // The cache knows about the children of "/a/b/c" and "/a/b/c/e[2]".
        // This test removes "/a/b/c/e[2]"
        Location oldLocation = location2;
        Location newLocation = new Location(pathFactory.create("/a/b/c/d3/e[1]"));
        assertThat(oldLocation.getPath().getString(namespaces), is("/a/b/c/e[2]"));
        assertThat(newLocation.getPath().getString(namespaces), is("/a/b/c/d3/e[1]"));
        cache.addNewNode(location);
        cache.addNewNode(location2);
        for (Location loc : children)
            cache.addNewNode(loc);
        for (Location loc : children2)
            cache.addNewNode(loc);
        cache.addNewNode(location);
        cache.addNewNode(location2);
        cache.setAllChildren(location.getPath(), childrenList);
        cache.setAllChildren(location2.getPath(), childrenList2);

        // Assert the information before the move ...
        assertThat(cache.getAllChildren(location.getPath()), hasItems(children));
        assertThat(cache.getAllChildren(location2.getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(oldLocation.getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[5].getPath()), hasItems(children2));
        assertThat(cache.getAllChildren(children[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[7].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[7].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(location.getPath()), is(location));
        assertThat(cache.getLocationFor(location2.getPath()), is(location2));
        assertThat(cache.getLocationFor(oldLocation.getPath()), is(oldLocation));
        assertThat(cache.getLocationFor(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children[0].getPath()), is(children[0]));
        assertThat(cache.getLocationFor(children[1].getPath()), is(children[1]));
        assertThat(cache.getLocationFor(children[2].getPath()), is(children[2]));
        assertThat(cache.getLocationFor(children[3].getPath()), is(children[3]));
        assertThat(cache.getLocationFor(children[4].getPath()), is(children[4]));
        assertThat(cache.getLocationFor(children[5].getPath()), is(children[5]));
        assertThat(cache.getLocationFor(children[6].getPath()), is(children[6]));
        assertThat(cache.getLocationFor(children[7].getPath()), is(children[7]));
        assertThat(cache.getLocationFor(children2[0].getPath()), is(children2[0]));
        assertThat(cache.getLocationFor(children2[1].getPath()), is(children2[1]));
        assertThat(cache.getLocationFor(children2[2].getPath()), is(children2[2]));
        assertThat(cache.getLocationFor(children2[3].getPath()), is(children2[3]));
        assertThat(cache.getLocationFor(children2[4].getPath()), is(children2[4]));
        assertThat(cache.getLocationFor(children2[5].getPath()), is(children2[5]));
        assertThat(cache.getLocationFor(children2[6].getPath()), is(children2[6]));
        assertThat(cache.getLocationFor(children2[7].getPath()), is(children2[7]));

        // System.out.println("Before:");
        // System.out.println(cache.getString(namespaces));

        // Create the locations that in the branch to be removed ...
        List<Location> locationsToRemove = new LinkedList<Location>();
        locationsToRemove.add(location2);
        for (Location childLocation : children2) {
            locationsToRemove.add(childLocation);
        }
        locationsToRemove.add(new Location(pathFactory.create(children2[6].getPath(), "m1")));
        locationsToRemove.add(new Location(pathFactory.create(children2[6].getPath(), "m2")));
        locationsToRemove.add(new Location(pathFactory.create(children2[6].getPath(), "m3")));

        // Remove the branch ...
        assertThat(cache.removeBranch(locationsToRemove), is(true));

        // System.out.println("After removing " + locationsToRemove.get(0).getString(namespaces));
        // System.out.println(cache.getString(namespaces));

        // Check the cache content, which should no longer have any content below the old and new locations ...
        LinkedList<Location> afterRemoval = cache.getAllChildren(location.getPath());
        assertThat(afterRemoval.get(0), is(children[0]));
        assertThat(afterRemoval.get(1), is(children[1]));
        assertThat(afterRemoval.get(2), is(children[2]));
        assertThat(afterRemoval.get(3), is(children[3]));
        assertThat(afterRemoval.get(4), is(children[4]));
        assertThat(afterRemoval.get(5), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(afterRemoval.get(6), is(children[7].with(path("/a/b/c/e[3]"))));

        assertThat(cache.getAllChildren(location2.getPath()), is(nullValue())); // old location
        assertThat(cache.getAllChildren(oldLocation.getPath()), is(nullValue())); // old location
        assertThat(cache.getAllChildren(newLocation.getPath()), is(nullValue())); // all children removed

        assertThat(cache.getAllChildren(children[0].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getAllChildren(children2[7].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(location.getPath()), is(location));
        // location 2 was moved, so it's been replaced by the next SNS (children 6 with SNS index of 2) ...
        assertThat(cache.getLocationFor(location2.getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(oldLocation.getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(newLocation.getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children[0].getPath()), is(children[0]));
        assertThat(cache.getLocationFor(children[1].getPath()), is(children[1]));
        assertThat(cache.getLocationFor(children[2].getPath()), is(children[2]));
        assertThat(cache.getLocationFor(children[3].getPath()), is(children[3]));
        assertThat(cache.getLocationFor(children[4].getPath()), is(children[4]));
        // children[6] replaced children[5]'s path, and [7] replaced [6]
        assertThat(cache.getLocationFor(children[5].getPath()), is(children[6].with(path("/a/b/c/e[2]"))));
        assertThat(cache.getLocationFor(children[6].getPath()), is(children[7].with(path("/a/b/c/e[3]"))));
        assertThat(cache.getLocationFor(children[7].getPath()), is(nullValue()));
        // The following nodes were moved, but as children they were removed from the cache
        // rather than having a non-last-segment in their paths updated.
        assertThat(cache.getLocationFor(children2[0].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[1].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[2].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[3].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[4].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[5].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[6].getPath()), is(nullValue()));
        assertThat(cache.getLocationFor(children2[7].getPath()), is(nullValue()));
    }
}
