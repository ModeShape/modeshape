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
package org.jboss.dna.graph;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.basic.BasicPathSegment;
import org.junit.matchers.IsCollectionContaining;
import org.junit.matchers.TypeSafeMatcher;

/**
 * @author Randall Hauch
 */
public class IsNodeWithChildren extends TypeSafeMatcher<Node> {
    private final Matcher<Iterable<Path.Segment>> childMatcher;

    public IsNodeWithChildren( Matcher<Iterable<Path.Segment>> childMatcher ) {
        this.childMatcher = childMatcher;
    }

    @Override
    public boolean matchesSafely( Node node ) {
        List<Location> children = node.getChildren();
        List<Path.Segment> childSegments = new ArrayList<Path.Segment>(children.size());
        for (Location child : children) {
            childSegments.add(child.getPath().getLastSegment());
        }
        return childMatcher.matches(childSegments);
    }

    public void describeTo( Description description ) {
        description.appendText("a node containing children").appendDescriptionOf(childMatcher);
    }

    @Factory
    public static IsNodeWithChildren hasChild( Name name,
                                               int sameNameSiblingIndex ) {
        Path.Segment segment = new BasicPathSegment(name, sameNameSiblingIndex);
        return new IsNodeWithChildren(IsCollectionContaining.hasItem(segment));
    }

    @Factory
    public static IsNodeWithChildren hasChild( Path.Segment child ) {
        return new IsNodeWithChildren(IsCollectionContaining.hasItem(child));
    }

    @Factory
    public static IsNodeWithChildren hasChildren( Path.Segment... childSegments ) {
        return new IsNodeWithChildren(IsCollectionContaining.hasItems(childSegments));
    }

    @Factory
    public static IsNodeWithChildren hasNoChildren() {
        Path.Segment[] childSegments = new Path.Segment[] {};
        return new IsNodeWithChildren(IsCollectionContaining.hasItems(childSegments));
    }

}
