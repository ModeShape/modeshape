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
package org.modeshape.common.text;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Randall Hauch
 */
public class StringMatcher extends TypeSafeMatcher<String> {

    protected final String substring;

    protected StringMatcher( final String substring ) {
        this.substring = substring;
    }

    @Override
    public boolean matchesSafely( String item ) {
        return item.startsWith(item);
    }

    @Override
    public void describeTo( Description description ) {
        description.appendText("a string starts with ").appendValue(substring);
    }

    public static Matcher<String> startsWith( String prefix ) {
        return new StringMatcher(prefix);
    }

}
