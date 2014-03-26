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
package org.modeshape.jcr.query;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DB.BTreeMapMaker;
import org.mapdb.DB.HTreeSetMaker;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.modeshape.common.collection.SingleIterator;
import org.modeshape.common.collection.Supplier;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.Tuples.TupleFactory;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.UriFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A manager of temporary buffers used in the query system.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class BufferManager implements AutoCloseable {

    /**
     * A basic buffer interface.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface Buffer extends AutoCloseable {

        /**
         * Determine if this buffer is empty. This may be more efficient than {@link #size()}.
         * 
         * @return true if the buffer is empty, or false otherwise
         */
        boolean isEmpty();

        /**
         * Get the size of the buffer. This might be more expensive if the buffer is not created with the flag to keep the size.
         * 
         * @return the size of the buffer.
         */
        long size();

        /**
         * Close the buffer and release all resources. The buffer is not usable after this.
         */
        @Override
        void close();
    }

    /**
     * A buffer that maintains the insertion order of a series of values.
     * 
     * @param <T> the type of value
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface QueueBuffer<T> extends Buffer, Iterable<T> {

        /**
         * Add a value to the end of this buffer.
         * 
         * @param value the value to be added; may be null
         */
        void append( T value );

        /**
         * Get an iterator over all of the values.
         * 
         * @return the iterator; never null
         */
        @Override
        Iterator<T> iterator();
    }

    public static interface Predicate<T> {
        /**
         * Determine whether the buffer has seen this value.
         * 
         * @param value the value
         * @return true if the buffer already has seen that value, or false otherwise
         */
        boolean contains( T value );
    }

    /**
     * A buffer used to determine distinct values.
     * 
     * @param <T> the type of the distinct value
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface DistinctBuffer<T> extends Buffer, Iterable<T>, Predicate<T> {

        /**
         * Get an iterator over all of the records.
         * 
         * @return the iterator; never null
         */
        @Override
        Iterator<T> iterator();
    }

    /**
     * A buffer used to sort values into ascending or descending order.
     * 
     * @param <SortType> the type of the sorting value
     * @param <RecordType> the type of record that is to be sorted
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface SortingBuffer<SortType, RecordType> extends Buffer {
        /**
         * Put the supplied value into the buffer given its sortable value.
         * 
         * @param sortable the value of the record that is to be used for sorting
         * @param record the record
         */
        void put( SortType sortable,
                  RecordType record );

        /**
         * Get an iterator over all of the records in ascending order.
         * 
         * @return the ascending iterator
         */
        Iterator<RecordType> ascending();

        /**
         * Get an iterator over all of the records in descending order.
         * 
         * @return the ascending iterator
         */
        Iterator<RecordType> descending();

        /**
         * Get an iterator over all of the records that have the given sortable value.
         * 
         * @param key the sortable value
         * @return the iterator; null if there is no entry, or an iterator with one or more values
         */
        Iterator<RecordType> getAll( SortType key );

        /**
         * Get an iterator over all of the records that have a sortable value with in the specified range.
         * 
         * @param lowerKey the minimum sortable value; may be null if there is no minimum
         * @param includeLowerKey true if the values equal to or greater than {@code lowerKey} value is to be included, or false
         *        if only values greater than {@code lowerKey} should be included
         * @param upperKey the maximum sortable value; may be null if there is no maximum
         * @param includeUpperKey true if the values equal to or less than {@code upperKey} value are to be included, or false if
         *        only values less than {@code upperKey} should be included
         * @return the iterator; null if there is no entry, or an iterator with one or more values
         */
        Iterator<RecordType> getAll( SortType lowerKey,
                                     boolean includeLowerKey,
                                     SortType upperKey,
                                     boolean includeUpperKey );
    }

    /**
     * An object use to create a new {@link DistinctBuffer}.
     * 
     * @see BufferManager#createDistinctBuffer(Serializer)
     * @param <T> the type of value to be used in the buffer
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface QueueBufferMaker<T> {
        /**
         * Specify whether to store the bufer on the heap.
         * 
         * @param useHeap true if the buffer's contents are to be stored on the heap, or false if off-heap storage should be used.
         * @return this maker instance; never null
         */
        QueueBufferMaker<T> useHeap( boolean useHeap );

        /**
         * Create the {@link DistinctBuffer} instance.
         * 
         * @return the distinct buffer; never null
         */
        QueueBuffer<T> make();
    }

    /**
     * An object use to create a new {@link DistinctBuffer}.
     * 
     * @see BufferManager#createDistinctBuffer(Serializer)
     * @param <T> the type of value to be used in the buffer
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface DistinctBufferMaker<T> {
        /**
         * Specify whether to store the bufer on the heap.
         * 
         * @param useHeap true if the buffer's contents are to be stored on the heap, or false if off-heap storage should be used.
         * @return this maker instance; never null
         */
        DistinctBufferMaker<T> useHeap( boolean useHeap );

        /**
         * Specify whether to keep track of the buffer size when adding value. Doing so may slow adds, but it will make returning
         * the size very quick. Adds may be faster if this is set to false, but asking the buffer for its size requires iterating
         * over the entire buffer and thus may be slower.
         * 
         * @param keepBufferSize true if the buffer is to efficiently track its size, or false if it can skip this and, only if
         *        {@link Buffer#size()} is called, compute the size in a brute force manner.
         * @return this maker instance; never null
         */
        DistinctBufferMaker<T> keepSize( boolean keepBufferSize );

        /**
         * Create the {@link DistinctBuffer} instance.
         * 
         * @return the distinct buffer; never null
         */
        DistinctBuffer<T> make();
    }

    /**
     * An object use to create a new {@link SortingBuffer}.
     * 
     * @see BufferManager#createSortingBuffer(BTreeKeySerializer, Serializer)
     * @param <SortType> the type of sortable value
     * @param <RecordType> the type of record to be placed into the buffer
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface SortingBufferMaker<SortType, RecordType> {
        /**
         * Specify whether to store the bufer on the heap.
         * 
         * @param useHeap true if the buffer's contents are to be stored on the heap, or false if off-heap storage should be used.
         * @return this maker instance; never null
         */
        SortingBufferMaker<SortType, RecordType> useHeap( boolean useHeap );

        /**
         * Specify whether to keep track of the buffer size when adding value. Doing so may slow adds, but it will make returning
         * the size very quick. Adds may be faster if this is set to false, but asking the buffer for its size requires iterating
         * over the entire buffer and thus may be slower.
         * 
         * @param keepBufferSize true if the buffer is to efficiently track its size, or false if it can skip this and, only if
         *        {@link Buffer#size()} is called, compute the size in a brute force manner.
         * @return this maker instance; never null
         */
        SortingBufferMaker<SortType, RecordType> keepSize( boolean keepBufferSize );

        /**
         * Create the {@link SortingBuffer} instance.
         * 
         * @return the distinct buffer; never null
         */
        SortingBuffer<SortType, RecordType> make();
    }

    protected static final class DbHolder implements AutoCloseable {
        private final AtomicReference<DB> reference = new AtomicReference<>();
        private final Lock lock = new ReentrantLock();
        private final Supplier<DB> supplier;

        protected DbHolder( Supplier<DB> supplier ) {
            this.supplier = supplier;
        }

        public DB get() {
            DB db = reference.get();
            if (db == null) {
                try {
                    lock.lock();
                    // Allocate it ...
                    db = supplier.get();
                    reference.set(db);
                } finally {
                    lock.unlock();
                }
            }
            return db;
        }

        @Override
        public void close() {
            DB db = reference.getAndSet(null);
            if (db != null) {
                db.close();
            }
        }
    }

    private final static Supplier<DB> OFF_HEAP_DB_SUPPLIER = new Supplier<DB>() {
        @Override
        public DB get() {
            return DBMaker.newMemoryDirectDB().make();
        }
    };

    private final static Supplier<DB> ON_HEAP_DB_SUPPLIER = new Supplier<DB>() {
        @Override
        public DB get() {
            return DBMaker.newMemoryDB().make();
        }
    };

    private final static Serializer<?> DEFAULT_SERIALIZER = Serializer.BASIC;
    private final static BTreeKeySerializer<?> DEFAULT_BTREE_KEY_SERIALIZER = BTreeKeySerializer.BASIC;

    private final Map<Class<?>, Serializer<?>> serializersByClass;
    private final Map<Class<?>, BTreeKeySerializer<?>> bTreeKeySerializersByClass;
    private final Map<Class<?>, BTreeKeySerializer<?>> packedBTreeKeySerializersByClass;
    private final DbHolder offheap;
    private final DbHolder onheap;
    private final AtomicLong dbCounter = new AtomicLong();

    public BufferManager( ExecutionContext context ) {
        this(context, OFF_HEAP_DB_SUPPLIER, ON_HEAP_DB_SUPPLIER);
    }

    protected BufferManager( ExecutionContext context,
                             Supplier<DB> offheapDbSupplier,
                             Supplier<DB> onheapDbSupplier ) {
        offheap = new DbHolder(offheapDbSupplier);
        onheap = new DbHolder(onheapDbSupplier);

        // Create the serializers ...
        ValueFactories factories = context.getValueFactories();
        final PathFactory pathFactory = factories.getPathFactory();
        final NameFactory nameFactory = factories.getNameFactory();
        final StringFactory stringFactory = factories.getStringFactory();
        final ReferenceFactory refFactory = factories.getReferenceFactory();
        final UriFactory uriFactory = factories.getUriFactory();
        final DateTimeFactory dateFactory = factories.getDateFactory();
        final ValueFactory<BigDecimal> decimalFactory = factories.getDecimalFactory();
        serializersByClass = new HashMap<Class<?>, Serializer<?>>();
        serializersByClass.put(String.class, Serializer.STRING);
        serializersByClass.put(Long.class, Serializer.LONG);
        serializersByClass.put(Boolean.class, Serializer.BOOLEAN);
        serializersByClass.put(Double.class, new DoubleSerializer());
        serializersByClass.put(BigDecimal.class, new ValueSerializer<BigDecimal>(stringFactory, decimalFactory));
        serializersByClass.put(URI.class, new ValueSerializer<URI>(stringFactory, uriFactory));
        serializersByClass.put(DateTime.class, new ValueSerializer<DateTime>(stringFactory, dateFactory));
        serializersByClass.put(Path.class, new ValueSerializer<Path>(stringFactory, pathFactory));
        serializersByClass.put(Name.class, new ValueSerializer<Name>(stringFactory, nameFactory));
        serializersByClass.put(Reference.class, new ValueSerializer<Reference>(stringFactory, refFactory));
        serializersByClass.put(NodeKey.class, new Serializer<NodeKey>() {
            @Override
            public void serialize( DataOutput out,
                                   NodeKey value ) throws IOException {
                out.writeUTF(value.toString());
            }

            @Override
            public NodeKey deserialize( DataInput in,
                                        int available ) throws IOException {
                String keyStr = in.readUTF();
                return new NodeKey(keyStr);
            }

            @Override
            public int fixedSize() {
                return -1; // not fixed size
            }
        });

        bTreeKeySerializersByClass = new HashMap<Class<?>, BTreeKeySerializer<?>>();
        packedBTreeKeySerializersByClass = new HashMap<Class<?>, BTreeKeySerializer<?>>();
        for (Map.Entry<Class<?>, Serializer<?>> entry : serializersByClass.entrySet()) {
            Serializer<?> serializer = entry.getValue();
            @SuppressWarnings( {"rawtypes", "unchecked"} )
            BTreeKeySerializer<?> bTreeSerializer = new DelegatingKeySerializer(serializer);
            bTreeKeySerializersByClass.put(entry.getKey(), bTreeSerializer);
            packedBTreeKeySerializersByClass.put(entry.getKey(), bTreeSerializer);
        }

        // Override some of the types for string-based keys ...
        packedBTreeKeySerializersByClass.put(String.class, new PackedStringKeySerializer<String>(stringFactory, stringFactory));
        packedBTreeKeySerializersByClass.put(Name.class, new PackedStringKeySerializer<Name>(stringFactory, nameFactory));
        packedBTreeKeySerializersByClass.put(Path.class, new PackedStringKeySerializer<Path>(stringFactory, pathFactory));
        packedBTreeKeySerializersByClass.put(Name.class, new PackedStringKeySerializer<Name>(stringFactory, nameFactory));
        packedBTreeKeySerializersByClass.put(DateTime.class, new PackedStringKeySerializer<DateTime>(stringFactory, dateFactory));
        packedBTreeKeySerializersByClass.put(URI.class, new PackedStringKeySerializer<URI>(stringFactory, uriFactory));
        packedBTreeKeySerializersByClass.put(Reference.class, new PackedStringKeySerializer<Reference>(stringFactory, refFactory));
        packedBTreeKeySerializersByClass.put(BigDecimal.class, new PackedStringKeySerializer<BigDecimal>(stringFactory,
                                                                                                         decimalFactory));
    }

    @Override
    public void close() {
        RuntimeException error = null;
        try {
            onheap.close();
        } catch (RuntimeException e) {
            error = e;
        } finally {
            try {
                offheap.close();
            } catch (RuntimeException e) {
                if (error == null) error = e;
            }
            if (error != null) throw error;
        }
    }

    /**
     * Obtain a maker object that can create a new {@link QueueBuffer}.
     * 
     * @param serializer the serializer for the value
     * @return the maker; never null
     */
    public <T> QueueBufferMaker<T> createQueueBuffer( Serializer<T> serializer ) {
        return new MakeOrderedBuffer<T>("buffer-" + dbCounter.incrementAndGet(), serializer);
    }

    /**
     * Obtain a maker object that can create a new {@link DistinctBuffer}.
     * 
     * @param distinctSerializer the serializer for the distinct value
     * @return the maker; never null
     */
    public <T> DistinctBufferMaker<T> createDistinctBuffer( Serializer<T> distinctSerializer ) {
        return new MakeDistinctBuffer<T>("buffer-" + dbCounter.incrementAndGet(), distinctSerializer);
    }

    /**
     * Obtain a maker object that can create a new {@link SortingBuffer} that will keep a single values for any given key.
     * 
     * @param keySerializer the serializer for the keys
     * @param valueSerializer the serializer for the values
     * @return the maker; never null
     */
    public <K, V> SortingBufferMaker<K, V> createSortingBuffer( BTreeKeySerializer<K> keySerializer,
                                                                Serializer<V> valueSerializer ) {
        return new MakeSortingBuffer<K, V>("buffer-" + dbCounter.incrementAndGet(), keySerializer, valueSerializer);
    }

    /**
     * Obtain a maker object that can create a new {@link SortingBuffer} that can store multiple values for any given key.
     * 
     * @param keySerializer the serializer for the keys
     * @param keyComparator the comparator for the keys, or null if natural ordering should be used
     * @param valueSerializer the serializer for the values
     * @return the maker; never null
     */
    public <K extends Comparable<K>, V> SortingBufferMaker<K, V> createSortingWithDuplicatesBuffer( Serializer<K> keySerializer,
                                                                                                    Comparator<?> keyComparator,
                                                                                                    Serializer<V> valueSerializer ) {
        return new MakeSortingWithDuplicatesBuffer<K, V>("buffer-" + dbCounter.incrementAndGet(), keySerializer, keyComparator,
                                                         valueSerializer);
    }

    /**
     * Obtain a serializer for the given value type.
     * 
     * @param type the type; may not be null
     * @return the serializer
     */
    public Serializer<?> serializerFor( TypeFactory<?> type ) {
        Serializer<?> result = serializersByClass.get(type.getType());
        if (result != null) return result;
        if (type instanceof TupleFactory) {
            return ((TupleFactory<?>)type).getSerializer(this);
        }
        return DEFAULT_SERIALIZER;
    }

    /**
     * Obtain a serializer for the given key type.
     * 
     * @param type the type; may not be null
     * @param pack true if the serializer can/should pack keys together when possible, or false otherwise
     * @return the serializer
     */
    public BTreeKeySerializer<?> bTreeKeySerializerFor( TypeFactory<?> type,
                                                        boolean pack ) {
        Map<Class<?>, BTreeKeySerializer<?>> byClass = pack ? packedBTreeKeySerializersByClass : bTreeKeySerializersByClass;
        BTreeKeySerializer<?> result = byClass.get(type.getType());
        if (result == null) result = DEFAULT_BTREE_KEY_SERIALIZER;
        // Make sure the serializer uses the type's comparator ...
        KeySerializerWithComparator<?> serializer = (KeySerializerWithComparator<?>)result;
        Comparator<?> comparator = type.getComparator();
        return serializer.withComparator(comparator);
    }

    protected final DB db( boolean useHeap ) {
        return useHeap ? onheap.get() : offheap.get();
    }

    protected final void delete( String name,
                                 boolean onHeap ) {
        db(onHeap).delete(name);
    }

    protected abstract class CloseableBuffer implements Buffer {
        protected final String name;
        protected final boolean onHeap;

        protected CloseableBuffer( String name,
                                   boolean onHeap ) {
            this.name = name;
            this.onHeap = onHeap;
        }

        @Override
        public void close() {
            BufferManager.this.delete(name, onHeap);
        }
    }

    protected final class CloseableQueueBuffer<T> extends CloseableBuffer implements QueueBuffer<T> {
        protected final Map<Long, T> buffer;
        private final AtomicLong size = new AtomicLong();

        protected CloseableQueueBuffer( String name,
                                        boolean onHeap,
                                        Map<Long, T> buffer ) {
            super(name, onHeap);
            this.buffer = buffer;
        }

        @Override
        public boolean isEmpty() {
            return buffer.isEmpty();
        }

        @Override
        public long size() {
            return size.get();
        }

        @Override
        public void append( T value ) {
            buffer.put(size.getAndIncrement(), value);
        }

        @Override
        public Iterator<T> iterator() {
            final AtomicLong counter = new AtomicLong(0L);
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return counter.get() < buffer.size();
                }

                @Override
                public T next() {
                    Long key = counter.getAndIncrement();
                    if (key.intValue() < buffer.size()) {
                        return buffer.get(key);
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String toString() {
            return "QueueBuffer(" + name + ",size=" + size.get() + ")";
        }
    }

    protected final class CloseableDistinctBuffer<T> extends CloseableBuffer implements DistinctBuffer<T> {
        private final Set<T> buffer;

        protected CloseableDistinctBuffer( String name,
                                           boolean onHeap,
                                           Set<T> buffer ) {
            super(name, onHeap);
            this.buffer = buffer;
        }

        @Override
        public boolean isEmpty() {
            return buffer.isEmpty();
        }

        @Override
        public long size() {
            return buffer.size();
        }

        @Override
        public boolean contains( T value ) {
            return !buffer.add(value);
        }

        @Override
        public Iterator<T> iterator() {
            return buffer.iterator();
        }

        @Override
        public String toString() {
            return "DistinctBuffer(" + name + ")";
        }
    }

    protected final class CloseableSortingBuffer<K, V> extends CloseableBuffer implements SortingBuffer<K, V> {
        private final NavigableMap<K, V> buffer;

        protected CloseableSortingBuffer( String name,
                                          boolean onHeap,
                                          NavigableMap<K, V> buffer ) {
            super(name, onHeap);
            this.buffer = buffer;
        }

        @Override
        public boolean isEmpty() {
            return buffer.isEmpty();
        }

        @Override
        public long size() {
            return buffer.size();
        }

        @Override
        public void put( K sortable,
                         V record ) {
            buffer.put(sortable, record);
        }

        @Override
        public Iterator<V> getAll( K key ) {
            V value = buffer.get(key);
            return value == null ? null : new SingleIterator<V>(value);
        }

        @Override
        public Iterator<V> getAll( K lowerKey,
                                   boolean includeLowerKey,
                                   K upperKey,
                                   boolean includeUpperKey ) {
            if (lowerKey == null) {
                if (upperKey == null) {
                    // It is unbounded ...
                    return buffer.values().iterator();
                }
                return buffer.headMap(upperKey, includeUpperKey).values().iterator();
            }
            assert lowerKey != null;
            if (upperKey == null) {
                return buffer.tailMap(lowerKey, includeLowerKey).values().iterator();
            }
            return buffer.subMap(lowerKey, includeLowerKey, upperKey, includeUpperKey).values().iterator();
        }

        @Override
        public Iterator<V> ascending() {
            final Iterator<Map.Entry<K, V>> entryIter = buffer.entrySet().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public V next() {
                    return entryIter.next().getValue();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                }
            };
        }

        @Override
        public Iterator<V> descending() {
            final Iterator<Map.Entry<K, V>> entryIter = buffer.descendingMap().entrySet().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public V next() {
                    return entryIter.next().getValue();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                }
            };
        }

        @Override
        public String toString() {
            return "SortingBuffer(" + name + ")";
        }
    }

    protected static final class UniqueKey<K> {
        protected final K actualKey;
        protected final long id;
        private final int hc;

        protected UniqueKey( K actualKey,
                             long id ) {
            this.actualKey = actualKey;
            this.id = id;
            this.hc = actualKey != null ? actualKey.hashCode() : 0;
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (this == obj) return true;
            if (obj instanceof UniqueKey) {
                @SuppressWarnings( "unchecked" )
                UniqueKey<K> that = (UniqueKey<K>)obj;
                if (this.actualKey == null && that.actualKey != null) return false;
                if (!this.actualKey.equals(that.actualKey)) return false;
                return this.id == that.id;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + actualKey + "," + id + "]";
        }
    }

    protected final class CloseableSortingBufferWithDuplicates<K extends Comparable<K>, V> extends CloseableBuffer
        implements SortingBuffer<K, V> {
        private final NavigableMap<UniqueKey<K>, V> buffer;
        private final AtomicLong counter = new AtomicLong();

        protected CloseableSortingBufferWithDuplicates( String name,
                                                        boolean onHeap,
                                                        NavigableMap<UniqueKey<K>, V> buffer ) {
            super(name, onHeap);
            this.buffer = buffer;
        }

        @Override
        public boolean isEmpty() {
            return buffer.isEmpty();
        }

        @Override
        public long size() {
            return buffer.size();
        }

        @Override
        public void put( K sortable,
                         V record ) {
            buffer.put(new UniqueKey<K>(sortable, counter.incrementAndGet()), record);
        }

        @Override
        public Iterator<V> getAll( K key ) {
            UniqueKey<K> lowest = new UniqueKey<K>(key, 0);
            UniqueKey<K> pastHighest = new UniqueKey<K>(key, Long.MAX_VALUE);
            SortedMap<UniqueKey<K>, V> map = buffer.subMap(lowest, pastHighest);
            if (map == null || map.isEmpty()) return null;
            final Iterator<Map.Entry<UniqueKey<K>, V>> entryIter = map.entrySet().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public V next() {
                    return entryIter.next().getValue();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                }
            };
        }

        @Override
        public Iterator<V> getAll( K lowerKey,
                                   boolean includeLowerKey,
                                   K upperKey,
                                   boolean includeUpperKey ) {
            UniqueKey<K> lowest = includeLowerKey ? new UniqueKey<K>(lowerKey, 0) : new UniqueKey<K>(lowerKey, Long.MAX_VALUE);
            UniqueKey<K> highest = includeUpperKey ? new UniqueKey<K>(upperKey, Long.MAX_VALUE) : new UniqueKey<K>(upperKey, 0L);
            if (upperKey == null) {
                if (lowerKey == null) return Collections.<V>emptyList().iterator();
                return buffer.tailMap(lowest, includeLowerKey).values().iterator();
            } else if (lowerKey == null) {
                assert upperKey != null;
                return buffer.headMap(highest, includeUpperKey).values().iterator();
            }
            assert lowerKey != null;
            assert upperKey != null;
            return buffer.subMap(lowest, includeLowerKey, highest, includeUpperKey).values().iterator();
        }

        @Override
        public Iterator<V> ascending() {
            final Iterator<Map.Entry<UniqueKey<K>, V>> entryIter = buffer.entrySet().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public V next() {
                    return entryIter.next().getValue();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                }
            };
        }

        @Override
        public Iterator<V> descending() {
            final Iterator<Map.Entry<UniqueKey<K>, V>> entryIter = buffer.descendingMap().entrySet().iterator();
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return entryIter.hasNext();
                }

                @Override
                public V next() {
                    return entryIter.next().getValue();
                }

                @Override
                public void remove() {
                    entryIter.remove();
                }
            };
        }

        @Override
        public String toString() {
            return "SortingBufferWithDuplicateKeys(" + name + ")";
        }
    }

    protected final class MakeOrderedBuffer<T> implements QueueBufferMaker<T> {
        private final String name;
        private boolean useHeap = true;
        private final Serializer<T> serializer;

        protected MakeOrderedBuffer( String name,
                                     Serializer<T> serializer ) {
            assert name != null;
            assert serializer != null;
            this.name = name;
            this.serializer = serializer;
        }

        @Override
        public MakeOrderedBuffer<T> useHeap( boolean useHeap ) {
            this.useHeap = useHeap;
            return this;
        }

        @Override
        public QueueBuffer<T> make() {
            HTreeMap<Long, T> values = db(useHeap).createHashMap(name).valueSerializer(serializer).counterEnable().make();
            return new CloseableQueueBuffer<T>(name, useHeap, values);
        }
    }

    protected final class MakeDistinctBuffer<T> implements DistinctBufferMaker<T> {
        private final String name;
        private boolean useHeap = true;
        private boolean keepsize = false;
        private final Serializer<T> serializer;

        protected MakeDistinctBuffer( String name,
                                      Serializer<T> serializer ) {
            assert name != null;
            assert serializer != null;
            this.name = name;
            this.serializer = serializer;
        }

        @Override
        public DistinctBufferMaker<T> keepSize( boolean keepBufferSize ) {
            this.keepsize = keepBufferSize;
            return this;
        }

        @Override
        public DistinctBufferMaker<T> useHeap( boolean useHeap ) {
            this.useHeap = useHeap;
            return this;
        }

        @Override
        public DistinctBuffer<T> make() {
            HTreeSetMaker maker = db(useHeap).createHashSet(name).serializer(serializer);
            if (keepsize) maker = maker.counterEnable();
            Set<T> buffer = maker.make();
            return new CloseableDistinctBuffer<T>(name, useHeap, buffer);
        }
    }

    protected final class MakeSortingBuffer<K, V> implements SortingBufferMaker<K, V> {
        private final String name;
        private boolean useHeap = true;
        private boolean keepsize = false;
        private final BTreeKeySerializer<K> keySerializer;
        private final Serializer<V> valueSerializer;

        protected MakeSortingBuffer( String name,
                                     BTreeKeySerializer<K> keySerializer,
                                     Serializer<V> valueSerializer ) {
            this.name = name;
            this.keySerializer = keySerializer;
            this.valueSerializer = valueSerializer;
        }

        @Override
        public SortingBufferMaker<K, V> keepSize( boolean keepBufferSize ) {
            this.keepsize = keepBufferSize;
            return this;
        }

        @Override
        public SortingBufferMaker<K, V> useHeap( boolean useHeap ) {
            this.useHeap = useHeap;
            return this;
        }

        @Override
        public SortingBuffer<K, V> make() {
            BTreeMapMaker maker = db(useHeap).createTreeMap(name).keySerializer(keySerializer).valueSerializer(valueSerializer);
            if (keepsize) maker = maker.counterEnable();
            NavigableMap<K, V> buffer = maker.make();
            return new CloseableSortingBuffer<K, V>(name, useHeap, buffer);
        }
    }

    protected final class MakeSortingWithDuplicatesBuffer<K extends Comparable<K>, V> implements SortingBufferMaker<K, V> {
        private final String name;
        private boolean useHeap = true;
        private boolean keepsize = false;
        private final Serializer<K> keySerializer;
        private final Serializer<V> valueSerializer;
        private final Comparator<K> keyComparator;

        @SuppressWarnings( "unchecked" )
        protected MakeSortingWithDuplicatesBuffer( String name,
                                                   Serializer<K> keySerializer,
                                                   Comparator<?> keyComparator,
                                                   Serializer<V> valueSerializer ) {
            this.name = name;
            this.keySerializer = keySerializer;
            this.valueSerializer = valueSerializer;
            this.keyComparator = (Comparator<K>)keyComparator;
        }

        @Override
        public SortingBufferMaker<K, V> keepSize( boolean keepBufferSize ) {
            this.keepsize = keepBufferSize;
            return this;
        }

        @Override
        public SortingBufferMaker<K, V> useHeap( boolean useHeap ) {
            this.useHeap = useHeap;
            return this;
        }

        @Override
        public SortingBuffer<K, V> make() {
            Comparator<UniqueKey<K>> comparator = this.keyComparator != null ? new UniqueKeyComparator<K>(keyComparator) : new ComparableUniqueKeyComparator<K>();
            BTreeKeySerializer<UniqueKey<K>> uniqueKeySerializer = new UniqueKeySerializer<K>(keySerializer, comparator);
            BTreeMapMaker maker = db(useHeap).createTreeMap(name).keySerializer(uniqueKeySerializer)
                                             .valueSerializer(valueSerializer);
            if (keepsize) maker = maker.counterEnable();
            NavigableMap<UniqueKey<K>, V> buffer = maker.make();
            return new CloseableSortingBufferWithDuplicates<K, V>(name, useHeap, buffer);
        }
    }

    protected static final class UniqueKeyComparator<K> implements Comparator<UniqueKey<K>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient Comparator<K> valueComparator;

        protected UniqueKeyComparator( Comparator<K> valueComparator ) {
            this.valueComparator = valueComparator;
        }

        @Override
        public int compare( UniqueKey<K> o1,
                            UniqueKey<K> o2 ) {
            if (o1 == o2) return 0;
            int diff = valueComparator.compare(o1.actualKey, o2.actualKey);
            if (diff != 0) return diff;
            long ldiff = o1.id - o2.id;
            return ldiff == 0L ? 0 : (ldiff <= 0L ? -1 : 1);
        }
    }

    protected static final class ComparableUniqueKeyComparator<K> implements Comparator<UniqueKey<K>>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( UniqueKey<K> o1,
                            UniqueKey<K> o2 ) {
            if (o1 == o2) return 0;
            int diff = ObjectUtil.compareWithNulls((Comparable<?>)o1.actualKey, (Comparable<?>)o2.actualKey);
            if (diff != 0) return diff;
            long ldiff = o1.id - o2.id;
            return ldiff == 0L ? 0 : (ldiff <= 0L ? -1 : 1);
        }
    }

    protected static final class UniqueKeySerializer<K extends Comparable<K>> extends BTreeKeySerializer<UniqueKey<K>>
        implements Serializable {
        private static final long serialVersionUID = 1L;
        protected final transient Serializer<K> keySerializer;
        protected final transient Comparator<UniqueKey<K>> comparator;

        protected UniqueKeySerializer( Serializer<K> keySerializer,
                                       Comparator<UniqueKey<K>> comparator ) {
            this.keySerializer = keySerializer;
            this.comparator = comparator;
        }

        @Override
        public Comparator<UniqueKey<K>> getComparator() {
            return comparator;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void serialize( DataOutput out,
                               int start,
                               int end,
                               Object[] keys ) throws IOException {
            for (int i = start; i < end; i++) {
                UniqueKey<K> key = (UniqueKey<K>)keys[i];
                keySerializer.serialize(out, key.actualKey);
                out.writeLong(key.id);
            }
        }

        @Override
        public Object[] deserialize( DataInput in,
                                     int start,
                                     int end,
                                     int size ) throws IOException {
            Object[] ret = new Object[size];
            for (int i = start; i < end; i++) {
                K key = keySerializer.deserialize(in, -1);
                long id = in.readLong();
                ret[i] = new UniqueKey<K>(key, id);
            }
            return ret;
        }

        @Override
        public String toString() {
            return "UniqueKeySerializer<" + keySerializer + ">";
        }

    }

    protected static class NaturalComparator<K extends Comparable<K>> implements Comparator<K>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( K o1,
                            K o2 ) {
            return o1.compareTo(o2);
        }
    }

    /**
     * A key serializer that just writes data without applying any compression.
     * 
     * @param <K> the type to be serialized
     */
    public static final class DelegatingKeySerializer<K extends Comparable<K>> extends BTreeKeySerializer<K>
        implements Serializable, KeySerializerWithComparator<K> {
        private static final long serialVersionUID = 1L;
        protected final transient Serializer<K> defaultSerializer;
        protected final transient Comparator<K> comparator;

        protected DelegatingKeySerializer( Serializer<K> defaultSerializer ) {
            this(defaultSerializer, null);
        }

        protected DelegatingKeySerializer( Serializer<K> defaultSerializer,
                                           Comparator<K> comparator ) {
            this.defaultSerializer = defaultSerializer;
            this.comparator = comparator != null ? comparator : new NaturalComparator<K>();
        }

        @Override
        public Comparator<K> getComparator() {
            return comparator;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public BTreeKeySerializer<K> withComparator( Comparator<?> comparator ) {
            if (comparator == null) return this;
            return new DelegatingKeySerializer<K>(defaultSerializer, (Comparator<K>)comparator);
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void serialize( DataOutput out,
                               int start,
                               int end,
                               Object[] keys ) throws IOException {
            for (int i = start; i < end; i++) {
                defaultSerializer.serialize(out, (K)keys[i]);
            }
        }

        @Override
        public Object[] deserialize( DataInput in,
                                     int start,
                                     int end,
                                     int size ) throws IOException {
            Object[] ret = new Object[size];
            for (int i = start; i < end; i++) {
                ret[i] = defaultSerializer.deserialize(in, -1);
            }
            return ret;
        }

        @Override
        public String toString() {
            return "DelegatingBTreeSerializer<" + defaultSerializer + ">";
        }
    }

    public static interface KeySerializerWithComparator<K> {
        BTreeKeySerializer<K> withComparator( Comparator<?> comparator );
    }

    /**
     * Applies delta packing on {@code java.lang.String}. This serializer splits consequent strings to two parts: shared prefix
     * and different suffix. Only suffix is than stored.
     * 
     * @param <K> the type to be serialized
     */
    public static class PackedStringKeySerializer<K extends Comparable<K>> extends BTreeKeySerializer<K>
        implements Serializable, KeySerializerWithComparator<K> {
        private static final long serialVersionUID = 1L;
        private static final Charset UTF8_CHARSET = Charset.forName("UTF8");

        private final transient ValueFactory<K> valueFactory;
        private final transient StringFactory stringFactory;
        protected final transient Comparator<K> comparator;

        public PackedStringKeySerializer( StringFactory stringFactory,
                                          ValueFactory<K> valueFactory ) {
            this(stringFactory, valueFactory, null);
        }

        protected PackedStringKeySerializer( StringFactory stringFactory,
                                             ValueFactory<K> valueFactory,
                                             Comparator<K> comparator ) {
            this.valueFactory = valueFactory;
            this.stringFactory = stringFactory;
            this.comparator = comparator != null ? comparator : new Comparator<K>() {
                @Override
                public int compare( K o1,
                                    K o2 ) {
                    return o1.compareTo(o2);
                }
            };
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public BTreeKeySerializer<K> withComparator( Comparator<?> comparator ) {
            if (comparator == null) return this;
            return new PackedStringKeySerializer<>(stringFactory, valueFactory, (Comparator<K>)comparator);
        }

        @Override
        public Comparator<K> getComparator() {
            return comparator;
        }

        @Override
        public void serialize( DataOutput out,
                               int start,
                               int end,
                               Object[] keys ) throws IOException {
            byte[] previous = null;
            for (int i = start; i < end; i++) {
                String key = stringFactory.create(keys[i]);
                byte[] b = key.getBytes(UTF8_CHARSET);
                leadingValuePackWrite(out, b, previous, 0);
                previous = b;
            }
        }

        @Override
        public Object[] deserialize( DataInput in,
                                     int start,
                                     int end,
                                     int size ) throws IOException {
            Object[] ret = new Object[size];
            byte[] previous = null;
            for (int i = start; i < end; i++) {
                byte[] b = leadingValuePackRead(in, previous, 0);
                if (b == null) continue;
                String str = new String(b, UTF8_CHARSET);
                ret[i] = valueFactory.create(str);
                previous = b;
            }
            return ret;
        }

        @Override
        public String toString() {
            return "ValueSerializer<" + valueFactory.getPropertyType() + ">";
        }
    }

    public static class DoubleSerializer implements Serializer<Double>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public void serialize( DataOutput out,
                               Double value ) throws IOException {
            out.writeDouble(value.doubleValue());
        }

        @Override
        public Double deserialize( DataInput in,
                                   int available ) throws IOException {
            if (available == 0) return null;
            return in.readDouble();
        }

        @Override
        public int fixedSize() {
            return -1;
        }
    }

    public static class ValueSerializer<V> implements Serializer<V>, Serializable {
        private static final long serialVersionUID = 1L;

        private final transient ValueFactory<V> valueFactory;
        private final transient StringFactory stringFactory;

        public ValueSerializer( StringFactory stringFactory,
                                ValueFactory<V> valueFactory ) {
            this.valueFactory = valueFactory;
            this.stringFactory = stringFactory;
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public void serialize( DataOutput out,
                               V value ) throws IOException {
            out.writeUTF(stringFactory.create(value));
        }

        @Override
        public V deserialize( DataInput in,
                              int available ) throws IOException {
            String pathStr = in.readUTF();
            return valueFactory.create(pathStr);
        }

        @Override
        public String toString() {
            return "ValueSerializer<" + valueFactory.getPropertyType() + ">";
        }
    }
}
