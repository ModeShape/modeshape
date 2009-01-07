/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;
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
