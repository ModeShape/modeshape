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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.ValueFormatException;

/**
 * Abstract {@link ValueFactory}.
 * @author Randall Hauch
 * @param <T> the property type
 */
@Immutable
public abstract class AbstractValueFactory<T> implements ValueFactory<T> {

    private final TextEncoder encoder;
    private final PropertyType propertyType;
    private final ValueFactory<String> stringValueFactory;

    protected AbstractValueFactory( PropertyType type, TextEncoder encoder, ValueFactory<String> stringValueFactory ) {
        ArgCheck.isNotNull(type, "type");
        this.propertyType = type;
        this.encoder = encoder != null ? encoder : DEFAULT_ENCODER;
        this.stringValueFactory = stringValueFactory;
    }

    /**
     * @return stringValueFactory
     */
    protected ValueFactory<String> getStringValueFactory() {
        return this.stringValueFactory;
    }

    /**
     * Get the text encoder/decoder.
     * @return encoder/decoder
     */
    public TextEncoder getEncoder() {
        return this.encoder;
    }

    /**
     * Utility method to obtain either the supplied encoder (if not null) or this factory's {@link #getEncoder() encoder}.
     * @param encoder the encoder, which may be null if this factory's {@link #getEncoder() is to be used}
     * @return the encoder; never null
     */
    protected TextEncoder getEncoder( TextEncoder encoder ) {
        return encoder != null ? encoder : this.getEncoder();
    }

    /**
     * {@inheritDoc}
     */
    public PropertyType getPropertyType() {
        return propertyType;
    }

    /**
     * {@inheritDoc}
     */
    public T create( Object value ) throws ValueFormatException {
        if (value == null) return null;
        if (value instanceof String) return create((String)value);
        if (value instanceof Integer) return create(((Integer)value).intValue());
        if (value instanceof Long) return create(((Long)value).longValue());
        if (value instanceof Double) return create(((Double)value).doubleValue());
        if (value instanceof Float) return create(((Float)value).floatValue());
        if (value instanceof Boolean) return create(((Boolean)value).booleanValue());
        if (value instanceof BigDecimal) return create((BigDecimal)value);
        if (value instanceof Calendar) return create((Calendar)value);
        if (value instanceof Date) return create((Date)value);
        if (value instanceof Name) return create((Name)value);
        if (value instanceof Path) return create((Path)value);
        if (value instanceof Reference) return create((Reference)value);
        if (value instanceof URI) return create((URI)value);
        if (value instanceof byte[]) return create((byte[])value);
        if (value instanceof InputStream) {
            try {
                return create((InputStream)value, 0);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }
        if (value instanceof Reader) {
            try {
                return create((Reader)value, 0);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }
        return create(value.toString());
    }

}
