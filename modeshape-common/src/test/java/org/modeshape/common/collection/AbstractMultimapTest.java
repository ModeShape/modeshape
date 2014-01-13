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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractMultimapTest {

    protected Multimap<String, String> multimap;
    protected String[] keys;
    protected String[] values;

    @Before
    public void beforeEach() {
        multimap = createMultimap();
        keys = new String[] {"key1", "key2", "key3", "key4"};
        values = new String[] {"value1", "value2", "value3", "value4", "value5", "value6"};
    }

    @After
    public void afterEach() {
        multimap = null;
    }

    protected abstract <K, V> Multimap<K, V> createMultimap();

    protected abstract boolean valuesAllowDuplicates();

    @Test
    public void shouldBeEmptyAfterCreation() {
        assertThat(multimap.isEmpty(), is(true));
    }

    @Test
    public void shouldHaveZeroSizeAfterCreation() {
        assertThat(multimap.size(), is(0));
    }

    @Test
    public void shouldNotBeEmptyAfterAddingKeyValuePairToEmptyCollection() {
        multimap.put(keys[0], values[0]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(1));
        assertKeys(multimap, keys[0]);
        assertValues(multimap, keys[0], values[0]);
        assertEntries(multimap, entry(keys[0], values[0]));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldNotBeEmptyAfterAddingKeyAndTwoValuesToEmptyCollection() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[0], values[1]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(2));
        assertKeys(multimap, keys[0]);
        assertValues(multimap, keys[0], values[0], values[1]);
        assertEntries(multimap, entry(keys[0], values[0]), entry(keys[0], values[1]));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldNotBeEmptyAfterAddingMultipleKeyValuePairsToEmptyCollection() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[1]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(2));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0]);
        assertValues(multimap, keys[1], values[1]);
        assertEntries(multimap, entry(keys[0], values[0]), entry(keys[1], values[1]));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldNotBeEmptyAfterAddingMultipleKeyValuePairsMultipleTimesToEmptyCollection() {
        for (int i = 0; i != 3; ++i) {
            multimap.put(keys[0], values[0]);
            multimap.put(keys[1], values[1]);
        }
        if (valuesAllowDuplicates()) {
            assertThat(multimap.isEmpty(), is(false));
            assertThat(multimap.size(), is(6));
            assertKeys(multimap, keys[0], keys[1]);
            assertValues(multimap, keys[0], values[0], values[0], values[0]);
            assertValues(multimap, keys[1], values[1], values[1], values[1]);
            Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
            for (int i = 0; i != 3; ++i) {
                entries.add(entry(keys[0], values[0]));
                entries.add(entry(keys[1], values[1]));
            }
            assertEntries(multimap, entries);
        } else {
            assertThat(multimap.isEmpty(), is(false));
            assertThat(multimap.size(), is(2));
            assertKeys(multimap, keys[0], keys[1]);
            assertValues(multimap, keys[0], values[0]);
            assertValues(multimap, keys[1], values[1]);
            assertEntries(multimap, entry(keys[0], values[0]), entry(keys[1], values[1]));
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldAllowAddingToCollectionOfValues() {
        multimap.put(keys[0], values[0]);
        Collection<String> vals = multimap.get(keys[0]);
        vals.add(values[1]);
        vals.add(values[2]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(3));
        assertKeys(multimap, keys[0]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertEntries(multimap, entry(keys[0], values[0]), entry(keys[0], values[1]), entry(keys[0], values[2]));
    }

    protected <K, V> Map.Entry<K, V> entry( K key,
                                            V value ) {
        return new ImmutableMapEntry<K, V>(key, value);
    }

    protected <K, V> void assertEntries( Multimap<K, V> multimap,
                                         Map.Entry<K, V> entry ) {
        assertEntries(multimap, java.util.Collections.singletonList(entry));
    }

    protected <K, V> void assertEntries( Multimap<K, V> multimap,
                                         Map.Entry<K, V>... entries ) {
        assertEntries(multimap, Arrays.asList(entries));
    }

    protected <K, V> void assertEntries( Multimap<K, V> multimap,
                                         Collection<Map.Entry<K, V>> entries ) {
        Collection<Map.Entry<K, V>> actualEntries = multimap.entries();
        assertThat(actualEntries.size(), is(entries.size()));
        assertThat(actualEntries.containsAll(entries), is(true));
        assertThat(entries.containsAll(actualEntries), is(true));
    }

    protected <K, V> void assertKeys( Multimap<K, V> multimap,
                                      K key ) {
        assertKeys(multimap, java.util.Collections.singletonList(key));
    }

    protected <K, V> void assertKeys( Multimap<K, V> multimap,
                                      K... keys ) {
        assertKeys(multimap, Arrays.asList(keys));
    }

    protected <K, V> void assertKeys( Multimap<K, V> multimap,
                                      Collection<K> expectedKeys ) {
        Set<K> actualKeys = multimap.keySet();
        assertThat(actualKeys.size(), is(expectedKeys.size()));
        assertThat(actualKeys.containsAll(expectedKeys), is(true));
        assertThat(expectedKeys.containsAll(actualKeys), is(true));
    }

    protected <K, V> void assertValues( Multimap<K, V> multimap,
                                        K key,
                                        V value ) {
        Collection<V> expectedValues = null;
        if (valuesAllowDuplicates()) {
            expectedValues = java.util.Collections.singletonList(value);
        } else {
            expectedValues = java.util.Collections.singleton(value);
        }
        assertValues(multimap, key, expectedValues);
    }

    protected <K, V> void assertValues( Multimap<K, V> multimap,
                                        K key,
                                        V... values ) {
        Collection<V> expectedValues = null;
        if (valuesAllowDuplicates()) {
            expectedValues = Arrays.asList(values);
        } else {
            expectedValues = new HashSet<V>(Arrays.asList(values));
        }
        assertValues(multimap, key, expectedValues);
    }

    protected <K, V> void assertValues( Multimap<K, V> multimap,
                                        K key,
                                        Collection<V> expectedValues ) {
        Collection<V> actualValues = multimap.get(key);
        assertThat(actualValues.size(), is(expectedValues.size()));
        if (actualValues instanceof List) {
            assertThat(actualValues, is(expectedValues));
            Iterator<V> actualIter = actualValues.iterator();
            Iterator<V> expectedIter = expectedValues.iterator();
            while (expectedIter.hasNext()) {
                V actual = actualIter.next();
                V expected = expectedIter.next();
                assertThat(actual, is(expected));
            }
        } else {
            assertThat(actualValues.containsAll(expectedValues), is(true));
            assertThat(expectedValues.containsAll(actualValues), is(true));
        }
    }
}
