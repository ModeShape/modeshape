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
package org.modeshape.common.collection;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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

    @Override
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
    @SafeVarargs
    public static <T> Matcher<Iterator<T>> hasItems( Matcher<? extends T>... elementMatchers ) {
        Collection<Matcher<? super Iterator<T>>> all = new ArrayList<>(elementMatchers.length);
        for (Matcher<? extends T> elementMatcher : elementMatchers) {
            all.add(hasItem(elementMatcher));
        }
        return allOf(all);
    }

    @Factory
    @SafeVarargs
    public static <T> Matcher<Iterator<T>> hasItems( T... elements ) {
        Collection<Matcher<? super Iterator<T>>> all = new ArrayList<>(elements.length);
        for (T element : elements) {
            all.add(hasItem(element));
        }
        return allOf(all);
    }
}
