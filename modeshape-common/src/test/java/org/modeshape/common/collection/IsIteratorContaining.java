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
package org.modeshape.common.collection;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.matchers.TypeSafeMatcher;

/**
 * @author Randall Hauch
 * @param <T>
 */
public class IsIteratorContaining<T> extends TypeSafeMatcher<Iterator<T>> {
    private final Matcher<? extends T> elementMatcher;

    public IsIteratorContaining( Matcher<? extends T> elementMatcher ) {
        this.elementMatcher = elementMatcher;
    }

    @Override
    public boolean matchesSafely( Iterator<T> iterator ) {
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (elementMatcher.matches(item)) {
                return true;
            }
        }
        return false;
    }

    public void describeTo( Description description ) {
        description.appendText("a iterator containing ").appendDescriptionOf(elementMatcher);
    }

    @Factory
    public static <T> Matcher<Iterator<T>> hasItem( Matcher<? extends T> elementMatcher ) {
        return new IsIteratorContaining<T>(elementMatcher);
    }

    @Factory
    public static <T> Matcher<Iterator<T>> hasItem( T element ) {
        return hasItem(equalTo(element));
    }

    @Factory
    public static <T> Matcher<Iterator<T>> hasItems( Matcher<? extends T>... elementMatchers ) {
        Collection<Matcher<? extends Iterator<T>>> all = new ArrayList<Matcher<? extends Iterator<T>>>(elementMatchers.length);
        for (Matcher<? extends T> elementMatcher : elementMatchers) {
            all.add(hasItem(elementMatcher));
        }
        return allOf(all);
    }

    @Factory
    public static <T> Matcher<Iterator<T>> hasItems( T... elements ) {
        Collection<Matcher<? extends Iterator<T>>> all = new ArrayList<Matcher<? extends Iterator<T>>>(elements.length);
        for (T element : elements) {
            all.add(hasItem(element));
        }
        return allOf(all);
    }

}
