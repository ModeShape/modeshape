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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

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

    @Test
    @FixFor("MODE-2743")
    public void shouldDecrementSizeOnValueIteratorRemove() {
        assertEquals(0, multimap.size());
        assertTrue(multimap.isEmpty());
        multimap.put(keys[0], values[0]);
        assertEquals(1, multimap.size());
        Iterator<String> iterator = multimap.get(keys[0]).iterator();
        iterator.next();
        iterator.remove();
        assertEquals(0, multimap.size());
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void shouldIterateSuccessfullyWithRemoval() {
        for (String v : values) {
            multimap.put(keys[0], v);
        }
        for (Iterator<String> iter = multimap.get(keys[0]).iterator(); iter.hasNext();) {
            iter.next();
            iter.remove();
        }
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void shouldSuccessfullyRemoveLastElementFromValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        assertTrue(collection.contains(values[0]));

        assertTrue(collection.remove(values[0]));
        assertFalse(collection.contains(values[0]));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());

        assertFalse(collection.remove(values[0]));
        assertFalse(collection.contains(values[0]));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void shouldSuccessfullyAddValueToValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        assertEquals(1, collection.size());
        assertFalse(collection.isEmpty());
        assertEquals(1, multimap.size());
        assertFalse(multimap.isEmpty());

        assertTrue(collection.add(values[1]));
        assertEquals(2, collection.size());
        assertFalse(collection.isEmpty());
        assertEquals(2, multimap.size());
        assertFalse(multimap.isEmpty());
    }

    @Test
    public void shouldSuccessfullyAddValueToEmptyValueCollection() {
        Collection<String> collection = multimap.get(keys[0]);

        assertTrue(collection.add(values[0]));
        assertTrue(collection.contains(values[0]));
        assertFalse(collection.isEmpty());
        assertEquals(1, collection.size());
        assertFalse(multimap.isEmpty());
        assertEquals(1, multimap.size());
    }

    @Test
    public void shouldSuccessfullyAddValueToEmptiedValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);

        assertTrue(collection.remove(values[0]));
        assertFalse(collection.contains(values[0]));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());

        assertTrue(collection.add(values[0]));
        assertTrue(collection.contains(values[0]));
        assertFalse(collection.isEmpty());
        assertEquals(1, collection.size());
        assertFalse(multimap.isEmpty());
        assertEquals(1, multimap.size());
    }

    @Test
    public void shouldSuccessfullyRemoveAllFromValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);

        assertTrue(collection.removeAll(Arrays.asList(values)));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());

        assertFalse(collection.removeAll(Arrays.asList(values)));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void shouldSuccessfullyAddAllToValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);

        collection.addAll(Arrays.asList(values));

        int expectedNumberOfValues = values.length;
        if (valuesAllowDuplicates()) {
            expectedNumberOfValues++;
        }

        assertEquals(expectedNumberOfValues, collection.size());
        assertFalse(collection.isEmpty());

        assertEquals(expectedNumberOfValues, multimap.size());
        assertFalse(multimap.isEmpty());
    }

    @Test
    public void shouldSuccessfullyClearValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);

        collection.clear();
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());

        collection.clear();
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void shouldSuccessfullyAddAllToEmptyValueCollection() {
        Collection<String> collection = multimap.get(keys[0]);

        assertTrue(collection.addAll(Arrays.asList(values)));
        assertEquals(values.length, collection.size());
        assertFalse(collection.isEmpty());
        assertEquals(values.length, multimap.size());
        assertFalse(multimap.isEmpty());
    }

    @Test
    public void shouldSuccessfullyAddAllToEmptiedValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        collection.clear();

        assertTrue(collection.addAll(Arrays.asList(values)));
        assertEquals(values.length, collection.size());
        assertFalse(collection.isEmpty());
        assertEquals(values.length, multimap.size());
        assertFalse(multimap.isEmpty());
    }

    @Test
    public void shouldRetainAllInValueCollection() {
        for (String value : values) {
            multimap.put(keys[0], value);
        }
        assertEquals(values.length, multimap.size());
        Collection<String> collection = multimap.get(keys[0]);
        assertEquals(values.length, collection.size());

        assertFalse(collection.retainAll(Arrays.asList(values)));
        assertEquals(values.length, multimap.size());
        assertEquals(values.length, collection.size());

        assertTrue(collection.retainAll(Collections.singleton(values[0])));
        assertEquals(1, multimap.size());
        assertEquals(1, collection.size());
    }

    @Test
    public void shouldRetainNoneInValueCollection() {
        for (String value : values) {
            multimap.put(keys[0], value);
        }
        assertEquals(values.length, multimap.size());
        Collection<String> collection = multimap.get(keys[0]);
        assertEquals(values.length, collection.size());

        assertTrue(collection.retainAll(Collections.emptySet()));
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
    }

    @Test
    public void shouldSuccessfullyRetainAllInValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);

        assertFalse(collection.retainAll(Arrays.asList(values)));
        assertFalse(collection.isEmpty());
        assertEquals(1, collection.size());
        assertFalse(multimap.isEmpty());
        assertEquals(1, multimap.size());
    }

    @Test
    public void shouldSuccessfullyRetainAllInEmptyValueCollection() {
        Collection<String> collection = multimap.get(keys[0]);

        assertFalse(collection.retainAll(Arrays.asList(values)));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void shouldSuccessfullyRetainAllInEmptiedValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        collection.clear();

        assertFalse(collection.retainAll(Arrays.asList(values)));
        assertTrue(collection.isEmpty());
        assertEquals(0, collection.size());
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void shouldProduceHashCodeOfEmptyValueCollection() {
        multimap.get(keys[0]).hashCode();
    }

    @Test
    public void shouldProduceHashCodeOfEmptiedValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        collection.clear();
        collection.hashCode();
    }

    @Test
    public void shouldCompareEqualityOfEmptyValueCollection() {
        Collection<String> collection = multimap.get(keys[0]);

        assertNotEquals(this, collection);
        assertNotEquals(collection, this);
        if (collection instanceof List<?>) {
            assertEquals(Collections.emptyList(), collection);
            assertEquals(collection, Collections.emptyList());
        } else if (collection instanceof Set<?>) {
            assertEquals(Collections.emptySet(), collection);
            assertEquals(collection, Collections.emptySet());
        }
    }

    @Test
    public void shouldCompareEqualityOfEmptiedValueCollection() {
        multimap.put(keys[0], values[0]);
        Collection<String> collection = multimap.get(keys[0]);
        collection.clear();

        assertNotEquals(this, collection);
        assertNotEquals(collection, this);
        if (collection instanceof List<?>) {
            assertEquals(Collections.emptyList(), collection);
            assertEquals(collection, Collections.emptyList());
        } else if (collection instanceof Set<?>) {
            assertEquals(Collections.emptySet(), collection);
            assertEquals(collection, Collections.emptySet());
        }
    }

    protected <K, V> Map.Entry<K, V> entry( K key,
                                            V value ) {
        return new ImmutableMapEntry<K, V>(key, value);
    }

    protected <K, V> void assertEntries( Multimap<K, V> multimap,
                                         Map.Entry<K, V> entry ) {
        assertEntries(multimap, java.util.Collections.singletonList(entry));
    }

    @SafeVarargs
    protected final <K, V> void assertEntries( Multimap<K, V> multimap, Map.Entry<K, V>... entries ) {
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

    @SafeVarargs
    protected final <K, V> void assertKeys( Multimap<K, V> multimap, K... keys ) {
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

    @SafeVarargs
    protected final <K, V> void assertValues( Multimap<K, V> multimap, K key, V... values ) {
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
