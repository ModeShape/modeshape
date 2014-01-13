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
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class LinkedListMultimapTest extends AbstractMultimapTest {

    protected ListMultimap<String, String> listMultimap;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        listMultimap = (ListMultimap<String, String>)multimap;
    }

    @Override
    protected <K, V> LinkedListMultimap<K, V> createMultimap() {
        return LinkedListMultimap.create();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.AbstractMultimapTest#valuesAllowDuplicates()
     */
    @Override
    protected boolean valuesAllowDuplicates() {
        return true;
    }

    @Test
    public void shouldAddEntryToEmtpyMap() {
        LinkedListMultimap<String, String> listMap = createMultimap();
        assertThat(listMap.size(), is(0));
        listMap.addEntryFor(keys[0], values[0]);
        assertThat(listMap.size(), is(1));
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValues() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[2]);
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(6));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[0], values[0]);
        assertValues(multimap, keys[1], values[1], values[2], values[3]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[2]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[3]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfFirstKeyValuePair() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        multimap.remove(keys[0], values[0]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], /*values[0],*/values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        // entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfMiddleKeyValuePair() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[2]);
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[3]);
        multimap.remove(keys[1], values[2]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[0], values[0]);
        assertValues(multimap, keys[1], values[1], values[3]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[3]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfLastKeyValuePair() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        multimap.remove(keys[1], values[2]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1] /*, values[2]*/);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        // entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfFirstValueFromKeyCollection() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        multimap.get(keys[0]).remove(values[0]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], /*values[0],*/values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        // entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfMiddleValueFromKeyCollection() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        multimap.get(keys[1]).remove(values[1]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterRemovalOfLastValueFromKeyCollection() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        multimap.get(keys[1]).remove(values[2]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(5));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1] /*, values[2]*/);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        // entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtBeginngingOfCollectionForKey() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingCollection(listMultimap, keys[0], 0, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[3], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[3]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtMiddleOfCollectionForKey() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingCollection(listMultimap, keys[1], 1, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[3], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[3]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtEndOfCollectionForKey() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingCollection(listMultimap, keys[1], 3, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2], values[3]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        entries.add(entry(keys[1], values[3]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtBeginngingOfCollectionForKeyUsingIterator() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingIterator(listMultimap, keys[0], 0, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[3], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[3]));
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtMiddleOfCollectionForKeyUsingIterator() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingIterator(listMultimap, keys[1], 1, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[3], values[1], values[2]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[3]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        assertEntries(multimap, entries);
    }

    @Test
    public void shouldMaintainOrderOfInsertionInKeysAndValuesAfterAddingValueAtEndOfCollectionForKeyUsingIterator() {
        multimap.put(keys[0], values[0]);
        multimap.put(keys[1], values[0]);
        multimap.put(keys[0], values[1]);
        multimap.put(keys[1], values[1]);
        multimap.put(keys[0], values[2]);
        multimap.put(keys[1], values[2]);
        addValueUsingIterator(listMultimap, keys[1], 3, values[3]);
        assertThat(multimap.isEmpty(), is(false));
        assertThat(multimap.size(), is(7));
        assertKeys(multimap, keys[0], keys[1]);
        assertValues(multimap, keys[0], values[0], values[1], values[2]);
        assertValues(multimap, keys[1], values[0], values[1], values[2], values[3]);
        Collection<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>();
        entries.add(entry(keys[0], values[0]));
        entries.add(entry(keys[1], values[0]));
        entries.add(entry(keys[0], values[1]));
        entries.add(entry(keys[1], values[1]));
        entries.add(entry(keys[0], values[2]));
        entries.add(entry(keys[1], values[2]));
        entries.add(entry(keys[1], values[3]));
        assertEntries(multimap, entries);
    }

    protected void addValueUsingCollection( ListMultimap<String, String> multimap,
                                            String key,
                                            int atPosition,
                                            String value ) {
        List<String> values = multimap.get(key);
        values.add(atPosition, value);
    }

    protected void addValueUsingIterator( ListMultimap<String, String> multimap,
                                          String key,
                                          int atPosition,
                                          String value ) {
        List<String> values = multimap.get(key);
        ListIterator<String> iter = values.listIterator();
        while (iter.hasNext()) {
            if (iter.nextIndex() == atPosition) {
                iter.add(value); // will insert before the 'next' value that was at the 'atPosition' index
                return;
            }
            iter.next();
        }
        iter.add(value); // at the end
    }

    protected void setValueUsingCollection( ListMultimap<String, String> multimap,
                                            String key,
                                            int atPosition,
                                            String value ) {
        List<String> values = multimap.get(key);
        values.set(atPosition, value);
    }

    protected void setValueUsingIterator( ListMultimap<String, String> multimap,
                                          String key,
                                          int atPosition,
                                          String value ) {
        List<String> values = multimap.get(key);
        ListIterator<String> iter = values.listIterator();
        while (iter.hasNext()) {
            if (iter.nextIndex() == atPosition) {
                iter.set(value);
                break;
            }
            iter.next();
        }
    }

}
