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

package org.modeshape.jcr.index.local;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Serializer;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class MapDB {

    public static interface Serializers {
        /**
         * Obtain a serializer for the given value type.
         *
         * @param type the type; may not be null
         * @return the serializer
         */
        Serializer<?> serializerFor( Class<?> type );

        /**
         * Obtain a serializer for the given key type.
         *
         * @param type the type; may not be null
         * @param comparator the comparator; may not be null
         * @param pack true if the serializer can/should pack keys together when possible, or false otherwise
         * @return the serializer
         */
        BTreeKeySerializer<?> bTreeKeySerializerFor( Class<?> type,
                                                     Comparator<?> comparator,
                                                     boolean pack );

        /**
         * Obtain a serializer for the given value type that can handle nulls.
         *
         * @param type the type; may not be null
         * @return the serializer that can handle nulls
         */
        Serializer<?> nullSafeSerializerFor( Class<?> type );
    }

    public static Serializers serializers( ValueFactories factories ) {
        return new SerializerSupplier(factories);
    }

    public final static Serializer<NodeKey> NODE_KEY_SERIALIZER = new NodeKeySerializer();

    protected final static Serializer<?> DEFAULT_SERIALIZER = Serializer.BASIC;
    protected final static BTreeKeySerializer<?> DEFAULT_BTREE_KEY_SERIALIZER = BTreeKeySerializer.BASIC;

    public static final class SerializerSupplier implements Serializers {
        private final Map<Class<?>, Serializer<?>> serializersByClass;
        private final Map<Class<?>, Serializer<?>> nullSafeSerializersByClass;
        private final Map<Class<?>, BTreeKeySerializer<?>> bTreeKeySerializersByClass;
        private final Serializer<?> DEFAULT_NULL_SAFE_SERIALIZER;

        protected SerializerSupplier( ValueFactories factories ) {
            DEFAULT_NULL_SAFE_SERIALIZER = (isNullSafe(DEFAULT_SERIALIZER) ? DEFAULT_SERIALIZER
                                                                           : new NullSafeSerializer<>(DEFAULT_SERIALIZER));

            // Create the serializers ...
            serializersByClass = new HashMap<Class<?>, Serializer<?>>();
            serializersByClass.put(String.class, Serializer.STRING);
            serializersByClass.put(Long.class, Serializer.LONG);
            serializersByClass.put(Boolean.class, Serializer.BOOLEAN);
            serializersByClass.put(Double.class, new DoubleSerializer());
            serializersByClass.put(BigDecimal.class, Serializer.JAVA);
            serializersByClass.put(URI.class, Serializer.JAVA);
            serializersByClass.put(DateTime.class, Serializer.JAVA);
            serializersByClass.put(Path.class, Serializer.JAVA);
            serializersByClass.put(Name.class, Serializer.JAVA);
            serializersByClass.put(Reference.class, Serializer.JAVA);
            serializersByClass.put(NodeKey.class, NODE_KEY_SERIALIZER);

            bTreeKeySerializersByClass = new HashMap<Class<?>, BTreeKeySerializer<?>>();
            nullSafeSerializersByClass = new HashMap<Class<?>, Serializer<?>>();
            for (Map.Entry<Class<?>, Serializer<?>> entry : serializersByClass.entrySet()) {
                Serializer<?> serializer = entry.getValue();
                @SuppressWarnings( {"rawtypes", "unchecked"} )
                BTreeKeySerializer<?> bTreeSerializer = new DelegatingKeySerializer(serializer);
                bTreeKeySerializersByClass.put(entry.getKey(), bTreeSerializer);

                Serializer<?> nullsafe = (isNullSafe(serializer) ? serializer : new NullSafeSerializer<>(serializer));
                nullSafeSerializersByClass.put(entry.getKey(), nullsafe);
            }
        }

        private static <T> boolean isNullSafe(Serializer<T> serializer) {
            try {
                serializer.serialize(new DataOutput2(), null);
                return true;
            } catch(IOException | NullPointerException e) {
                return false;
            }
        }

        @Override
        public Serializer<?> serializerFor( Class<?> type ) {
            Serializer<?> result = serializersByClass.get(type);
            if (result != null) return result;
            return DEFAULT_SERIALIZER;
        }

        public Serializer<?> nullSafeSerializerFor( Class<?> type ) {
            Serializer<?> result = nullSafeSerializersByClass.get(type);
            if (result != null) return result;
            return DEFAULT_NULL_SAFE_SERIALIZER;
        }

        @Override
        public BTreeKeySerializer<?> bTreeKeySerializerFor( Class<?> type,
                                                            final Comparator<?> comparator,
                                                            boolean pack ) {
            Map<Class<?>, BTreeKeySerializer<?>> byClass = bTreeKeySerializersByClass;
            final BTreeKeySerializer<?> result = byClass.containsKey(type) ? byClass.get(type) : DEFAULT_BTREE_KEY_SERIALIZER;
            // Make sure the serializer uses the type's comparator ...
            if (result instanceof KeySerializerWithComparator) {
                KeySerializerWithComparator<?> serializer = (KeySerializerWithComparator<?>)result;
                assert comparator != null;
                return serializer.withComparator(comparator);
            }
            return bTreeKeySerializerWith(result, comparator);
        }

        private <T> BTreeKeySerializer<T> bTreeKeySerializerWith( final BTreeKeySerializer<?> original,
                                                                  final Comparator<T> comparator ) {
            return new BTreeKeySerializerWitheComparator<T>(original, comparator);
        }

        private static class NullSafeSerializer<T> implements Serializer<T>, Serializable {
            private Serializer<T> baseSerializer;

            public NullSafeSerializer(Serializer<T> baseSerializer) {
                this.baseSerializer = baseSerializer;
            }

            @Override
            public void serialize(DataOutput dataOutput, T t) throws IOException {
                if (t == null) {
                    dataOutput.writeBoolean(false);
                } else {
                    dataOutput.writeBoolean(true);
                    baseSerializer.serialize(dataOutput, t);
                }
            }

            @Override
            public T deserialize(DataInput dataInput, int i) throws IOException {
                if (!dataInput.readBoolean()) {
                    return null;
                } else {
                    return baseSerializer.deserialize(dataInput, i);
                }
            }

            @Override
            public int fixedSize() {
                return -1;
            }
        }
    }

    /**
     * NodeKey serializer for MapDB (which must be in turn, Serializable)
     */
    private static class NodeKeySerializer implements Serializer<NodeKey>, Serializable {
        private static final long serialVersionUID = 1L;

        protected NodeKeySerializer() {
        }

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
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            return obj instanceof NodeKeySerializer;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }
    }

    private static class BTreeKeySerializerWitheComparator<T> extends BTreeKeySerializer<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private final BTreeKeySerializer<?> original;
        private final Comparator<T> comparator;

        protected BTreeKeySerializerWitheComparator( BTreeKeySerializer<?> original,
                                                     Comparator<T> comparator ) {
            this.original = original;
            this.comparator = comparator;
        }

        @Override
        public Object[] deserialize( DataInput in,
                                     int start,
                                     int end,
                                     int size ) throws IOException {
            return original.deserialize(in, start, end, size);
        }

        @Override
        public Comparator<T> getComparator() {
            return comparator;
        }

        @Override
        public void serialize( DataOutput out,
                               int start,
                               int end,
                               Object[] keys ) throws IOException {
            original.serialize(out, start, end, keys);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof BTreeKeySerializerWitheComparator) {
                @SuppressWarnings( "unchecked" )
                BTreeKeySerializerWitheComparator<T> that = (BTreeKeySerializerWitheComparator<T>)obj;
                return original.equals(that.original) && comparator.equals(comparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    public static <T> BTreeKeySerializer<UniqueKey<T>> uniqueKeyBTreeSerializer( Serializer<T> serializer,
                                                                                 Comparator<T> comparator ) {
        return new UniqueKeyBTreeSerializer<T>(serializer, uniqueKeyComparator(comparator));
    }

    public static <T> Serializer<UniqueKey<T>> uniqueKeySerializer( Serializer<T> serializer,
                                                                    Comparator<T> comparator ) {
        return new UniqueKeySerializer<T>(serializer, uniqueKeyComparator(comparator));
    }

    public static <T> Comparator<UniqueKey<T>> uniqueKeyComparator( Comparator<T> comparator ) {
        return new UniqueKeyComparator<T>(comparator);
    }

    public static <A, B> Comparator<Fun.Tuple2<A, B>> tupleComparator( Comparator<A> aComparator,
                                                                       Comparator<B> bComparator ) {
        return new TupleComparator<A, B>(aComparator, bComparator);
    }

    public static <A, B> BTreeKeySerializer<Fun.Tuple2<A, B>> tupleBTreeSerializer( Comparator<A> aComparator,
                                                                                    Serializer<A> aSerializer,
                                                                                    Serializer<B> bSerializer,
                                                                                    Comparator<Fun.Tuple2<A, B>> tupleComparator ) {
        return new LocalTuple2KeySerializer<>(aComparator, aSerializer, bSerializer, tupleComparator);
    }

    public static final class UniqueKey<K> implements Serializable {
        private static final long serialVersionUID = 1L;

        protected final K actualKey;
        protected final long id;
        private final int hc;

        public UniqueKey( K actualKey,
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

    public static final class UniqueKeyComparator<K> implements Comparator<UniqueKey<K>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Comparator<K> valueComparator;

        public UniqueKeyComparator( Comparator<K> valueComparator ) {
            this.valueComparator = valueComparator;
        }

        @Override
        public int compare( UniqueKey<K> o1,
                            UniqueKey<K> o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) {
                return o2 == null ? 0 : 1;
            } else if (o2 == null) {
                return -1;
            }
            int diff = valueComparator.compare(o1.actualKey, o2.actualKey);
            if (diff != 0) return diff;
            long ldiff = o1.id - o2.id;
            return ldiff == 0L ? 0 : (ldiff <= 0L ? -1 : 1);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof UniqueKeyComparator) {
                @SuppressWarnings( "unchecked" )
                UniqueKeyComparator<K> that = (UniqueKeyComparator<K>)obj;
                return valueComparator.equals(that.valueComparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    public static final class ComparableUniqueKeyComparator<K> implements Comparator<UniqueKey<K>>, Serializable {
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

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ComparableUniqueKeyComparator) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }

    }

    public static final class UniqueKeySerializer<K> implements Serializer<UniqueKey<K>>, Serializable {
        private static final long serialVersionUID = 1L;
        protected final Serializer<K> keySerializer;
        protected final Comparator<UniqueKey<K>> comparator;

        public UniqueKeySerializer( Serializer<K> keySerializer,
                                    Comparator<UniqueKey<K>> comparator ) {
            this.keySerializer = keySerializer;
            this.comparator = comparator;
        }

        @Override
        public UniqueKey<K> deserialize( DataInput in,
                                         int available ) throws IOException {
            K actualKey = keySerializer.deserialize(in, available);
            long id = in.readLong();
            return new UniqueKey<K>(actualKey, id);
        }

        @Override
        public void serialize( DataOutput out,
                               UniqueKey<K> value ) throws IOException {
            keySerializer.serialize(out, value.actualKey);
            out.writeLong(value.id);
        }

        @Override
        public int fixedSize() {
            return -1;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof UniqueKeySerializer) {
                @SuppressWarnings( "unchecked" )
                UniqueKeySerializer<K> that = (UniqueKeySerializer<K>)obj;
                return keySerializer.equals(that.keySerializer) && comparator.equals(that.comparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return "UniqueKeySerializer<" + keySerializer + ">";
        }

    }

    public static final class UniqueKeyBTreeSerializer<K> extends BTreeKeySerializer<UniqueKey<K>> implements Serializable {
        private static final long serialVersionUID = 1L;
        protected final Serializer<K> keySerializer;
        protected final Comparator<UniqueKey<K>> comparator;

        public UniqueKeyBTreeSerializer( Serializer<K> keySerializer,
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
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof UniqueKeyBTreeSerializer) {
                @SuppressWarnings( "unchecked" )
                UniqueKeyBTreeSerializer<K> that = (UniqueKeyBTreeSerializer<K>)obj;
                return keySerializer.equals(that.keySerializer) && comparator.equals(that.comparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return "UniqueKeyBTreeSerializer<" + keySerializer + ">";
        }

    }

    public static class NaturalComparator<K extends Comparable<K>> implements Comparator<K>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( K o1,
                            K o2 ) {
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NaturalComparator) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
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
        protected final Serializer<K> defaultSerializer;
        protected final Comparator<K> comparator;

        public DelegatingKeySerializer( Serializer<K> defaultSerializer ) {
            this(defaultSerializer, null);
        }

        public DelegatingKeySerializer( Serializer<K> defaultSerializer,
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
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof DelegatingKeySerializer) {
                @SuppressWarnings( "unchecked" )
                DelegatingKeySerializer<K> that = (DelegatingKeySerializer<K>)obj;
                return defaultSerializer.equals(that.defaultSerializer) && comparator.equals(that.comparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return "DelegatingBTreeSerializer<" + defaultSerializer + ">";
        }
    }

    public static interface KeySerializerWithComparator<K> {
        BTreeKeySerializer<K> withComparator( Comparator<?> comparator );
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
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof DoubleSerializer) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public int fixedSize() {
            return -1;
        }
    }

    /**
     * Applies delta compression on array of tuple. First tuple value may be shared between consequentive tuples, so only first
     * occurrence is serialized. An example:
     *
     * <pre>
     *     Value            Serialized as
     *     -------------------------
     *     Tuple(1, 1)       1, 1
     *     Tuple(1, 2)          2
     *     Tuple(1, 3)          3
     *     Tuple(1, 4)          4
     * </pre>
     *
     * @param <A> first tuple value
     * @param <B> second tuple value
     */
    @SuppressWarnings( "unchecked" )
    protected final static class LocalTuple2KeySerializer<A, B> extends BTreeKeySerializer<Fun.Tuple2<A, B>>
        implements Serializable {

        private static final long serialVersionUID = 0L;
        protected final Comparator<A> aComparator;
        protected final Serializer<A> aSerializer;
        protected final Serializer<B> bSerializer;
        protected final Comparator<Fun.Tuple2<A, B>> comparator;

        /**
         * Construct new Tuple2 Key Serializer. You may pass null for some value, In that case 'default' value will be used,
         * Comparable comparator and Default Serializer from DB.
         *
         * @param aComparator comparator used for first tuple value
         * @param aSerializer serializer used for first tuple value
         * @param bSerializer serializer used for second tuple value
         * @param comparator the comparator for the tuple
         */
        public LocalTuple2KeySerializer( Comparator<A> aComparator,
                                         Serializer<A> aSerializer,
                                         Serializer<B> bSerializer,
                                         Comparator<Fun.Tuple2<A, B>> comparator ) {
            this.aComparator = aComparator;
            this.aSerializer = aSerializer;
            this.bSerializer = bSerializer;
            this.comparator = comparator;
        }

        @Override
        public void serialize( DataOutput out,
                               int start,
                               int end,
                               Object[] keys ) throws IOException {
            int acount = 0;
            for (int i = start; i < end; i++) {
                Fun.Tuple2<A, B> t = (Fun.Tuple2<A, B>)keys[i];
                if (acount == 0) {
                    // write new A
                    aSerializer.serialize(out, t.a);
                    // count how many A are following
                    acount = 1;
                    while (i + acount < end && aComparator.compare(t.a, ((Fun.Tuple2<A, B>)keys[i + acount]).a) == 0) {
                        acount++;
                    }
                    DataOutput2.packInt(out, acount);
                }
                bSerializer.serialize(out, t.b);

                acount--;
            }
        }

        @Override
        public Object[] deserialize( DataInput in,
                                     int start,
                                     int end,
                                     int size ) throws IOException {
            Object[] ret = new Object[size];
            A a = null;
            int acount = 0;

            for (int i = start; i < end; i++) {
                if (acount == 0) {
                    // read new A
                    a = aSerializer.deserialize(in, -1);
                    acount = DataInput2.unpackInt(in);
                }
                B b = bSerializer.deserialize(in, -1);
                ret[i] = Fun.t2(a, b);
                acount--;
            }
            assert (acount == 0);

            return ret;
        }

        @Override
        public Comparator<Fun.Tuple2<A, B>> getComparator() {
            return comparator;
        }

        @Override
        public boolean equals( Object o ) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocalTuple2KeySerializer<A, B> t = (LocalTuple2KeySerializer<A, B>)o;
            return Fun.eq(aComparator, t.aComparator) && Fun.eq(aSerializer, t.aSerializer) && Fun.eq(bSerializer, t.bSerializer);
        }

        @Override
        public int hashCode() {
            int result = aComparator != null ? aComparator.hashCode() : 0;
            result = 31 * result + (aSerializer != null ? aSerializer.hashCode() : 0);
            result = 31 * result + (bSerializer != null ? bSerializer.hashCode() : 0);
            return result;
        }
    }

    protected final static class TupleComparator<A, B> implements Comparator<Fun.Tuple2<A, B>>, Serializable {
        private static final long serialVersionUID = 1L;

        private final Comparator<A> aComparator;
        private final Comparator<B> bComparator;

        protected TupleComparator( Comparator<A> aComparator,
                                   Comparator<B> bComparator ) {
            this.aComparator = aComparator;
            this.bComparator = bComparator;
        }

        @Override
        public int compare( Tuple2<A, B> o1,
                            Tuple2<A, B> o2 ) {
            int i = aComparator.compare(o1.a, o2.a);
            if (i != 0) return i;
            // Before we check the second value in the tuples, check them against the "positive infinity" values ...
            if (o1.b == null) {
                // This is negative infinity ...
                if (o2.b == null) {
                    // Both are negative infinity ...
                    return 0;
                }
                // o1.b is negative infinity, but o2.b is not
                return -1;
            } else if (o2.b == null) {
                // This is negative infinity, but o1.b is not ...
                return 1;
            }
            if (o1.b == Fun.HI) {
                if (o2.b == Fun.HI) {
                    // Both are positive infinity ...
                    return 0;
                }
                // o1.b is positive infinity, and o2.b is not ...
                return 1;
            } else if (o2.b == Fun.HI) {
                // o1.b is not positive infinity, and o1.b is ...
                return -1;
            }
            // Neither is positive infinity, so use the actual comparator ...
            i = bComparator.compare(o1.b, o2.b);
            return i;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof TupleComparator) {
                @SuppressWarnings( "unchecked" )
                TupleComparator<A, B> that = (TupleComparator<A, B>)obj;
                return aComparator.equals(that.aComparator) && bComparator.equals(that.bComparator);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private MapDB() {
    }

}
