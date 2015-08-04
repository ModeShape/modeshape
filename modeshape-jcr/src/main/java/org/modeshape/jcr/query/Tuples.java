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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import org.infinispan.schematic.internal.HashCode;
import org.mapdb.Serializer;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * A simple set of classes for working with tuples of data.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Tuples {

    private Tuples() {
    }

    /**
     * Create a tuple with the given two values.
     *
     * @param v1 the first value
     * @param v2 the second value
     * @return the tuple; never null
     */
    public static <T1, T2> Tuple2<T1, T2> tuple( T1 v1,
                                                 T2 v2 ) {
        return new Tuple2<T1, T2>(v1, v2);
    }

    /**
     * Create a tuple with the given three values.
     *
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @return the tuple; never null
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple( T1 v1,
                                                         T2 v2,
                                                         T3 v3 ) {
        return new Tuple3<T1, T2, T3>(v1, v2, v3);
    }

    /**
     * Create a tuple with the given four values.
     *
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @param v4 the fourth value
     * @return the tuple; never null
     */
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple( T1 v1,
                                                                 T2 v2,
                                                                 T3 v3,
                                                                 T4 v4 ) {
        return new Tuple4<T1, T2, T3, T4>(v1, v2, v3, v4);
    }

    /**
     * Create a tuple with the given values. Note that this always creates a n-ary tuple, so use {@link #tuple(Object, Object)},
     * {@link #tuple(Object, Object, Object)} or {@link #tuple(Object, Object, Object, Object)} for tuples smaller than 5.
     *
     * @param values the values
     * @return the tuple; never null
     */
    public static TupleN tuple( Object[] values ) {
        return new TupleN(values);
    }

    /**
     * Create a type factory for tuples of size 2.
     *
     * @param type1 the first type; may not be null
     * @param type2 the second type; may not be null
     * @return the type factory; never null
     */
    public static <T1, T2> TypeFactory<Tuple2<T1, T2>> typeFactory( TypeFactory<T1> type1,
                                                                    TypeFactory<T2> type2 ) {
        return new Tuple2TypeFactory<T1, T2>(type1, type2);
    }

    /**
     * Create a type factory for tuples of size 3.
     *
     * @param type1 the first type; may not be null
     * @param type2 the second type; may not be null
     * @param type3 the third type; may not be null
     * @return the type factory; never null
     */
    public static <T1, T2, T3> TypeFactory<Tuple3<T1, T2, T3>> typeFactory( TypeFactory<T1> type1,
                                                                            TypeFactory<T2> type2,
                                                                            TypeFactory<T3> type3 ) {
        return new Tuple3TypeFactory<T1, T2, T3>(type1, type2, type3);
    }

    /**
     * Create a type factory for tuples of size 4.
     *
     * @param type1 the first type; may not be null
     * @param type2 the second type; may not be null
     * @param type3 the third type; may not be null
     * @param type4 the fourth type; may not be null
     * @return the type factory; never null
     */
    public static <T1, T2, T3, T4> TypeFactory<Tuple4<T1, T2, T3, T4>> typeFactory( TypeFactory<T1> type1,
                                                                                    TypeFactory<T2> type2,
                                                                                    TypeFactory<T3> type3,
                                                                                    TypeFactory<T4> type4 ) {
        return new Tuple4TypeFactory<T1, T2, T3, T4>(type1, type2, type3, type4);
    }

    /**
     * Create a type factory for n-ary tuples.
     *
     * @param types the collection of types for each slot in the tuple
     * @return the type factory; never null
     */
    public static TypeFactory<?> typeFactory( Collection<TypeFactory<?>> types ) {
        return new TupleNTypeFactory(types);
    }

    /**
     * Create a type factory for uniform tuples.
     *
     * @param type the type of each/every slot in the tuples
     * @param tupleSize the size of the tuples
     * @return the type factory; never null
     */
    public static TypeFactory<?> typeFactory( TypeFactory<?> type,
                                              int tupleSize ) {
        if (tupleSize <= 1) return type;
        if (tupleSize == 2) return typeFactory(type, type);
        if (tupleSize == 3) return typeFactory(type, type, type);
        if (tupleSize == 4) return typeFactory(type, type, type, type);
        Collection<TypeFactory<?>> types = new ArrayList<TypeFactory<?>>(tupleSize);
        for (int i = 0; i != tupleSize; ++i) {
            types.add(type);
        }
        return new TupleNTypeFactory(types);
    }

    /**
     * Create a {@link Serializer} for tuples of size 2.
     *
     * @param first the serializer for the first slot
     * @param second the serializer for the second slot
     * @return the serializer; never null
     */
    public static <T1, T2> Serializer<Tuple2<T1, T2>> serializer( Serializer<T1> first,
                                                                  Serializer<T2> second ) {
        return new Tuple2Serializer<T1, T2>(first, second);
    }

    /**
     * Create a {@link Serializer} for tuples of size 3.
     *
     * @param first the serializer for the first slot
     * @param second the serializer for the second slot
     * @param third the serializer for the third slot
     * @return the serializer; never null
     */
    public static <T1, T2, T3> Serializer<Tuple3<T1, T2, T3>> serializer( Serializer<T1> first,
                                                                          Serializer<T2> second,
                                                                          Serializer<T3> third ) {
        return new Tuple3Serializer<T1, T2, T3>(first, second, third);
    }

    /**
     * Create a {@link Serializer} for tuples of size 4.
     *
     * @param first the serializer for the first slot
     * @param second the serializer for the second slot
     * @param third the serializer for the third slot
     * @param fourth the serializer for the fourth slot
     * @return the serializer; never null
     */
    public static <T1, T2, T3, T4> Serializer<Tuple4<T1, T2, T3, T4>> serializer( Serializer<T1> first,
                                                                                  Serializer<T2> second,
                                                                                  Serializer<T3> third,
                                                                                  Serializer<T4> fourth ) {
        return new Tuple4Serializer<T1, T2, T3, T4>(first, second, third, fourth);
    }

    /**
     * Create a {@link Serializer} for n-ary tuples
     *
     * @param serializers the serializers for each slot in the tuples
     * @return the serializer; never null
     */
    public static Serializer<TupleN> serializer( Serializer<?>[] serializers ) {
        return new TupleNSerializer(serializers);
    }

    /**
     * Create a {@link Serializer} for uniform tuples.
     *
     * @param serializer the serializer of each/every slot in the tuples
     * @param tupleSize the size of the tuples
     * @return the type factory; never null
     */
    public static Serializer<?> serializer( Serializer<?> serializer,
                                            int tupleSize ) {
        if (tupleSize <= 1) return serializer;
        if (tupleSize == 2) return serializer(serializer, serializer);
        if (tupleSize == 3) return serializer(serializer, serializer, serializer);
        if (tupleSize == 4) return serializer(serializer, serializer, serializer, serializer);
        Serializer<?>[] serializers = new Serializer<?>[tupleSize];
        Arrays.fill(serializers, serializer);
        return new TupleNSerializer(serializers);
    }

    public static final class Tuple2Serializer<T1, T2> implements Serializer<Tuple2<T1, T2>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Serializer<T1> serializer1;
        private final Serializer<T2> serializer2;

        public Tuple2Serializer( Serializer<T1> serializer1,
                                 Serializer<T2> serializer2 ) {
            this.serializer1 = serializer1;
            this.serializer2 = serializer2;
        }

        @Override
        public int fixedSize() {
            return 1; // not fixed size
        }

        @Override
        public void serialize( DataOutput out,
                               Tuple2<T1, T2> value ) throws IOException {
            serializer1.serialize(out, value.v1);
            serializer2.serialize(out, value.v2);
        }

        @Override
        public Tuple2<T1, T2> deserialize( DataInput in,
                                           int available ) throws IOException {
            T1 v1 = serializer1.deserialize(in, available);
            T2 v2 = serializer2.deserialize(in, available);
            return tuple(v1, v2);
        }
    }

    public static final class Tuple3Serializer<T1, T2, T3> implements Serializer<Tuple3<T1, T2, T3>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Serializer<T1> serializer1;
        private final Serializer<T2> serializer2;
        private final Serializer<T3> serializer3;

        public Tuple3Serializer( Serializer<T1> serializer1,
                                 Serializer<T2> serializer2,
                                 Serializer<T3> serializer3 ) {
            this.serializer1 = serializer1;
            this.serializer2 = serializer2;
            this.serializer3 = serializer3;
        }

        @Override
        public int fixedSize() {
            return 1; // not fixed size
        }

        @Override
        public void serialize( DataOutput out,
                               Tuple3<T1, T2, T3> value ) throws IOException {
            serializer1.serialize(out, value.v1);
            serializer2.serialize(out, value.v2);
            serializer3.serialize(out, value.v3);
        }

        @Override
        public Tuple3<T1, T2, T3> deserialize( DataInput in,
                                               int available ) throws IOException {
            T1 v1 = serializer1.deserialize(in, available);
            T2 v2 = serializer2.deserialize(in, available);
            T3 v3 = serializer3.deserialize(in, available);
            return tuple(v1, v2, v3);
        }
    }

    public static final class Tuple4Serializer<T1, T2, T3, T4> implements Serializer<Tuple4<T1, T2, T3, T4>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Serializer<T1> serializer1;
        private final Serializer<T2> serializer2;
        private final Serializer<T3> serializer3;
        private final Serializer<T4> serializer4;

        public Tuple4Serializer( Serializer<T1> serializer1,
                                 Serializer<T2> serializer2,
                                 Serializer<T3> serializer3,
                                 Serializer<T4> serializer4 ) {
            this.serializer1 = serializer1;
            this.serializer2 = serializer2;
            this.serializer3 = serializer3;
            this.serializer4 = serializer4;
        }

        @Override
        public int fixedSize() {
            return 1; // not fixed size
        }

        @Override
        public void serialize( DataOutput out,
                               Tuple4<T1, T2, T3, T4> value ) throws IOException {
            serializer1.serialize(out, value.v1);
            serializer2.serialize(out, value.v2);
            serializer3.serialize(out, value.v3);
            serializer4.serialize(out, value.v4);
        }

        @Override
        public Tuple4<T1, T2, T3, T4> deserialize( DataInput in,
                                                   int available ) throws IOException {
            T1 v1 = serializer1.deserialize(in, available);
            T2 v2 = serializer2.deserialize(in, available);
            T3 v3 = serializer3.deserialize(in, available);
            T4 v4 = serializer4.deserialize(in, available);
            return tuple(v1, v2, v3, v4);
        }
    }

    public static final class TupleNSerializer implements Serializer<TupleN>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Serializer<Object>[] serializers;
        private final int size;

        @SuppressWarnings( "unchecked" )
        public TupleNSerializer( Serializer<?>[] serializers ) {
            this.serializers = (Serializer<Object>[])serializers;
            this.size = serializers.length;
        }

        @Override
        public int fixedSize() {
            return 1; // not fixed size
        }

        @Override
        public void serialize( DataOutput out,
                               TupleN value ) throws IOException {
            for (int i = 0; i != size; ++i) {
                serializers[i].serialize(out, value.values[i]);
            }
        }

        @Override
        public TupleN deserialize( DataInput in,
                                   int available ) throws IOException {
            Object[] values = new Object[size];
            for (int i = 0; i != size; ++i) {
                values[i] = serializers[i].deserialize(in, available);
            }
            return tuple(values);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected static int compareValues( Object value1,
                                        Object value2 ) {
        if (value1 == null) {
            return value2 == null ? 0 : -1;
        }
        return value2 == null ? 1 : ((Comparable<Object>)value1).compareTo(value2);
    }

    public static final class Tuple2<T1, T2> implements Comparable<Tuple2<T1, T2>> {
        public final T1 v1;
        public final T2 v2;
        private final int hc;

        public Tuple2( T1 v1,
                       T2 v2 ) {
            this.v1 = v1;
            this.v2 = v2;
            this.hc = HashCode.compute(v1, v2);
        }

        @Override
        public int compareTo( Tuple2<T1, T2> that ) {
            if (that == this) return 0;
            int diff = compareValues(this.v1, that.v1);
            if (diff != 0) return diff;
            return compareValues(this.v2, that.v2);
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Tuple2) {
                Tuple2<?, ?> that = (Tuple2<?, ?>)obj;
                return ObjectUtil.isEqualWithNulls(this.v1, that.v1) && ObjectUtil.isEqualWithNulls(this.v2, that.v2);
            }
            return false;
        }

        @Override
        public String toString() {
            return "<" + v1 + "," + v2 + ">";
        }
    }

    public static final class Tuple3<T1, T2, T3> implements Comparable<Tuple3<T1, T2, T3>> {
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        private final int hc;

        public Tuple3( T1 v1,
                       T2 v2,
                       T3 v3 ) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.hc = HashCode.compute(v1, v2, v3);
        }

        @Override
        public int compareTo( Tuple3<T1, T2, T3> that ) {
            if (that == this) return 0;
            int diff = compareValues(this.v1, that.v1);
            if (diff != 0) return diff;
            diff = compareValues(this.v2, that.v2);
            if (diff != 0) return diff;
            return compareValues(this.v3, that.v3);
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Tuple3) {
                Tuple3<?, ?, ?> that = (Tuple3<?, ?, ?>)obj;
                return ObjectUtil.isEqualWithNulls(this.v1, that.v1) && ObjectUtil.isEqualWithNulls(this.v2, that.v2)
                       && ObjectUtil.isEqualWithNulls(this.v3, that.v3);
            }
            return false;
        }

        @Override
        public String toString() {
            return "<" + v1 + "," + v2 + "," + v3 + ">";
        }
    }

    public static final class Tuple4<T1, T2, T3, T4> implements Comparable<Tuple4<T1, T2, T3, T4>> {
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        public final T4 v4;
        private final int hc;

        public Tuple4( T1 v1,
                       T2 v2,
                       T3 v3,
                       T4 v4 ) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.hc = HashCode.compute(v1, v2, v3, v4);
        }

        @Override
        public int compareTo( Tuple4<T1, T2, T3, T4> that ) {
            if (that == this) return 0;
            int diff = compareValues(this.v1, that.v1);
            if (diff != 0) return diff;
            diff = compareValues(this.v2, that.v2);
            if (diff != 0) return diff;
            diff = compareValues(this.v3, that.v3);
            if (diff != 0) return diff;
            return compareValues(this.v4, that.v4);
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Tuple4) {
                Tuple4<?, ?, ?, ?> that = (Tuple4<?, ?, ?, ?>)obj;
                return ObjectUtil.isEqualWithNulls(this.v1, that.v1) && ObjectUtil.isEqualWithNulls(this.v2, that.v2)
                       && ObjectUtil.isEqualWithNulls(this.v3, that.v3) && ObjectUtil.isEqualWithNulls(this.v4, that.v4);
            }
            return false;
        }

        @Override
        public String toString() {
            return "<" + v1 + "," + v2 + "," + v3 + "," + v4 + ">";
        }
    }

    public static final class TupleN implements Comparable<TupleN> {
        protected final Object[] values;
        private final int hc;

        public TupleN( Object[] values ) {
            this.values = values;
            this.hc = HashCode.compute(values);
        }

        @Override
        public int compareTo( TupleN that ) {
            if (that == this) return 0;
            int diff = this.values.length - that.values.length;
            if (diff != 0) return diff;
            for (int i = 0; i != values.length; ++i) {
                diff = compareValues(this.values[i], that.values[i]);
                if (diff != 0) return diff;
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof TupleN) {
                TupleN that = (TupleN)obj;
                if (this.values.length == that.values.length) {
                    for (int i = 0; i != values.length; ++i) {
                        if (ObjectUtil.isEqualWithNulls(this.values[i], that.values[i])) return false;
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    public interface TupleFactory<T> {
        Serializer<T> getSerializer( BufferManager bufferMgr );
    }

    protected static final class Tuple2Comparator<T1, T2> implements Comparator<Tuple2<T1, T2>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Comparator<T1> comparator1;
        private final Comparator<T2> comparator2;

        protected Tuple2Comparator( Comparator<T1> comparator1,
                                    Comparator<T2> comparator2 ) {
            this.comparator1 = comparator1;
            this.comparator2 = comparator2;
        }

        @Override
        public int compare( Tuple2<T1, T2> arg0,
                            Tuple2<T1, T2> arg1 ) {
            int diff = comparator1.compare(arg0.v1, arg1.v1);
            if (diff != 0) return diff;
            return comparator2.compare(arg0.v2, arg1.v2);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Tuple2Comparator) {
                @SuppressWarnings( "unchecked" )
                Tuple2Comparator<T1, T2> that = (Tuple2Comparator<T1, T2>)obj;
                return comparator1.equals(that.comparator1) && comparator2.equals(that.comparator2);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    @SuppressWarnings( "unchecked" )
    protected static final class Tuple2TypeFactory<T1, T2> implements TypeFactory<Tuple2<T1, T2>>, TupleFactory<Tuple2<T1, T2>> {

        private final TypeFactory<T1> type1;
        private final TypeFactory<T2> type2;
        private final Class<Tuple2<T1, T2>> type;
        private final Comparator<Tuple2<T1, T2>> comparator;
        private final String typeName;

        protected Tuple2TypeFactory( TypeFactory<T1> type1,
                                     TypeFactory<T2> type2 ) {
            this.type1 = type1;
            this.type2 = type2;
            Class<?> clazz = Tuple2.class;
            this.type = (Class<Tuple2<T1, T2>>)clazz;
            this.comparator = new Tuple2Comparator<T1, T2>(type1.getComparator(), type2.getComparator());
            this.typeName = "Tuple2<" + type1.getTypeName() + "," + type2.getTypeName() + ">";
        }

        @Override
        public Serializer<Tuple2<T1, T2>> getSerializer( BufferManager bufferMgr ) {
            Serializer<T1> ser1 = (Serializer<T1>)bufferMgr.nullSafeSerializerFor(type1);
            Serializer<T2> ser2 = (Serializer<T2>)bufferMgr.nullSafeSerializerFor(type2);
            return new Tuple2Serializer<T1, T2>(ser1, ser2);
        }

        @Override
        public Class<Tuple2<T1, T2>> getType() {
            return this.type;
        }

        @Override
        public Comparator<Tuple2<T1, T2>> getComparator() {
            return comparator;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        @Override
        public Tuple2<T1, T2> create( String value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tuple2<T1, T2> create( Object value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asString( Object value ) {
            return value.toString();
        }

        @Override
        public long length( Object value ) {
            if (value instanceof Tuple2) {
                Tuple2<?, ?> tuple = (Tuple2<?, ?>)value;
                return type1.length(tuple.v1) + type2.length(tuple.v2);
            }
            return asString(value).length();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected static final class Tuple3TypeFactory<T1, T2, T3>
        implements TypeFactory<Tuple3<T1, T2, T3>>, TupleFactory<Tuple3<T1, T2, T3>> {

        private final TypeFactory<T1> type1;
        private final TypeFactory<T2> type2;
        private final TypeFactory<T3> type3;
        private final Class<Tuple3<T1, T2, T3>> type;
        private final Comparator<Tuple3<T1, T2, T3>> comparator;
        private final String typeName;

        protected Tuple3TypeFactory( TypeFactory<T1> type1,
                                     TypeFactory<T2> type2,
                                     TypeFactory<T3> type3 ) {
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
            Class<?> clazz = Tuple3.class;
            this.type = (Class<Tuple3<T1, T2, T3>>)clazz;
            final Comparator<T1> comparator1 = type1.getComparator();
            final Comparator<T2> comparator2 = type2.getComparator();
            final Comparator<T3> comparator3 = type3.getComparator();
            this.comparator = new Comparator<Tuple3<T1, T2, T3>>() {
                @Override
                public int compare( Tuple3<T1, T2, T3> arg0,
                                    Tuple3<T1, T2, T3> arg1 ) {
                    int diff = comparator1.compare(arg0.v1, arg1.v1);
                    if (diff != 0) return diff;
                    diff = comparator2.compare(arg0.v2, arg1.v2);
                    if (diff != 0) return diff;
                    return comparator3.compare(arg0.v3, arg1.v3);
                }
            };
            this.typeName = "Tuple3<" + type1.getTypeName() + "," + type2.getTypeName() + "," + type3.getTypeName() + ">";

        }

        @Override
        public Serializer<Tuple3<T1, T2, T3>> getSerializer( BufferManager bufferMgr ) {
            Serializer<T1> ser1 = (Serializer<T1>)bufferMgr.nullSafeSerializerFor(type1);
            Serializer<T2> ser2 = (Serializer<T2>)bufferMgr.nullSafeSerializerFor(type2);
            Serializer<T3> ser3 = (Serializer<T3>)bufferMgr.nullSafeSerializerFor(type3);
            return new Tuple3Serializer<T1, T2, T3>(ser1, ser2, ser3);
        }

        @Override
        public Class<Tuple3<T1, T2, T3>> getType() {
            return this.type;
        }

        @Override
        public Comparator<Tuple3<T1, T2, T3>> getComparator() {
            return comparator;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        @Override
        public Tuple3<T1, T2, T3> create( String value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tuple3<T1, T2, T3> create( Object value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asString( Object value ) {
            return value.toString();
        }

        @Override
        public long length( Object value ) {
            if (value instanceof Tuple3) {
                Tuple3<?, ?, ?> tuple = (Tuple3<?, ?, ?>)value;
                return type1.length(tuple.v1) + type2.length(tuple.v2) + type3.length(tuple.v3);
            }
            return asString(value).length();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected static final class Tuple4TypeFactory<T1, T2, T3, T4>
        implements TypeFactory<Tuple4<T1, T2, T3, T4>>, TupleFactory<Tuple4<T1, T2, T3, T4>> {

        private final TypeFactory<T1> type1;
        private final TypeFactory<T2> type2;
        private final TypeFactory<T3> type3;
        private final TypeFactory<T4> type4;
        private final Class<Tuple4<T1, T2, T3, T4>> type;
        private final Comparator<Tuple4<T1, T2, T3, T4>> comparator;
        private final String typeName;

        protected Tuple4TypeFactory( TypeFactory<T1> type1,
                                     TypeFactory<T2> type2,
                                     TypeFactory<T3> type3,
                                     TypeFactory<T4> type4 ) {
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
            this.type4 = type4;
            Class<?> clazz = Tuple4.class;
            this.type = (Class<Tuple4<T1, T2, T3, T4>>)clazz;
            final Comparator<T1> comparator1 = type1.getComparator();
            final Comparator<T2> comparator2 = type2.getComparator();
            final Comparator<T3> comparator3 = type3.getComparator();
            final Comparator<T4> comparator4 = type4.getComparator();
            this.comparator = new Comparator<Tuple4<T1, T2, T3, T4>>() {
                @Override
                public int compare( Tuple4<T1, T2, T3, T4> arg0,
                                    Tuple4<T1, T2, T3, T4> arg1 ) {
                    int diff = comparator1.compare(arg0.v1, arg1.v1);
                    if (diff != 0) return diff;
                    diff = comparator2.compare(arg0.v2, arg1.v2);
                    if (diff != 0) return diff;
                    diff = comparator3.compare(arg0.v3, arg1.v3);
                    if (diff != 0) return diff;
                    return comparator4.compare(arg0.v4, arg1.v4);
                }
            };
            this.typeName = "Tuple4<" + type1.getTypeName() + "," + type2.getTypeName() + "," + type3.getTypeName() + ","
                            + type4.getTypeName() + ">";
        }

        @Override
        public Serializer<Tuple4<T1, T2, T3, T4>> getSerializer( BufferManager bufferMgr ) {
            Serializer<T1> ser1 = (Serializer<T1>)bufferMgr.nullSafeSerializerFor(type1);
            Serializer<T2> ser2 = (Serializer<T2>)bufferMgr.nullSafeSerializerFor(type2);
            Serializer<T3> ser3 = (Serializer<T3>)bufferMgr.nullSafeSerializerFor(type3);
            Serializer<T4> ser4 = (Serializer<T4>)bufferMgr.nullSafeSerializerFor(type4);
            return new Tuple4Serializer<T1, T2, T3, T4>(ser1, ser2, ser3, ser4);
        }

        @Override
        public Class<Tuple4<T1, T2, T3, T4>> getType() {
            return this.type;
        }

        @Override
        public Comparator<Tuple4<T1, T2, T3, T4>> getComparator() {
            return comparator;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        @Override
        public Tuple4<T1, T2, T3, T4> create( String value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tuple4<T1, T2, T3, T4> create( Object value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asString( Object value ) {
            return value.toString();
        }

        @Override
        public long length( Object value ) {
            if (value instanceof Tuple4) {
                Tuple4<?, ?, ?, ?> tuple = (Tuple4<?, ?, ?, ?>)value;
                return type1.length(tuple.v1) + type2.length(tuple.v2) + type3.length(tuple.v3) + type4.length(tuple.v4);
            }
            return asString(value).length();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected static final class TupleNTypeFactory implements TypeFactory<TupleN>, TupleFactory<TupleN> {

        protected final TypeFactory<?>[] types;
        private final Comparator<TupleN> comparator;
        private final String typeName;

        protected TupleNTypeFactory( Collection<TypeFactory<?>> typeFactories ) {
            this.types = new TypeFactory<?>[typeFactories.size()];
            final Comparator<Object>[] comparators = new Comparator[types.length];
            Iterator<TypeFactory<?>> typeIter = typeFactories.iterator();
            for (int i = 0; i != this.types.length; ++i) {
                TypeFactory<?> typeFactory = typeIter.next();
                this.types[i] = typeFactory;
                comparators[i] = (Comparator<Object>)typeFactory.getComparator();
            }
            this.comparator = new Comparator<TupleN>() {
                @Override
                public int compare( TupleN arg0,
                                    TupleN arg1 ) {
                    int diff = 0;
                    for (int i = 0; i != types.length; ++i) {
                        diff = comparators[i].compare(arg0.values[i], arg1.values[i]);
                        if (diff != 0) return diff;
                    }
                    return 0;
                }
            };
            StringBuilder sb = new StringBuilder("TupleN<");
            for (int i = 0; i != types.length; ++i) {
                if (i != 0) sb.append(',');
                sb.append(types[i].getTypeName());
            }
            sb.append(">");
            this.typeName = sb.toString();
        }

        @Override
        public Serializer<TupleN> getSerializer( BufferManager bufferMgr ) {
            Serializer<?>[] serializers = new Serializer<?>[types.length];
            for (int i = 0; i != types.length; ++i) {
                serializers[i] = (Serializer<?>)bufferMgr.nullSafeSerializerFor(types[i]);
            }
            return new TupleNSerializer(serializers);
        }

        @Override
        public Class<TupleN> getType() {
            return TupleN.class;
        }

        @Override
        public Comparator<TupleN> getComparator() {
            return comparator;
        }

        @Override
        public String getTypeName() {
            return this.typeName;
        }

        @Override
        public TupleN create( String value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public TupleN create( Object value ) throws ValueFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asString( Object value ) {
            return value.toString();
        }

        @Override
        public long length( Object value ) {
            if (value instanceof TupleN) {
                TupleN tuple = (TupleN)value;
                int len = 0;
                for (int i = 0; i != types.length; ++i) {
                    len += types[i].length(tuple.values[i]);
                }
                return len;
            }
            return asString(value).length();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }
    }

}
