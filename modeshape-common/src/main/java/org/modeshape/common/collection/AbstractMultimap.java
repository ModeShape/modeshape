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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * A {@link Multimap} implementation that uses an {@link ArrayList} to store the values associated with a key. This implementation
 * allows duplicates.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class AbstractMultimap<K, V> implements Multimap<K, V> {

    private final Map<K, Collection<V>> data;
    private int totalSize;
    private transient Set<K> keysView;
    private transient Map<K, Collection<V>> mapView;
    private transient Collection<V> valuesCollection;
    private transient Collection<Map.Entry<K, V>> entriesCollection;

    protected AbstractMultimap( Map<K, Collection<V>> entries ) {
        assert entries != null;
        this.data = entries;
    }

    protected Map<K, Collection<V>> rawData() {
        return data;
    }

    protected abstract Collection<V> createCollection();

    protected abstract Collection<V> createUnmodifiableEmptyCollection();

    protected Collection<V> createUnmodifiable( Collection<V> original ) {
        if (original instanceof List<?>) {
            return Collections.unmodifiableList((List<V>) original);
        }
        if (original instanceof Set<?>) {
            return Collections.unmodifiableSet((Set<V>) original);
        }
        return Collections.unmodifiableCollection(original);
    }

    @Override
    public int size() {
        return totalSize;
    }

    void incrementSize( int n ) {
        totalSize += n;
    }

    void decrementSize( int n ) {
        totalSize -= n;
        assert totalSize >= 0;
    }

    @Override
    public boolean isEmpty() {
        return totalSize == 0;
    }

    @Override
    public boolean containsKey( K key ) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue( Object value ) {
        return data.values().stream().anyMatch(c -> c.contains(value));
    }

    @Override
    public boolean containsEntry( Object key,
                                  Object value ) {
        Collection<V> values = data.get(key);
        return values != null && values.contains(value);
    }

    @Override
    public Collection<V> get( K key ) {
        Collection<V> collection = data.get(key);
        if (collection == null) {
            collection = createCollection();
        }
        return wrapCollection(key, collection);
    }

    @Override
    public boolean put( K key,
                        V value ) {
        if (getOrCreateCollection(key).add(value)) {
            incrementSize(1);
            return true;
        }
        return false;
    }

    protected Collection<V> getOrCreateCollection( K key ) {
        return data.computeIfAbsent(key, k -> createCollection());
    }

    @Override
    public boolean remove( K key,
                           V value ) {
        Collection<V> values = data.get(key);
        if (values == null) return false;

        if (!values.remove(value)) return false;
        decrementSize(1);
        if (values.isEmpty()) {
            data.remove(key);
        }
        return true;
    }

    @Override
    public Collection<V> removeAll( K key ) {
        Collection<V> values = data.remove(key);
        if (values == null) return createUnmodifiableEmptyCollection();
        Collection<V> copy = createCollection();
        copy.addAll(values);
        decrementSize(values.size());
        values.clear();
        return createUnmodifiable(copy);
    }

    protected boolean removeAllValuesForKey( Object key ) {
        Collection<V> values = data.remove(key);
        if (values == null) return false;
        decrementSize(values.size());
        values.clear();
        return true;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return data.equals(obj);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public Set<K> keySet() {
        if (keysView == null) keysView = wrapKeySet();
        return keysView;
    }

    @Override
    public void clear() {
        totalSize = 0;
        data.clear();
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        if (mapView == null) mapView = wrapMap(data);
        return mapView;
    }

    /**
     * Return an iterator over all entries in this multimap.
     * 
     * @return the entry iterator; never null
     */
    protected Iterator<Map.Entry<K, V>> createEntryIterator() {
        return new EntryIterator();
    }

    protected Collection<V> wrapCollection( K key,
                                            Collection<V> values ) {
        if (values instanceof List) {
            return wrapList(key, (List<V>)values);
        }
        return new WrappedCollection(key, values);
    }

    protected List<V> wrapList( K key,
                                List<V> values ) {
        return new WrappedList(key, values);
    }

    protected Map<K, Collection<V>> wrapMap( Map<K, Collection<V>> map ) {
        return new WrappedMap(map);
    }

    protected Set<K> wrapKeySet() {
        if (data instanceof SortedMap) {
            return new WrappedSortedKeySet((SortedMap<K, Collection<V>>)data);
        }
        return new WrappedKeySet(data);
    }

    @Override
    public Collection<V> values() {
        if (valuesCollection == null) valuesCollection = new ValuesCollection();
        return valuesCollection;
    }

    protected final class ValuesCollection extends AbstractCollection<V> {
        @Override
        public int size() {
            return AbstractMultimap.this.size();
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public void clear() {
            AbstractMultimap.this.clear();
        }

        @Override
        public boolean contains( Object o ) {
            return AbstractMultimap.this.containsValue(o);
        }
    }

    protected final class ValueIterator implements Iterator<V> {
        private final Iterator<Map.Entry<K, V>> entryIterator = createEntryIterator();

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @Override
        public V next() {
            return entryIterator.next().getValue();
        }

        @Override
        public void remove() {
            entryIterator.remove();
        }
    }

    @Override
    public Collection<Entry<K, V>> entries() {
        if (entriesCollection == null) entriesCollection = new EntriesCollection();
        return entriesCollection;
    }

    protected final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K, Collection<V>>> iter;
        protected K currentKey;
        protected Collection<V> currentValues;
        protected Iterator<V> currentValuesIterator;

        EntryIterator() {
            iter = rawData().entrySet().iterator();
            if (iter.hasNext()) {
                nextKey();
            } else {
                currentValuesIterator = new EmptyIterator<>();
            }
        }

        protected void nextKey() {
            Map.Entry<K, Collection<V>> entry = iter.next();
            currentKey = entry.getKey();
            currentValues = entry.getValue();
            currentValuesIterator = currentValues.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext() || currentValuesIterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            if (!currentValuesIterator.hasNext()) {
                nextKey();
            }
            return new ImmutableMapEntry<>(currentKey, currentValuesIterator.next());
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void remove() {
            currentValuesIterator.remove();
            if (currentValues.isEmpty()) {
                iter.remove();
            }
            --totalSize;
        }
    }

    protected final class EntriesCollection extends AbstractCollection<Map.Entry<K, V>> {
        @Override
        public int size() {
            return AbstractMultimap.this.size();
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public void clear() {
            AbstractMultimap.this.clear();
        }

        @Override
        public boolean contains( Object o ) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> that = (Map.Entry<?, ?>)o;
            return AbstractMultimap.this.containsEntry(that.getKey(), that.getValue());
        }
    }

    protected class WrappedCollection implements Collection<V> {
        private final K key;
        private Collection<V> delegate;

        protected WrappedCollection( K key,
                                     Collection<V> values ) {
            this.key = key;
            this.delegate = values;
        }

        public K getKey() {
            return key;
        }

        protected Collection<V> delegate() {
            if (delegate == null || delegate.isEmpty()) {
                // Fetch the actual values from the multimap's raw data ...
                Collection<V> rawValues = rawData().get(key);
                if (rawValues != null) {
                    // Always use the raw values ...
                    delegate = rawValues;
                }
            }
            return delegate;
        }

        protected final void removeIfEmpty() {
            if (delegate != null && delegate.isEmpty()) {
                AbstractMultimap.this.removeAll(key);
                delegate = null;
            }
        }

        protected final void addToMap() {
            rawData().put(key, delegate);
        }

        @Override
        public int size() {
            Collection<V> d = delegate();
            return d == null ? 0 : d.size();
        }

        @Override
        public boolean isEmpty() {
            Collection<V> d = delegate();
            return d == null || d.isEmpty();
        }

        @Override
        public boolean contains( Object o ) {
            Collection<V> d = delegate();
            return d != null && d.contains(o);
        }

        @Override
        public Iterator<V> iterator() {
            return new DelegateIterator();
        }

        @Override
        public Object[] toArray() {
            // The size of the values (and thus the multimap) cannot be changed via the resulting array, so no need to wrap ...
            return delegate().toArray();
        }

        @Override
        public <T> T[] toArray( T[] a ) {
            // The size of the values (and thus the multimap) cannot be changed via the resulting array, so no need to wrap ...
            return delegate().toArray(a);
        }

        @Override
        public boolean add( V e ) {
            boolean wasEmpty = isEmpty();
            try {
                return AbstractMultimap.this.put(key, e);
            } finally {
                if (wasEmpty) {
                    delegate();
                }
            }
        }

        @Override
        public boolean remove( Object o ) {
            Collection<V> d = delegate();
            if (d == null || !d.remove(o)) return false;
            decrementSize(1);
            removeIfEmpty();
            return true;
        }

        @Override
        public boolean containsAll( Collection<?> c ) {
            return delegate().containsAll(c);
        }

        @Override
        public boolean addAll( Collection<? extends V> c ) {
            if (c.isEmpty()) return false;
            if (isEmpty()) getOrCreateCollection(key);
            Collection<V> d = delegate();
            int sizeBefore = d.size();
            if (!d.addAll(c)) return false;
            incrementSize(d.size() - sizeBefore);
            return true;
        }

        @Override
        public boolean removeAll( Collection<?> c ) {
            if (c.isEmpty() || isEmpty()) {
                return false;
            }
            Collection<V> d = delegate();
            int sizeBefore = d.size();
            if (!d.removeAll(c)) return false;
            incrementSize(d.size() - sizeBefore); // will be negative
            removeIfEmpty();
            return true;
        }

        @Override
        public boolean retainAll( Collection<?> c ) {
            if (isEmpty()) {
                return false;
            }
            Collection<V> d = delegate();
            int sizeBefore = d.size();
            if (!d.retainAll(c)) return false;
            int diff = d.size() - sizeBefore;
            incrementSize(diff); // will change correctly if diff is negative
            return true;
        }

        @Override
        public void clear() {
            if (isEmpty()) return;
            Collection<V> d = delegate();
            int sizeBefore = d.size();
            d.clear();
            decrementSize(sizeBefore);
            removeIfEmpty();
        }

        @Override
        public int hashCode() {
            return isEmpty() ? Collections.EMPTY_SET.hashCode() : delegate().hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this
                   || Optional.ofNullable(delegate()).orElseGet(AbstractMultimap.this::createUnmodifiableEmptyCollection).equals(obj);
        }

        @Override
        public String toString() {
            return delegate().toString();
        }

        protected class DelegateIterator implements Iterator<V> {
            private final Iterator<V> iterator;
            private Collection<V> source;

            protected DelegateIterator() {
                source = delegate();
                iterator = source instanceof List ? ((List<V>)source).listIterator() : source.iterator();
            }

            protected DelegateIterator( int index ) {
                this.source = delegate();
                iterator = source instanceof List ? ((List<V>)source).listIterator(index) : source.iterator();
            }

            protected Iterator<V> iterator() {
                if (source != delegate()) {
                    throw new ConcurrentModificationException();
                }
                return iterator;
            }

            @Override
            public boolean hasNext() {
                return iterator().hasNext();
            }

            @Override
            public V next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
                decrementSize(1);
                removeIfEmpty();
                if (isEmpty()) {
                    source = null;
                }
            }
        }
    }

    protected class WrappedList extends WrappedCollection implements List<V> {

        protected WrappedList( K key,
                               Collection<V> values ) {
            super(key, values);
        }

        @Override
        protected List<V> delegate() {
            return (List<V>)super.delegate();
        }

        @Override
        public boolean addAll( int index,
                               Collection<? extends V> c ) {
            if (c.isEmpty()) return false;
            List<V> delegate = delegate();
            int sizeBefore = delegate.size();
            if (!delegate.addAll(index, c)) return false;
            incrementSize(delegate.size() - sizeBefore);
            if (sizeBefore == 0) addToMap();
            return true;
        }

        @Override
        public V get( int index ) {
            return delegate().get(index);
        }

        @Override
        public V set( int index,
                      V element ) {
            return delegate().set(index, element);
        }

        @Override
        public void add( int index,
                         V element ) {
            List<V> values = delegate();
            boolean wasEmpty = values.isEmpty();
            values.add(index, element);
            incrementSize(1);
            if (wasEmpty) addToMap();
        }

        @Override
        public V remove( int index ) {
            V removed = delegate().remove(index);
            decrementSize(1);
            removeIfEmpty();
            return removed;
        }

        @Override
        public int indexOf( Object o ) {
            return delegate().indexOf(o);
        }

        @Override
        public int lastIndexOf( Object o ) {
            return delegate().lastIndexOf(o);
        }

        @Override
        public ListIterator<V> listIterator() {
            return new DelegateListIterator();
        }

        @Override
        public ListIterator<V> listIterator( int index ) {
            return new DelegateListIterator(index);
        }

        @Override
        public List<V> subList( int fromIndex,
                                int toIndex ) {
            List<V> valueSublist = delegate().subList(fromIndex, toIndex);
            return wrapList(getKey(), valueSublist);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.modeshape.common.collection.AbstractMultimap.WrappedCollection#hashCode()
         */
        @Override
        public int hashCode() {
            return isEmpty() ? Collections.EMPTY_LIST.hashCode() : delegate().hashCode();
        }

        protected class DelegateListIterator extends DelegateIterator implements ListIterator<V> {

            protected DelegateListIterator() {
                super();
            }

            protected DelegateListIterator( int index ) {
                super(index);
            }

            @Override
            protected ListIterator<V> iterator() {
                return (ListIterator<V>)super.iterator();
            }

            @Override
            public boolean hasPrevious() {
                return iterator().hasPrevious();
            }

            @Override
            public void add( V e ) {
                iterator().add(e);
                incrementSize(1);

            }

            @Override
            public int nextIndex() {
                return iterator().nextIndex();
            }

            @Override
            public V previous() {
                return iterator().previous();
            }

            @Override
            public int previousIndex() {
                return iterator().previousIndex();
            }

            @Override
            public void set( V e ) {
                iterator().set(e);
            }
        }
    }

    protected class WrappedMap extends AbstractMap<K, Collection<V>> {

        private transient Set<Map.Entry<K, Collection<V>>> entries;
        private Map<K, Collection<V>> delegate;

        protected WrappedMap( Map<K, Collection<V>> wrapped ) {
            this.delegate = wrapped;
        }

        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            if (entries == null) entries = new WrappedEntrySet();
            return entries;
        }

        protected Map<K, Collection<V>> delegate() {
            return delegate;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            if (o == this) return true;
            return delegate.equals(o);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Collection<V> get( Object key ) {
            return AbstractMultimap.this.get((K)key);
        }

        @Override
        public Collection<V> remove( Object key ) {
            Collection<V> values = rawData().remove(key);
            if (values != null) {
                // Now create a copy of the data, since someone might be holding a reference to the existing values ...
                Collection<V> copy = createCollection();
                copy.addAll(values);
                decrementSize(values.size());
                values.clear(); // in case anyone is holding onto this collection ...
                values = copy;
            }
            return values; // may be null
        }

        protected class WrappedEntrySet extends AbstractSet<Map.Entry<K, Collection<V>>> {

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new WrappedMapEntryIterator();
            }

            @Override
            public int size() {
                return delegate().size();
            }

            @Override
            public boolean contains( Object o ) {
                // Faster if we do this directly against the delegate ...
                return delegate().entrySet().contains(o);
            }

            @Override
            public boolean remove( Object o ) {
                if (!contains(o)) return false;
                // Faster if we do this directly against the delegate ...
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)o;
                removeAllValuesForKey(entry.getKey());
                return true;
            }
        }

        protected class WrappedMapEntryIterator implements Iterator<Map.Entry<K, Collection<V>>> {
            private final Iterator<Map.Entry<K, Collection<V>>> delegateIterator = delegate().entrySet().iterator();
            private Collection<V> currentValues = null;

            @Override
            public boolean hasNext() {
                return delegateIterator.hasNext();
            }

            @Override
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> currentEntry = delegateIterator.next();
                currentValues = currentEntry.getValue();
                K key = currentEntry.getKey();
                return new ImmutableMapEntry<>(key, wrapCollection(key, currentValues));
            }

            @Override
            public void remove() {
                // Remove the current value collection ...
                delegateIterator.remove();
                // Update the total size of the multimap ...
                decrementSize(currentValues.size());
                // and clear the values ...
                currentValues.clear();
            }
        }
    }

    protected class WrappedKeySet extends AbstractSet<K> {

        private final Map<K, Collection<V>> delegate;

        protected WrappedKeySet( Map<K, Collection<V>> wrapped ) {
            this.delegate = wrapped;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean remove( Object o ) {
            return removeAllValuesForKey(o);
        }

        @Override
        public boolean contains( Object o ) {
            return this.delegate.containsKey(o);
        }

        @Override
        public boolean containsAll( Collection<?> c ) {
            return this.delegate.keySet().containsAll(c);
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void clear() {
            this.delegate.clear();
            totalSize = 0;
        }

        @Override
        public Iterator<K> iterator() {
            final Map<K, Collection<V>> delegate = this.delegate;
            return new Iterator<K>() {
                private final Iterator<Map.Entry<K, Collection<V>>> entryIter = delegate.entrySet().iterator();
                private Map.Entry<K, Collection<V>> currentEntry;

                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public K next() {
                    currentEntry = entryIter.next();
                    return currentEntry.getKey();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                    Collection<V> values = currentEntry.getValue();
                    decrementSize(values.size());
                    values.clear();
                }
            };
        }
    }

    protected class WrappedSortedKeySet extends WrappedKeySet implements SortedSet<K> {

        private SortedMap<K, Collection<V>> sortedDelegate;

        protected WrappedSortedKeySet( SortedMap<K, Collection<V>> wrapped ) {
            super(wrapped);
            sortedDelegate = wrapped;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedDelegate.comparator();
        }

        @Override
        public SortedSet<K> subSet( K fromElement,
                                    K toElement ) {
            return new WrappedSortedKeySet(sortedDelegate.subMap(fromElement, toElement));
        }

        @Override
        public SortedSet<K> headSet( K toElement ) {
            return new WrappedSortedKeySet(sortedDelegate.headMap(toElement));
        }

        @Override
        public SortedSet<K> tailSet( K fromElement ) {
            return new WrappedSortedKeySet(sortedDelegate.tailMap(fromElement));
        }

        @Override
        public K first() {
            return sortedDelegate.firstKey();
        }

        @Override
        public K last() {
            return sortedDelegate.lastKey();
        }
    }

}
