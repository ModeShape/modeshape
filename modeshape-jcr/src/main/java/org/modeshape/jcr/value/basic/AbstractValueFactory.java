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
package org.modeshape.jcr.value.basic;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * Abstract {@link ValueFactory}.
 * 
 * @param <T> the property type
 */
@Immutable
public abstract class AbstractValueFactory<T> implements ValueFactory<T> {

    private final TextDecoder decoder;
    private final PropertyType propertyType;
    protected final ValueFactories valueFactories;

    protected AbstractValueFactory( PropertyType type,
                                    TextDecoder decoder,
                                    ValueFactories valueFactories ) {
        CheckArg.isNotNull(type, "type");
        this.propertyType = type;
        this.decoder = decoder != null ? decoder : DEFAULT_DECODER;
        this.valueFactories = valueFactories;
    }

    /**
     * @return stringValueFactory
     */
    protected ValueFactory<String> getStringValueFactory() {
        return valueFactories.getStringFactory();
    }

    /**
     * Get the text decoder.
     * 
     * @return the decoder
     */
    public TextDecoder getDecoder() {
        return this.decoder;
    }

    /**
     * Utility method to obtain either the supplied decoder (if not null) or this factory's {@link #getDecoder() decoder}.
     * 
     * @param decoder the decoder, which may be null if this factory's {@link #getDecoder() is to be used}
     * @return the decoder; never null
     */
    protected TextDecoder getDecoder( TextDecoder decoder ) {
        return decoder != null ? decoder : this.getDecoder();
    }

    @Override
    public PropertyType getPropertyType() {
        return propertyType;
    }

    @Override
    public T create( Object value ) {
        if (value == null) return null;
        if (value instanceof String) return create((String)value);
        if (value instanceof Integer) return create(((Integer)value).intValue());
        if (value instanceof Long) return create(((Long)value).longValue());
        if (value instanceof Double) return create(((Double)value).doubleValue());
        if (value instanceof Float) return create(((Float)value).floatValue());
        if (value instanceof Boolean) return create(((Boolean)value).booleanValue());
        if (value instanceof BigDecimal) return create((BigDecimal)value);
        if (value instanceof DateTime) return create((DateTime)value);
        if (value instanceof Calendar) return create((Calendar)value);
        if (value instanceof Date) return create((Date)value);
        if (value instanceof Name) return create((Name)value);
        if (value instanceof Path) return create((Path)value);
        if (value instanceof Path.Segment) return create((Path.Segment)value);
        if (value instanceof Reference) return create((Reference)value);
        if (value instanceof NodeKey) return create((NodeKey)value);
        if (value instanceof UUID) return create((UUID)value);
        if (value instanceof URI) return create((URI)value);
        if (value instanceof BinaryValue) return create((BinaryValue)value);
        if (value instanceof javax.jcr.Binary) {
            javax.jcr.Binary jcrBinary = (javax.jcr.Binary)value;
            try {
                return create(jcrBinary.getStream());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        if (value instanceof byte[]) return create((byte[])value);
        if (value instanceof InputStream) return create((InputStream)value);
        return create(value.toString());
    }

    protected abstract T[] createEmptyArray( int length );

    @Override
    public T[] create( BigDecimal[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( boolean[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( byte[][] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Calendar[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Date[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( DateTime[] values ) throws ValueFormatException {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( double[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( float[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( int[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( long[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Name[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Object[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Path[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( Reference[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( String[] values,
                       TextDecoder decoder ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i], decoder);
        }
        return result;
    }

    @Override
    public T[] create( String[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( URI[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( UUID[] values ) {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( NodeKey[] values ) throws ValueFormatException {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public T[] create( BinaryValue[] values ) throws ValueFormatException, IoException {
        if (values == null) return null;
        final int length = values.length;
        T[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i]);
        }
        return result;
    }

    @Override
    public Iterator<T> create( Iterator<?> values ) throws ValueFormatException, IoException {
        return new ConvertingIterator<T>(values, this);
    }

    @Override
    public Iterable<T> create( final Iterable<?> valueIterable ) throws ValueFormatException, IoException {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return create(valueIterable.iterator());
            }
        };
    }

    protected static class ConvertingIterator<ValueType> implements Iterator<ValueType> {
        private final Iterator<?> delegate;
        private final ValueFactory<ValueType> factory;

        protected ConvertingIterator( Iterator<?> delegate,
                                      ValueFactory<ValueType> factory ) {
            assert delegate != null;
            assert factory != null;
            this.delegate = delegate;
            this.factory = factory;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public ValueType next() {
            return factory.create(this.delegate.next());
        }

        @Override
        public void remove() {
            this.delegate.remove();
        }
    }

}
