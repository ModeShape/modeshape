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
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.IoException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#URI} values.
 */
@Immutable
public class UriValueFactory extends AbstractValueFactory<URI> {

    private final NamespaceRegistry namespaceRegistry;

    public UriValueFactory( NamespaceRegistry namespaceRegistry,
                            TextDecoder decoder,
                            ValueFactory<String> stringValueFactory ) {
        super(PropertyType.URI, decoder, stringValueFactory);
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.namespaceRegistry = namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public URI create( String value ) {
        if (value == null) return null;
        try {
            return new URI(value);
        } catch (URISyntaxException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              URI.class.getSimpleName(),
                                                                              value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URI create( String value,
                       TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.DateTime)
     */
    public URI create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Name value ) {
        if (value == null) return null;
        return create("./" + value.getString(this.namespaceRegistry));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Path value ) {
        if (value == null) return null;
        if (value.isAbsolute()) {
            return create("/" + value.getString(this.namespaceRegistry));
        }
        return create("./" + value.getString(this.namespaceRegistry));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Path.Segment value ) {
        if (value == null) return null;
        return create("./" + value.getString(this.namespaceRegistry));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(java.util.UUID)
     */
    public URI create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( URI value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public URI create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.Binary)
     */
    public URI create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( InputStream stream,
                       long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Reader reader,
                       long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URI[] createEmptyArray( int length ) {
        return new URI[length];
    }

}
