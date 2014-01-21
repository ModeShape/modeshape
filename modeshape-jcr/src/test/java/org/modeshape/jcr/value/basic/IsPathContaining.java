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
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.ArrayList;
import java.util.Collection;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;

/**
 * @author Randall Hauch
 */
public class IsPathContaining extends TypeSafeMatcher<Iterable<Segment>> {

    private final Matcher<? super Segment> elementMatcher;

    public IsPathContaining( Matcher<? super Segment> elementMatcher ) {
        this.elementMatcher = elementMatcher;
    }

    @Override
    public boolean matchesSafely( Iterable<Segment> collection ) {
        for (Segment item : collection) {
            if (elementMatcher.matches(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void describeTo( Description description ) {
        description.appendText("a path containing ").appendDescriptionOf(elementMatcher);
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegment( Matcher<? super Segment> elementMatcher ) {
        return new IsPathContaining(elementMatcher);
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegment( Segment element ) {
        return hasSegment(equalTo(element));
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegments( @SuppressWarnings( "unchecked" ) Matcher<Path.Segment>... elementMatchers ) {
        Collection<Matcher<? super Iterable<Path.Segment>>> all = new ArrayList<>(elementMatchers.length);

        for (Matcher<Path.Segment> elementMatcher : elementMatchers) {
            Matcher<Iterable<Path.Segment>> itemMatcher = hasSegment(elementMatcher);
            all.add(itemMatcher);
        }

        return allOf(all);
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegments( Segment... elements ) {
        Collection<Matcher<? super Iterable<Segment>>> all = new ArrayList<>(elements.length);
        for (Segment element : elements) {
            all.add(hasSegment(element));
        }
        return allOf(all);
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegment( PathFactory pathFactory,
                                                         String element ) {
        Path.Segment segment = pathFactory.createSegment(element);
        return hasSegment(equalTo(segment));
    }

    @Factory
    public static Matcher<Iterable<Segment>> hasSegments( PathFactory pathFactory,
                                                          String... segments ) {
        Collection<Matcher<? super Iterable<Segment>>> all = new ArrayList<>(segments.length);
        for (String element : segments) {
            all.add(hasSegment(pathFactory, element));
        }
        return allOf(all);
    }
}
