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
package org.modeshape.graph.property.basic;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.IoException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#OBJECT} values.
 */
@Immutable
public class ObjectValueFactory extends AbstractValueFactory<Object> {

    private final ValueFactory<Binary> binaryValueFactory;

    public ObjectValueFactory( TextDecoder decoder,
                               ValueFactory<String> stringValueFactory,
                               ValueFactory<Binary> binaryValueFactory ) {
        super(PropertyType.OBJECT, decoder, stringValueFactory);
        CheckArg.isNotNull(binaryValueFactory, "binaryValueFactory");
        this.binaryValueFactory = binaryValueFactory;
    }

    /**
     * @return binaryValueFactory
     */
    protected ValueFactory<Binary> getBinaryValueFactory() {
        return this.binaryValueFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( String value ) {
        return this.getStringValueFactory().create(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( String value,
                          TextDecoder decoder ) {
        return this.getStringValueFactory().create(value, decoder);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( int value ) {
        return Integer.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( long value ) {
        return Long.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( boolean value ) {
        return Boolean.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( float value ) {
        return Float.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( double value ) {
        return Double.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( BigDecimal value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Calendar value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Date value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.DateTime)
     */
    public Object create( DateTime value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Name value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Path value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Path.Segment value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Reference value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( URI value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(java.util.UUID)
     */
    public Object create( UUID value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractValueFactory#create(java.lang.Object)
     */
    @Override
    public Object create( Object value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractValueFactory#create(java.lang.Object[])
     */
    @Override
    public Object[] create( Object[] values ) {
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( byte[] value ) {
        return getBinaryValueFactory().create(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.Binary)
     */
    public Object create( Binary value ) throws ValueFormatException, IoException {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( InputStream stream,
                          long approximateLength ) {
        return getBinaryValueFactory().create(stream, approximateLength);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Reader reader,
                          long approximateLength ) {
        return getBinaryValueFactory().create(reader, approximateLength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object[] createEmptyArray( int length ) {
        return new Object[length];
    }

}
