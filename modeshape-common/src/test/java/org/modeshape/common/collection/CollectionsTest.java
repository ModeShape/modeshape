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

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.modeshape.common.collection.IsIteratorContaining.hasItems;


/**
 * Unit test for {@link Collections}
 */
public class CollectionsTest {

    @Test
    public void shouldConcatenateIterables() throws Exception {
        Iterable<String> a = Arrays.asList("a", "b", "c");
        Iterable<String> b = Arrays.asList("1", "2", "3");

        final Iterable<String> concatIterable = Collections.concat(a, b);
        final Iterator<String> iterator = concatIterable.iterator();
        assertThat(iterator, hasItems("a", "b", "c", "1", "2", "3"));
    }

    @Test
    public void shouldConcatenateIterators() throws Exception {
        Iterable<String> a = Arrays.asList("a", "b", "c");
        Iterable<String> b = Arrays.asList("1", "2", "3");

        final Iterator<String> iterator = Collections.concat(a.iterator(), b.iterator());
        assertThat(iterator, hasItems("a", "b", "c", "1", "2", "3"));
    }
}
