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
package org.modeshape.graph;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.modeshape.graph.Node;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.junit.matchers.IsCollectionContaining;
import org.junit.matchers.TypeSafeMatcher;

/**
 * @author Randall Hauch
 */
public class IsNodeWithProperty extends TypeSafeMatcher<Node> {
    private final String propertyNameStr;
    private final Name propertyName;
    private final Matcher<Iterable<Object>> valueMatcher;

    public IsNodeWithProperty( String propertyNameStr,
                       Name propertyName,
                       Matcher<Iterable<Object>> valueMatcher ) {
        this.propertyNameStr = propertyNameStr;
        this.propertyName = propertyName;
        this.valueMatcher = valueMatcher;
    }

    @Override
    public boolean matchesSafely( Node node ) {
        Property prop = propertyNameStr != null ? node.getProperty(propertyNameStr) : node.getProperty(propertyName);
        if (prop != null) {
            return valueMatcher.matches(prop);
        }
        return false;
    }

    public void describeTo( Description description ) {
        Object name = propertyNameStr != null ? propertyNameStr : propertyName;
        description.appendText("a property \"" + name + "\"containing ").appendDescriptionOf(valueMatcher);
    }

    @Factory
    public static IsNodeWithProperty hasProperty( Name name,
                                          Object... values ) {
        return new IsNodeWithProperty(null, name, IsCollectionContaining.hasItems(values));
    }

    @Factory
    public static IsNodeWithProperty hasProperty( String name,
                                          Object... values ) {
        return new IsNodeWithProperty(name, null, IsCollectionContaining.hasItems(values));
    }

}
