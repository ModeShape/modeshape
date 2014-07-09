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
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.Serializer;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
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

    }

    public static Serializers serializers( ValueFactories factories ) {
        return new SerializerSupplier(factories);
    }
    public final static Serializer<NodeKey> NODE_KEY_SERIALIZER = new NodeKeySerializer();

    protected final static Serializer<?> DEFAULT_SERIALIZER = Serializer.BASIC;
    protected final static BTreeKeySerializer<?> DEFAULT_BTREE_KEY_SERIALIZER = BTreeKeySerializer.BASIC;

    public static final class SerializerSupplier implements Serializers {
        private final Map<Class<?>, Serializer<?>> serializersByClass;
        private final Map<Class<?>, BTreeKeySerializer<?>> bTreeKeySerializersByClass;
        private final Map<Class<?>, BTreeKeySerializer<?>> packedBTreeKeySerializersByClass;

        protected SerializerSupplier( ValueFactories factories ) {
            // Create the serializers ...
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
            serializersByClass.put(NodeKey.class, NODE_KEY_SERIALIZER);

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
            packedBTreeKeySerializersByClass.put(String.class,
                                                 new PackedStringKeySerializer<String>(stringFactory, stringFactory));
            packedBTreeKeySerializersByClass.put(Name.class, new PackedStringKeySerializer<Name>(stringFactory, nameFactory));
            packedBTreeKeySerializersByClass.put(Path.class, new PackedStringKeySerializer<Path>(stringFactory, pathFactory));
            packedBTreeKeySerializersByClass.put(Name.class, new PackedStringKeySerializer<Name>(stringFactory, nameFactory));
            packedBTreeKeySerializersByClass.put(DateTime.class, new PackedStringKeySerializer<DateTime>(stringFactory,
                                                                                                         dateFactory));
            packedBTreeKeySerializersByClass.put(URI.class, new PackedStringKeySerializer<URI>(stringFactory, uriFactory));
            packedBTreeKeySerializersByClass.put(Reference.class, new PackedStringKeySerializer<Reference>(stringFactory,
                                                                                                           refFactory));
            packedBTreeKeySerializersByClass.put(BigDecimal.class, new PackedStringKeySerializer<BigDecimal>(stringFactory,
                                                                                                             decimalFactory));
        }

        @Override
        public Serializer<?> serializerFor( Class<?> type ) {
            Serializer<?> result = serializersByClass.get(type);
            if (result != null) return result;
            return DEFAULT_SERIALIZER;
        }

        @Override
        public BTreeKeySerializer<?> bTreeKeySerializerFor( Class<?> type,
                                                            final Comparator<?> comparator,
                                                            boolean pack ) {
            Map<Class<?>, BTreeKeySerializer<?>> byClass = pack ? packedBTreeKeySerializersByClass : bTreeKeySerializersByClass;
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
    }

    /**
     * NodeKey serializer for MapDB (which must be in turn, Serializable)
     */
    private static class NodeKeySerializer implements Serializer<NodeKey>, Serializable {
        private static final long serialVersionUID = 1L;

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
    }

    public static <T> BTreeKeySerializer<UniqueKey<T>> uniqueKeySerializer( Serializer<T> serializer,
                                                                            Comparator<T> comparator ) {
        return new UniqueKeySerializer<T>(serializer, uniqueKeyComparator(comparator));
    }

    public static <T> Comparator<UniqueKey<T>> uniqueKeyComparator( Comparator<T> comparator ) {
        return new UniqueKeyComparator<T>(comparator);
    }

    public static final class UniqueKey<K> {
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
        private final transient Comparator<K> valueComparator;

        public UniqueKeyComparator( Comparator<K> valueComparator ) {
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
    }

    public static final class UniqueKeySerializer<K> extends BTreeKeySerializer<UniqueKey<K>> implements Serializable {
        private static final long serialVersionUID = 1L;
        protected final transient Serializer<K> keySerializer;
        protected final transient Comparator<UniqueKey<K>> comparator;

        public UniqueKeySerializer( Serializer<K> keySerializer,
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

    public static class NaturalComparator<K extends Comparable<K>> implements Comparator<K>, Serializable {
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

    private MapDB() {
    }

}
