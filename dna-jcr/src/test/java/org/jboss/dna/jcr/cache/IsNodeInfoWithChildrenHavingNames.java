/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr.cache;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.basic.BasicPathSegment;
import org.junit.matchers.IsCollectionContaining;
import org.junit.matchers.TypeSafeMatcher;

/**
 * A JUnit {@link TypeSafeMatcher matcher} that can be used to assert that a NodeInfo (or other
 * <code>Iterable&lt;ChildNode></code>) has {@link ChildNode} instances with specific {@link ChildNode#getSegment() names}.
 */
public class IsNodeInfoWithChildrenHavingNames extends TypeSafeMatcher<Children> {
    private final Matcher<Iterable<Path.Segment>> childMatcher;

    public IsNodeInfoWithChildrenHavingNames( Matcher<Iterable<Path.Segment>> childMatcher ) {
        this.childMatcher = childMatcher;
    }

    @Override
    public boolean matchesSafely( Children children ) {
        List<Path.Segment> childSegments = new ArrayList<Path.Segment>(children.size());
        for (ChildNode child : children) {
            childSegments.add(child.getSegment());
        }
        return childMatcher.matches(childSegments);
    }

    public void describeTo( Description description ) {
        description.appendText("children").appendDescriptionOf(childMatcher);
    }

    @Factory
    public static IsNodeInfoWithChildrenHavingNames hasChild( Name name,
                                                              int sameNameSiblingIndex ) {
        Path.Segment segment = new BasicPathSegment(name, sameNameSiblingIndex);
        return new IsNodeInfoWithChildrenHavingNames(IsCollectionContaining.hasItem(segment));
    }

    @Factory
    public static IsNodeInfoWithChildrenHavingNames hasChild( Path.Segment child ) {
        return new IsNodeInfoWithChildrenHavingNames(IsCollectionContaining.hasItem(child));
    }

    @Factory
    public static IsNodeInfoWithChildrenHavingNames hasChildren( Path.Segment... childSegments ) {
        return new IsNodeInfoWithChildrenHavingNames(IsCollectionContaining.hasItems(childSegments));
    }

    @Factory
    public static IsNodeInfoWithChildrenHavingNames isEmpty() {
        Path.Segment[] childSegments = new Path.Segment[] {};
        return new IsNodeInfoWithChildrenHavingNames(IsCollectionContaining.hasItems(childSegments));
    }

}
