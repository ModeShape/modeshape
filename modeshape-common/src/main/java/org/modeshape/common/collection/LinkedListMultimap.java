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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.modeshape.common.util.ObjectUtil;

/**
 * A {@link Multimap} implementation that uses an {@link LinkedList} to store the values associated with a key. This
 * implementation allows duplicate values for a key, maintains the insertion-order of the {@link #get(Object) values for each key}
 * , and maintains the insertion order of all {@link #entries() key-value pairs}.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public final class LinkedListMultimap<K, V> implements ListMultimap<K, V> {

    /**
     * Creates a new, empty {@code LinkedListMultimap} with the default initial capacity.
     * 
     * @param <K> the key type
     * @param <V> the value type
     * @return the new linked-list multimap; never null
     */
    public static <K, V> LinkedListMultimap<K, V> create() {
        return new LinkedListMultimap<K, V>();
    }

    protected static final class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> nextWithKey;
        Entry<K, V> previousWithKey;
        Entry<K, V> next;
        Entry<K, V> previous;

        protected Entry( K key,
                         V value ) {
            this.key = key;
            this.value = value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + key + "=" + value + "]";
        }
    }

    protected Entry<K, V> firstEntry;
    protected Entry<K, V> lastEntry;
    protected int length;
    protected final Map<K, AtomicInteger> numberOfEntriesForKey;
    protected final Map<K, Entry<K, V>> firstEntryWithKey;
    protected final Map<K, Entry<K, V>> lastEntryWithKey;

    private transient Map<K, Collection<V>> mapView;

    protected LinkedListMultimap() {
        this.firstEntryWithKey = new HashMap<K, Entry<K, V>>();
        this.lastEntryWithKey = new HashMap<K, Entry<K, V>>();
        this.numberOfEntriesForKey = new HashMap<K, AtomicInteger>();
    }

    @Override
    public void clear() {
        length = 0;
        firstEntryWithKey.clear();
        lastEntryWithKey.clear();
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    @Override
    public boolean containsKey( K key ) {
        return firstEntryWithKey.containsKey(key);
    }

    @Override
    public boolean containsEntry( Object key,
                                  Object value ) {
        @SuppressWarnings( "unchecked" )
        ValueIterator values = new ValueIterator((K)key);
        while (values.hasNext()) {
            if (ObjectUtil.isEqualWithNulls(values.next(), value)) return true;
        }
        return false;
    }

    @Override
    public boolean containsValue( Object value ) {
        EntryIterator entries = new EntryIterator();
        while (entries.hasNext()) {
            if (ObjectUtil.isEqualWithNulls(entries.next().value, value)) return true;
        }
        return false;
    }

    @Override
    public List<V> get( final K key ) {
        return new AbstractSequentialList<V>() {
            @Override
            public int size() {
                return currentCountFor(key);
            }

            @Override
            public ListIterator<V> listIterator( int index ) {
                return new ValueIterator(key, index);
            }
        };
    }

    @Override
    public boolean put( K key,
                        V value ) {
        addEntryFor(key, value);
        return true;
    }

    @Override
    public boolean remove( K key,
                           V value ) {
        // Find the entry with the value ...
        ValueIterator values = new ValueIterator(key);
        while (values.hasNext()) {
            if (ObjectUtil.isEqualWithNulls(values.next(), value)) {
                values.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<V> removeAll( K key ) {
        // Find the entries with the value ...
        List<V> result = new ArrayList<V>(currentCountFor(key));
        ValueIterator values = new ValueIterator(key);
        while (values.hasNext()) {
            result.add(values.next());
            values.remove();
        }
        return result;
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return new AbstractCollection<Map.Entry<K, V>>() {
            @Override
            public int size() {
                return length;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    private final EntryIterator iter = new EntryIterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Entry<K, V> next = iter.next();
                        return new ImmutableMapEntry<K, V>(next.key, next.value);
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public int size() {
                return numberOfEntriesForKey.size();
            }

            @Override
            public Iterator<K> iterator() {
                return new KeyIterator();
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public int size() {
                return length;
            }

            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private final EntryIterator iter = new EntryIterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public V next() {
                        Entry<K, V> next = iter.next();
                        return next.value;
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        if (mapView == null) mapView = new MapView();
        return mapView;
    }

    protected class MapView extends AbstractMap<K, Collection<V>> {

        private Set<Map.Entry<K, Collection<V>>> entries;

        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            if (entries == null) entries = new MapEntries();
            return entries;
        }
    }

    protected class MapEntries extends AbstractSet<Map.Entry<K, Collection<V>>> {
        @Override
        public int size() {
            return length;
        }

        @Override
        public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            return new Iterator<Map.Entry<K, Collection<V>>>() {
                private KeyIterator iter = new KeyIterator();

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Map.Entry<K, Collection<V>> next() {
                    final K nextKey = iter.next();
                    return new ImmutableMapEntry<K, Collection<V>>(nextKey, null) {
                        @Override
                        public Collection<V> getValue() {
                            return get(nextKey);
                        }
                    };
                }

                @Override
                public void remove() {
                    iter.remove();
                }
            };
        }
    }

    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Multimap) {
            Multimap<?, ?> that = (Multimap<?, ?>)obj;
            return this.asMap().equals(that.asMap());
        }
        return false;
    }

    @Override
    public String toString() {
        return asMap().toString();
    }

    protected int incrementEntryCountFor( K key ) {
        AtomicInteger count = numberOfEntriesForKey.get(key);
        if (count == null) {
            count = new AtomicInteger(0);
            numberOfEntriesForKey.put(key, count);
        }
        ++length;
        return count.incrementAndGet();
    }

    protected int decrementEntryCountFor( K key ) {
        AtomicInteger count = numberOfEntriesForKey.get(key);
        assert count != null;
        int result = count.decrementAndGet();
        if (result == 0) numberOfEntriesForKey.remove(key);
        --length;
        return result;
    }

    protected int currentCountFor( K key ) {
        AtomicInteger count = numberOfEntriesForKey.get(key);
        return count != null ? count.get() : 0;
    }

    protected Entry<K, V> addEntryFor( K key,
                                       V value ) {
        Entry<K, V> entry = new Entry<K, V>(key, value);
        if (firstEntry == null) {
            assert length == 0;
            // This is the first entry ...
            firstEntry = entry;
            firstEntryWithKey.put(key, entry);
        } else {
            // Add this entry to the end ...
            lastEntry.next = entry;
            entry.previous = lastEntry;
            // Find the last entry with this key ...
            Entry<K, V> lastWithKey = lastEntryWithKey.get(key);
            if (lastWithKey == null) {
                // This is the first entry for this key ...
                firstEntryWithKey.put(key, entry);
            }
        }

        // This entry is always the last entry ...
        Entry<K, V> previousLastWithKey = lastEntryWithKey.put(key, entry);
        if (previousLastWithKey != null) {
            previousLastWithKey.nextWithKey = entry;
            entry.previousWithKey = previousLastWithKey;
        }
        lastEntry = entry;
        incrementEntryCountFor(key);
        return entry;
    }

    protected Entry<K, V> insertEntryBefore( K key,
                                             V value,
                                             Entry<K, V> nextEntryWithKey ) {
        if (nextEntryWithKey == null) return addEntryFor(key, value);

        // Otherwise the new entry will lever be last ...
        Entry<K, V> entry = new Entry<K, V>(key, value);
        assert firstEntry != null;
        assert length != 0;
        // Set the entry's references ...
        entry.next = nextEntryWithKey;
        entry.nextWithKey = nextEntryWithKey;
        entry.previous = nextEntryWithKey.previous;
        entry.previousWithKey = nextEntryWithKey.previousWithKey;
        // Set the reference from the entry that WAS before the next entry ...
        if (nextEntryWithKey.previousWithKey != null) {
            // This will NOT be the first entry for this key ...
            nextEntryWithKey.previousWithKey.nextWithKey = entry;
        } else {
            // This will be the first entry for this key ...
            firstEntryWithKey.put(key, entry);
        }
        if (nextEntryWithKey.previous != null) {
            // This will NOT be the first entry ...
            nextEntryWithKey.previous.next = entry;
        } else {
            // This will be the first entry ...
            firstEntry = entry;
        }
        // Set the reference from the next entry ...
        nextEntryWithKey.previousWithKey = entry;
        nextEntryWithKey.previous = entry;
        incrementEntryCountFor(key);
        return entry;
    }

    protected void removeEntry( Entry<K, V> entry ) {
        if (entry.previous == null) {
            // The entry was the first ...
            assert firstEntry == entry;
            firstEntry = entry.next;
        } else {
            // Just a normal entry that is not at the front ...
            entry.previous.next = entry.next;
        }
        if (entry.next == null) {
            // The entry was the last ...
            assert lastEntry == entry;
            lastEntry = entry.previous;
        } else {
            // Just a normal entry that is not at the ed ...
            entry.next.previous = entry.previous;
        }
        if (entry.previousWithKey != null) {
            entry.previousWithKey.nextWithKey = entry.nextWithKey;
        } else if (entry.nextWithKey != null) {
            // We're removing the first entry with the key, and there is a next entry with the key ..
            firstEntryWithKey.put(entry.key, entry.nextWithKey);
        } else {
            // We're removing the first and only entry with the key ...
            firstEntryWithKey.remove(entry.key);
        }
        if (entry.nextWithKey != null) {
            entry.nextWithKey.previousWithKey = entry.previousWithKey;
        } else if (entry.previousWithKey != null) {
            // We're removing the last entry with the key, and there is a previoius entry with the key ..
            lastEntryWithKey.put(entry.key, entry.previousWithKey);
        } else {
            // We're removing the last and only entry with the key ...
            lastEntryWithKey.remove(entry.key);
        }
        decrementEntryCountFor(entry.key);
    }

    protected void isValid( Entry<K, V> entry ) {
        if (entry == null) throw new NoSuchElementException();
    }

    protected class EntryIterator implements Iterator<Entry<K, V>> {
        private Entry<K, V> nextEntry;
        private Entry<K, V> currentEntry;

        protected EntryIterator() {
            nextEntry = firstEntry;
        }

        protected EntryIterator( Entry<K, V> first ) {
            nextEntry = first;
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public Entry<K, V> next() {
            isValid(nextEntry);
            currentEntry = nextEntry;
            nextEntry = nextEntry.next;
            return currentEntry;
        }

        @Override
        public void remove() {
            if (currentEntry == null) throw new IllegalStateException();
            removeEntry(currentEntry);
            currentEntry = null;
        }
    }

    protected class ValueIterator implements ListIterator<V> {
        private Entry<K, V> previous;
        private Entry<K, V> current;
        private Entry<K, V> next;
        private int nextIndex;
        private final K key;

        protected ValueIterator( K key ) {
            this.key = key;
            next = firstEntryWithKey.get(key);
            nextIndex = 0;
        }

        protected ValueIterator( K key,
                                 int index ) {
            this.key = key;
            int count = currentCountFor(key);
            if (index > count / 2) {
                // The index is closer to the end, so start at the end ..
                previous = lastEntryWithKey.get(key);
                nextIndex = count;
                while (index++ < count) {
                    previous();
                }
            } else {
                // The index is closer to the front, so start at the front ...
                next = firstEntryWithKey.get(key);
                nextIndex = 0;
                while (index-- > 0) {
                    next();
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public V next() {
            isValid(next);
            current = next;
            previous = next;
            next = current.nextWithKey;
            ++nextIndex;
            return current.value;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public boolean hasPrevious() {
            return previous != null;
        }

        @Override
        public V previous() {
            isValid(previous);
            current = previous;
            next = previous;
            previous = current.previousWithKey;
            --nextIndex;
            return current.value;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            if (current == null) throw new IllegalStateException();
            if (current != next) {
                // removing next element
                previous = current.previousWithKey;
                nextIndex--;
            } else {
                next = current.nextWithKey;
            }
            removeEntry(current);
            current = null;
        }

        @Override
        public void add( V value ) {
            if (next == null) {
                previous = addEntryFor(key, value);
            } else {
                previous = insertEntryBefore(key, value, next);
            }
            nextIndex++;
            current = null;
        }

        @Override
        public void set( V value ) {
            if (current == null) throw new IllegalStateException();
            current.value = value;
        }
    }

    protected class KeyIterator implements Iterator<K> {
        private final Set<K> seen = new HashSet<K>();
        private Entry<K, V> next = firstEntry;
        private Entry<K, V> current = null;

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public K next() {
            if (next == null) throw new NoSuchElementException();
            current = next;
            seen.add(current.key);
            do {
                next = next.next;
            } while (next != null && !seen.add(next.key));
            return current.key;
        }

        @Override
        public void remove() {
            if (current == null) throw new NoSuchElementException();
            removeAll(current.key);
            current = null;
        }
    }

}
