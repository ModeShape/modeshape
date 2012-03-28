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
import java.util.UUID;
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
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#OBJECT} values.
 */
@Immutable
public class ObjectValueFactory extends AbstractValueFactory<Object> {

    private final ValueFactory<BinaryValue> binaryValueFactory;

    public ObjectValueFactory( TextDecoder decoder,
                               ValueFactory<String> stringValueFactory,
                               ValueFactory<BinaryValue> binaryValueFactory ) {
        super(PropertyType.OBJECT, decoder, stringValueFactory);
        CheckArg.isNotNull(binaryValueFactory, "binaryValueFactory");
        this.binaryValueFactory = binaryValueFactory;
    }

    /**
     * @return binaryValueFactory
     */
    protected ValueFactory<BinaryValue> getBinaryValueFactory() {
        return this.binaryValueFactory;
    }

    @Override
    public Object create( String value ) {
        return this.getStringValueFactory().create(value);
    }

    @Override
    public Object create( String value,
                          TextDecoder decoder ) {
        return this.getStringValueFactory().create(value, decoder);
    }

    @Override
    public Object create( int value ) {
        return Integer.valueOf(value);
    }

    @Override
    public Object create( long value ) {
        return Long.valueOf(value);
    }

    @Override
    public Object create( boolean value ) {
        return Boolean.valueOf(value);
    }

    @Override
    public Object create( float value ) {
        return Float.valueOf(value);
    }

    @Override
    public Object create( double value ) {
        return Double.valueOf(value);
    }

    @Override
    public Object create( BigDecimal value ) {
        return value;
    }

    @Override
    public Object create( Calendar value ) {
        return value;
    }

    @Override
    public Object create( Date value ) {
        return value;
    }

    @Override
    public Object create( DateTime value ) {
        return value;
    }

    @Override
    public Object create( Name value ) {
        return value;
    }

    @Override
    public Object create( Path value ) {
        return value;
    }

    @Override
    public Object create( Path.Segment value ) {
        return value;
    }

    @Override
    public Object create( Reference value ) {
        return value;
    }

    @Override
    public Object create( URI value ) {
        return value;
    }

    @Override
    public Object create( UUID value ) {
        return value;
    }

    @Override
    public Object create( NodeKey value ) throws ValueFormatException {
        return value;
    }

    @Override
    public Object create( Object value ) {
        return value;
    }

    @Override
    public Object[] create( Object[] values ) {
        return values;
    }

    @Override
    public Object create( byte[] value ) {
        return getBinaryValueFactory().create(value);
    }

    @Override
    public Object create( BinaryValue value ) throws ValueFormatException, IoException {
        return value;
    }

    @Override
    public Object create( InputStream stream ) {
        return getBinaryValueFactory().create(stream);
    }

    @Override
    protected Object[] createEmptyArray( int length ) {
        return new Object[length];
    }
}
