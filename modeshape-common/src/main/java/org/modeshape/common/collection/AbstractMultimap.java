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

    protected AbstractMultimap( Map<K, Collection<V>> entries ) {
        assert entries != null;
        this.data = entries;
    }

    protected Map<K, Collection<V>> rawData() {
        return data;
    }

    protected abstract Collection<V> createCollection();

    protected abstract Collection<V> createUnmodifiableEmptyCollection();

    @SuppressWarnings( {"unchecked", "rawtypes"} )
    protected Collection<V> createUnmodifiable( Collection<V> original ) {
        if (original instanceof List) {
            return Collections.unmodifiableList((List)original);
        } else if (original instanceof Set) {
            return Collections.unmodifiableSet((Set)original);
        }
        return Collections.unmodifiableCollection(original);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#size()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return totalSize == 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey( K key ) {
        return data.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue( Object value ) {
        for (Collection<V> collection : data.values()) {
            if (collection.contains(value)) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#containsEntry(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean containsEntry( Object key,
                                  Object value ) {
        Collection<V> values = data.get(key);
        return values == null ? false : values.contains(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#get(java.lang.Object)
     */
    @Override
    public java.util.Collection<V> get( K key ) {
        Collection<V> collection = data.get(key);
        if (collection == null) {
            collection = createCollection();
        }
        return wrapCollection(key, collection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#put(java.lang.Object, java.lang.Object)
     */
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
        Collection<V> values = data.get(key);
        if (values == null) {
            values = createCollection();
            data.put(key, values);
        }
        return values;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#remove(java.lang.Object, java.lang.Object)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#removeAll(java.lang.Object)
     */
    @Override
    public java.util.Collection<V> removeAll( K key ) {
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return data.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return data.equals(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return data.toString();
    }

    private transient Set<K> keysView;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#keySet()
     */
    @Override
    public Set<K> keySet() {
        if (keysView == null) keysView = wrapKeySet();
        return keysView;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#clear()
     */
    @Override
    public void clear() {
        totalSize = 0;
        data.clear();
    }

    private transient Map<K, Collection<V>> mapView;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#asMap()
     */
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
        // if (values instanceof Set) {
        // return new WrappedSet(key, (Set<V>)values);
        // } else
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

    private transient Collection<V> valuesCollection;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#values()
     */
    @Override
    public Collection<V> values() {
        if (valuesCollection == null) valuesCollection = new ValuesCollection();
        return valuesCollection;
    }

    protected final class ValuesCollection extends AbstractCollection<V> {
        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return AbstractMultimap.this.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#iterator()
         */
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#clear()
         */
        @Override
        public void clear() {
            AbstractMultimap.this.clear();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#contains(java.lang.Object)
         */
        @Override
        public boolean contains( Object o ) {
            return AbstractMultimap.this.containsValue(o);
        }
    }

    protected final class ValueIterator implements Iterator<V> {
        private final Iterator<Map.Entry<K, V>> entryIterator = createEntryIterator();

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public V next() {
            return entryIterator.next().getValue();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            entryIterator.remove();
        }
    }

    private transient Collection<Map.Entry<K, V>> entriesCollection;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Multimap#entries()
     */
    @Override
    public Collection<Entry<K, V>> entries() {
        if (entriesCollection == null) entriesCollection = new EntriesCollection();
        return entriesCollection;
    }

    protected final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K, Collection<V>>> iter;
        K currentKey;
        Collection<V> currentValues;
        Iterator<V> currentValuesIterator;

        EntryIterator() {
            iter = rawData().entrySet().iterator();
            if (iter.hasNext()) {
                nextKey();
            } else {
                currentValuesIterator = new EmptyIterator<V>();
            }
        }

        protected void nextKey() {
            Map.Entry<K, Collection<V>> entry = iter.next();
            currentKey = entry.getKey();
            currentValues = entry.getValue();
            currentValuesIterator = currentValues.iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return iter.hasNext() || currentValuesIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public Entry<K, V> next() {
            if (!currentValuesIterator.hasNext()) {
                nextKey();
            }
            return new ImmutableMapEntry<K, V>(currentKey, currentValuesIterator.next());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
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
        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return AbstractMultimap.this.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#iterator()
         */
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#clear()
         */
        @Override
        public void clear() {
            AbstractMultimap.this.clear();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#contains(java.lang.Object)
         */
        @Override
        public boolean contains( Object o ) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> that = (Map.Entry<?, ?>)o;
            return AbstractMultimap.this.containsEntry(that.getKey(), that.getValue());
        }
    }

    protected class WrappedCollection implements Collection<V> {
        private Collection<V> delegate;
        private final K key;

        protected WrappedCollection( K key,
                                     Collection<V> values ) {
            this.key = key;
            this.delegate = values;
        }

        /**
         * @return key
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#size()
         */
        @Override
        public int size() {
            return delegate().size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#isEmpty()
         */
        @Override
        public boolean isEmpty() {
            return delegate().isEmpty();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#contains(java.lang.Object)
         */
        @Override
        public boolean contains( Object o ) {
            return delegate().contains(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#iterator()
         */
        @Override
        public Iterator<V> iterator() {
            return delegate().iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#toArray()
         */
        @Override
        public Object[] toArray() {
            // The size of the values (and thus the multimap) cannot be changed via the resulting array, so no need to wrap ...
            return delegate().toArray();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#toArray(T[])
         */
        @Override
        public <T> T[] toArray( T[] a ) {
            // The size of the values (and thus the multimap) cannot be changed via the resulting array, so no need to wrap ...
            return delegate().toArray(a);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#add(java.lang.Object)
         */
        @Override
        public boolean add( V e ) {
            Collection<V> values = delegate();
            boolean wasEmpty = values.isEmpty();
            if (!values.add(e)) return false;
            incrementSize(1);
            if (wasEmpty) addToMap();
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#remove(java.lang.Object)
         */
        @Override
        public boolean remove( Object o ) {
            if (!delegate().remove(o)) return false;
            decrementSize(1);
            removeIfEmpty();
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#containsAll(java.util.Collection)
         */
        @Override
        public boolean containsAll( Collection<?> c ) {
            return delegate().containsAll(c);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#addAll(java.util.Collection)
         */
        @Override
        public boolean addAll( Collection<? extends V> c ) {
            if (c.isEmpty()) return false;
            Collection<V> delegate = delegate();
            int sizeBefore = delegate.size();
            if (!delegate.addAll(c)) return false;
            incrementSize(delegate.size() - sizeBefore);
            if (sizeBefore == 0) addToMap();
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#removeAll(java.util.Collection)
         */
        @Override
        public boolean removeAll( Collection<?> c ) {
            if (c.isEmpty()) return false;
            Collection<V> delegate = delegate();
            int sizeBefore = delegate.size();
            if (!delegate.removeAll(c)) return false;
            incrementSize(delegate.size() - sizeBefore); // will be negative
            removeIfEmpty();
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#retainAll(java.util.Collection)
         */
        @Override
        public boolean retainAll( Collection<?> c ) {
            if (c.isEmpty()) return false;
            Collection<V> delegate = delegate();
            int sizeBefore = delegate.size();
            if (!delegate.retainAll(c)) return false;
            int diff = delegate.size() - sizeBefore;
            incrementSize(diff); // will change correctly if diff is negative
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Collection#clear()
         */
        @Override
        public void clear() {
            Collection<V> delegate = delegate();
            int sizeBefore = delegate.size();
            delegate.clear();
            decrementSize(sizeBefore);
            removeIfEmpty();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return delegate().hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            return delegate().equals(obj);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return delegate().toString();
        }

        protected class DelegateIterator implements Iterator<V> {
            private final Collection<V> source;
            private final Iterator<V> iterator;

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

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#hasNext()
             */
            @Override
            public boolean hasNext() {
                return iterator().hasNext();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#next()
             */
            @Override
            public V next() {
                return iterator.next();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#remove()
             */
            @Override
            public void remove() {
                iterator.remove();
                decrementSize(1);
                removeIfEmpty();
            }
        }
    }

    protected class WrappedList extends WrappedCollection implements List<V> {

        protected WrappedList( K key,
                               Collection<V> values ) {
            super(key, values);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.collection.AbstractMultimap.WrappedCollection#delegate()
         */
        @Override
        protected List<V> delegate() {
            return (List<V>)super.delegate();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#addAll(int, java.util.Collection)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#get(int)
         */
        @Override
        public V get( int index ) {
            return delegate().get(index);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#set(int, java.lang.Object)
         */
        @Override
        public V set( int index,
                      V element ) {
            return delegate().set(index, element);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#add(int, java.lang.Object)
         */
        @Override
        public void add( int index,
                         V element ) {
            List<V> values = delegate();
            boolean wasEmpty = values.isEmpty();
            values.add(index, element);
            incrementSize(1);
            if (wasEmpty) addToMap();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#remove(int)
         */
        @Override
        public V remove( int index ) {
            V removed = delegate().remove(index);
            decrementSize(1);
            removeIfEmpty();
            return removed;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#indexOf(java.lang.Object)
         */
        @Override
        public int indexOf( Object o ) {
            return delegate().indexOf(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#lastIndexOf(java.lang.Object)
         */
        @Override
        public int lastIndexOf( Object o ) {
            return delegate().lastIndexOf(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#listIterator()
         */
        @Override
        public ListIterator<V> listIterator() {
            return new DelegateListIterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#listIterator(int)
         */
        @Override
        public ListIterator<V> listIterator( int index ) {
            return new DelegateListIterator(index);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#subList(int, int)
         */
        @Override
        public List<V> subList( int fromIndex,
                                int toIndex ) {
            List<V> valueSublist = delegate().subList(fromIndex, toIndex);
            return wrapList(getKey(), valueSublist);
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

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#hasPrevious()
             */
            @Override
            public boolean hasPrevious() {
                return iterator().hasPrevious();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#add(java.lang.Object)
             */
            @Override
            public void add( V e ) {
                iterator().add(e);
                incrementSize(1);

            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#nextIndex()
             */
            @Override
            public int nextIndex() {
                return iterator().nextIndex();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#previous()
             */
            @Override
            public V previous() {
                return iterator().previous();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#previousIndex()
             */
            @Override
            public int previousIndex() {
                return iterator().previousIndex();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.ListIterator#set(java.lang.Object)
             */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#entrySet()
         */
        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            if (entries == null) entries = new WrappedEntrySet();
            return entries;
        }

        protected Map<K, Collection<V>> delegate() {
            return delegate;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#hashCode()
         */
        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object o ) {
            if (o == this) return true;
            return delegate.equals(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#toString()
         */
        @Override
        public String toString() {
            return delegate.toString();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#get(java.lang.Object)
         */
        @SuppressWarnings( "unchecked" )
        @Override
        public Collection<V> get( Object key ) {
            return AbstractMultimap.this.get((K)key);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractMap#remove(java.lang.Object)
         */
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

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#iterator()
             */
            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new WrappedMapEntryIterator();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#size()
             */
            @Override
            public int size() {
                return delegate().size();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#contains(java.lang.Object)
             */
            @Override
            public boolean contains( Object o ) {
                // Faster if we do this directly against the delegate ...
                return delegate().entrySet().contains(o);
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.AbstractCollection#remove(java.lang.Object)
             */
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

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#hasNext()
             */
            @Override
            public boolean hasNext() {
                return delegateIterator.hasNext();
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#next()
             */
            @Override
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> currentEntry = delegateIterator.next();
                currentValues = currentEntry.getValue();
                K key = currentEntry.getKey();
                return new ImmutableMapEntry<K, Collection<V>>(key, wrapCollection(key, currentValues));
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.util.Iterator#remove()
             */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return this.delegate.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#remove(java.lang.Object)
         */
        @Override
        public boolean remove( Object o ) {
            return removeAllValuesForKey(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#contains(java.lang.Object)
         */
        @Override
        public boolean contains( Object o ) {
            return this.delegate.containsKey(o);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#containsAll(java.util.Collection)
         */
        @Override
        public boolean containsAll( Collection<?> c ) {
            return this.delegate.keySet().containsAll(c);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#clear()
         */
        @SuppressWarnings( "synthetic-access" )
        @Override
        public void clear() {
            this.delegate.clear();
            totalSize = 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.AbstractCollection#iterator()
         */
        @Override
        public Iterator<K> iterator() {
            final Map<K, Collection<V>> delegate = this.delegate;
            return new Iterator<K>() {
                private final Iterator<Map.Entry<K, Collection<V>>> entryIter = delegate.entrySet().iterator();
                private Map.Entry<K, Collection<V>> currentEntry;

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#hasNext()
                 */
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#next()
                 */
                @Override
                public K next() {
                    currentEntry = entryIter.next();
                    return currentEntry.getKey();
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.Iterator#remove()
                 */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#comparator()
         */
        @Override
        public Comparator<? super K> comparator() {
            return sortedDelegate.comparator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
         */
        @Override
        public SortedSet<K> subSet( K fromElement,
                                    K toElement ) {
            return new WrappedSortedKeySet(sortedDelegate.subMap(fromElement, toElement));
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#headSet(java.lang.Object)
         */
        @Override
        public SortedSet<K> headSet( K toElement ) {
            return new WrappedSortedKeySet(sortedDelegate.headMap(toElement));
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#tailSet(java.lang.Object)
         */
        @Override
        public SortedSet<K> tailSet( K fromElement ) {
            return new WrappedSortedKeySet(sortedDelegate.tailMap(fromElement));
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#first()
         */
        @Override
        public K first() {
            return sortedDelegate.firstKey();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.SortedSet#last()
         */
        @Override
        public K last() {
            return sortedDelegate.lastKey();
        }
    }

}
