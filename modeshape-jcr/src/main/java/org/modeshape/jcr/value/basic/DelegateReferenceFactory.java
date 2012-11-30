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
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The delegating {@link ReferenceFactory} that wraps another. This is useful to extend and override a few methods.
 */
@Immutable
public class DelegateReferenceFactory extends AbstractObjectValueFactory<Reference> implements ReferenceFactory {

    protected final ReferenceFactory delegate;

    public DelegateReferenceFactory( ReferenceFactory delegate ) {
        this.delegate = delegate;
    }

    @Override
    public Reference create( NodeKey value,
                             boolean foreign ) throws ValueFormatException {
        return delegate.create(value, foreign);
    }

    @Override
    public Reference[] create( NodeKey[] value,
                               boolean foreign ) throws ValueFormatException {
        return delegate.create(value, foreign);
    }

    @Override
    public PropertyType getPropertyType() {
        return delegate.getPropertyType();
    }

    @Override
    public Reference create( String value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( String value,
                             TextDecoder decoder ) throws ValueFormatException {
        return delegate.create(value, decoder);
    }

    @Override
    public Reference create( int value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( long value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( boolean value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( float value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( double value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( BigDecimal value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Calendar value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Date value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( DateTime value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Name value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Path value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Segment value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( Reference value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( URI value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( UUID value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( NodeKey value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( byte[] value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference create( BinaryValue value ) throws ValueFormatException, IoException {
        return delegate.create(value);
    }

    @Override
    public Reference create( InputStream stream ) throws ValueFormatException, IoException {
        return delegate.create(stream);
    }

    @Override
    public Reference[] create( String[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( String[] values,
                               TextDecoder decoder ) throws ValueFormatException {
        return delegate.create(values, decoder);
    }

    @Override
    public Reference[] create( int[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( long[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( boolean[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( float[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( double[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( BigDecimal[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Calendar[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Date[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( DateTime[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Name[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Path[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Reference[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( URI[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( UUID[] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( NodeKey[] value ) throws ValueFormatException {
        return delegate.create(value);
    }

    @Override
    public Reference[] create( byte[][] values ) throws ValueFormatException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( BinaryValue[] values ) throws ValueFormatException, IoException {
        return delegate.create(values);
    }

    @Override
    public Reference[] create( Object[] values ) throws ValueFormatException, IoException {
        return delegate.create(values);
    }

    @Override
    public Iterator<Reference> create( Iterator<?> values ) throws ValueFormatException, IoException {
        return delegate.create(values);
    }

    @Override
    public Iterable<Reference> create( Iterable<?> valueIterable ) throws ValueFormatException, IoException {
        return delegate.create(valueIterable);
    }

}
