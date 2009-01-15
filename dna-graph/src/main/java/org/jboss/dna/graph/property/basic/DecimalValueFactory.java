/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.property.basic;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.IoException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#DECIMAL} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class DecimalValueFactory extends AbstractValueFactory<BigDecimal> {

    public DecimalValueFactory( TextDecoder decoder,
                                ValueFactory<String> stringValueFactory ) {
        super(PropertyType.DECIMAL, decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( String value ) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                            BigDecimal.class.getSimpleName(),
                                                                            value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( String value,
                              TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value.trim()));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( int value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( long value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Boolean.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( float value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( double value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( BigDecimal value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Calendar value ) {
        if (value == null) return null;
        return create(value.getTimeInMillis());
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Date value ) {
        if (value == null) return null;
        return create(value.getTime());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.DateTime)
     */
    public BigDecimal create( DateTime value ) throws ValueFormatException {
        if (value == null) return null;
        return create(value.getMilliseconds());
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Name.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Path.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                        Reference.class.getSimpleName(),
                                                                        value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  URI.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(java.util.UUID)
     */
    public BigDecimal create( UUID value ) throws IoException {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  UUID.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.Binary)
     */
    public BigDecimal create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( InputStream stream,
                              long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Reader reader,
                              long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BigDecimal[] createEmptyArray( int length ) {
        return new BigDecimal[length];
    }

}
