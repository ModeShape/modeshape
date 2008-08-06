/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.impl;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#OBJECT} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class ObjectValueFactory extends AbstractValueFactory<Object> {

    private final ValueFactory<Binary> binaryValueFactory;

    public ObjectValueFactory( TextDecoder decoder,
                               ValueFactory<String> stringValueFactory,
                               ValueFactory<Binary> binaryValueFactory ) {
        super(PropertyType.OBJECT, decoder, stringValueFactory);
        ArgCheck.isNotNull(binaryValueFactory, "binaryValueFactory");
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
     * @see org.jboss.dna.spi.graph.ValueFactory#create(java.util.UUID)
     */
    public Object create( UUID value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Object create( byte[] value ) {
        return getBinaryValueFactory().create(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( InputStream stream,
                          int approximateLength ) {
        return getBinaryValueFactory().create(stream, approximateLength);
    }

    /**
     * {@inheritDoc}
     */
    public Object create( Reader reader,
                          int approximateLength ) {
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
