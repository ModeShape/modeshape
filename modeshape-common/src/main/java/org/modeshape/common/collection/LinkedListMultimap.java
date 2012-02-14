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

    protected LinkedListMultimap() {
        this.firstEntryWithKey = new HashMap<K, Entry<K, V>>();
        this.lastEntryWithKey = new HashMap<K, Entry<K, V>>();
        this.numberOfEntriesForKey = new HashMap<K, AtomicInteger>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#clear()
     */
    @Override
    public void clear() {
        length = 0;
        firstEntryWithKey.clear();
        lastEntryWithKey.clear();
        numberOfEntriesForKey.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#size()
     */
    @Override
    public int size() {
        return length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey( K key ) {
        return firstEntryWithKey.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsEntry(java.lang.Object, java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue( Object value ) {
        EntryIterator entries = new EntryIterator();
        while (entries.hasNext()) {
            if (ObjectUtil.isEqualWithNulls(entries.next().value, value)) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.ListMultimap#get(java.lang.Object)
     */
    @Override
    public List<V> get( final K key ) {
        return new AbstractSequentialList<V>() {
            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#size()
             */
            @Override
            public int size() {
                return currentCountFor(key);
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractSequentialList#listIterator(int)
             */
            @Override
            public ListIterator<V> listIterator( int index ) {
                return new ValueIterator(key, index);
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean put( K key,
                        V value ) {
        addEntryFor(key, value);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#remove(java.lang.Object, java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#removeAll(java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#entries()
     */
    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return new AbstractCollection<Map.Entry<K, V>>() {
            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#size()
             */
            @Override
            public int size() {
                return length;
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#iterator()
             */
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    private final EntryIterator iter = new EntryIterator();

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#hasNext()
                     */
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#next()
                     */
                    @Override
                    public Map.Entry<K, V> next() {
                        Entry<K, V> next = iter.next();
                        return new ImmutableMapEntry<K, V>(next.key, next.value);
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#remove()
                     */
                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#keySet()
     */
    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#size()
             */
            @Override
            public int size() {
                return numberOfEntriesForKey.size();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#iterator()
             */
            @Override
            public Iterator<K> iterator() {
                return new KeyIterator();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#values()
     */
    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#size()
             */
            @Override
            public int size() {
                return length;
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#iterator()
             */
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private final EntryIterator iter = new EntryIterator();

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#hasNext()
                     */
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#next()
                     */
                    @Override
                    public V next() {
                        Entry<K, V> next = iter.next();
                        return next.value;
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @see java.util.Iterator#remove()
                     */
                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    private transient Map<K, Collection<V>> mapView;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#asMap()
     */
    @Override
    public Map<K, Collection<V>> asMap() {
        if (mapView == null) mapView = new MapView();
        return mapView;
    }

    protected class MapView extends AbstractMap<K, Collection<V>> {

        private Set<Map.Entry<K, Collection<V>>> entries;

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#entrySet()
         */
        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            if (entries == null) entries = new MapEntries();
            return entries;
        }
    }

    protected class MapEntries extends AbstractSet<Map.Entry<K, Collection<V>>> {
        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return length;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#iterator()
         */
        @Override
        public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            return new Iterator<Map.Entry<K, Collection<V>>>() {
                private KeyIterator iter = new KeyIterator();

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#hasNext()
                 */
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#next()
                 */
                @Override
                public Map.Entry<K, Collection<V>> next() {
                    final K nextKey = iter.next();
                    return new ImmutableMapEntry<K, Collection<V>>(nextKey, null) {
                        /**
                         * {@inheritDoc}
                         * 
                         * @see org.modeshape.common.collection.ImmutableMapEntry#getValue()
                         */
                        @Override
                        public Collection<V> getValue() {
                            return get(nextKey);
                        }
                    };
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#remove()
                 */
                @Override
                public void remove() {
                    iter.remove();
                }
            };
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Multimap) {
            Multimap<?, ?> that = (Multimap<?, ?>)obj;
            return this.asMap().equals(that.asMap());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public Entry<K, V> next() {
            isValid(nextEntry);
            currentEntry = nextEntry;
            nextEntry = nextEntry.next;
            return currentEntry;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return next != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public V next() {
            isValid(next);
            current = next;
            previous = next;
            next = current.nextWithKey;
            ++nextIndex;
            return current.value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#nextIndex()
         */
        @Override
        public int nextIndex() {
            return nextIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#hasPrevious()
         */
        @Override
        public boolean hasPrevious() {
            return previous != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#previous()
         */
        @Override
        public V previous() {
            isValid(previous);
            current = previous;
            next = previous;
            previous = current.previousWithKey;
            --nextIndex;
            return current.value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#previousIndex()
         */
        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#add(java.lang.Object)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.ListIterator#set(java.lang.Object)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return next != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            if (current == null) throw new NoSuchElementException();
            removeAll(current.key);
            current = null;
        }
    }

}
