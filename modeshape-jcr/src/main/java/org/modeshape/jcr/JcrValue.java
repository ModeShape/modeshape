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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactories;

/**
 * ModeShape implementation of a {@link Value JCR Value}.
 */
@NotThreadSafe
final class JcrValue implements Value, org.modeshape.jcr.api.Value {

    static final JcrValue[] EMPTY_ARRAY = new JcrValue[] {};

    private final SessionCache sessionCache;
    private final ValueFactories valueFactories;
    private final int type;
    private final Object value;
    private InputStream asStream = null;

    JcrValue( ValueFactories valueFactories,
              SessionCache sessionCache,
              int type,
              Object value ) {
        assert valueFactories != null;
        assert type == PropertyType.BINARY || type == PropertyType.BOOLEAN || type == PropertyType.DATE
               || type == PropertyType.DECIMAL || type == PropertyType.DOUBLE || type == PropertyType.LONG
               || type == PropertyType.NAME || type == PropertyType.PATH || type == PropertyType.REFERENCE
               || type == PropertyType.WEAKREFERENCE || type == PropertyType.STRING || type == PropertyType.URI : "Unxpected PropertyType: "
                                                                                                                  + PropertyType.nameFromValue(type)
                                                                                                                  + " for value "
                                                                                                                  + (value == null ? "null" : ("\""
                                                                                                                                               + value + "\""));

        // Leaving this assertion out for now so that values can be created in node type sources, which are created outside
        // the context of any particular session.
        // assert sessionCache != null;
        assert value != null;

        this.valueFactories = valueFactories;
        this.sessionCache = sessionCache;
        this.type = type;
        this.value = convertToType(this.type, value instanceof JcrBinary ? ((JcrBinary)value).binary() : value);
    }

    JcrValue( ValueFactories valueFactories,
              SessionCache sessionCache,
              Value value ) throws RepositoryException {
        assert value != null;

        this.valueFactories = valueFactories;
        this.sessionCache = sessionCache;
        this.type = value.getType();
        this.value = valueToType(this.type, value);
    }

    private ValueFormatException createValueFormatException( Class<?> type ) {
        return new ValueFormatException(JcrI18n.cannotConvertValue.text(value.getClass().getSimpleName(), type.getSimpleName()));
    }

    private ValueFormatException createValueFormatException( org.modeshape.graph.property.ValueFormatException vfe ) {
        return new ValueFormatException(vfe);
    }

    /**
     * Returns a direct reference to the internal value object wrapped by this {@link JcrValue}. Useful to avoid the expense of
     * {@link #asType(int)} if the caller already knows the type of the value.
     * 
     * @return a reference to the {@link #value} field.
     */
    final Object value() {
        return value;
    }

    /**
     * Returns the session cache for the session that created this value.
     * 
     * @return the session cache for the session that created this value.
     */
    final SessionCache sessionCache() {
        return sessionCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException {
        try {
            boolean convertedValue = valueFactories.getBooleanFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(boolean.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getDate()
     */
    public Calendar getDate() throws ValueFormatException {
        try {
            Calendar convertedValue = valueFactories.getDateFactory().create(value).toCalendar();
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(Calendar.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getDecimal()
     */
    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        try {
            BigDecimal convertedValue = valueFactories.getDecimalFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(double.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getDouble()
     */
    public double getDouble() throws ValueFormatException {
        try {
            double convertedValue = valueFactories.getDoubleFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(double.class);
        }
    }

    long getLength() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            return valueFactories.getBinaryFactory().create(value).getSize();
        }
        return getString().length();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getLong()
     */
    public long getLong() throws ValueFormatException {
        try {
            long convertedValue = valueFactories.getLongFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(long.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getStream()
     */
    public InputStream getStream() throws ValueFormatException {
        try {
            if (asStream == null) {
                Binary binary = valueFactories.getBinaryFactory().create(value);
                asStream = new SelfClosingInputStream(binary);
            }
            return asStream;
        } catch (RuntimeException error) {
            throw createValueFormatException(InputStream.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.Value#getBinary()
     */
    public javax.jcr.Binary getBinary() throws RepositoryException {
        try {
            Binary binary = valueFactories.getBinaryFactory().create(value);
            return new JcrBinary(binary);
        } catch (RuntimeException error) {
            throw createValueFormatException(InputStream.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getString()
     */
    public String getString() throws ValueFormatException {
        try {
            String convertedValue = valueFactories.getStringFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(String.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getType()
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // Use the value's hash code
        return value.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof JcrValue) {
            JcrValue that = (JcrValue)obj;
            if (this.type != that.type) return false;
            try {
                switch (this.type) {
                    case PropertyType.STRING:
                        return this.getString().equals(that.getString());
                    case PropertyType.BINARY:
                        BinaryFactory binaryFactory = valueFactories.getBinaryFactory();
                        Binary thisValue = binaryFactory.create(this.value);
                        Binary thatValue = binaryFactory.create(that.value);
                        return thisValue.equals(thatValue);
                    case PropertyType.BOOLEAN:
                        return this.getBoolean() == that.getBoolean();
                    case PropertyType.DOUBLE:
                        return this.getDouble() == that.getDouble();
                    case PropertyType.LONG:
                        return this.getLong() == that.getLong();
                    case PropertyType.DECIMAL:
                        return getDecimal().equals(that.getDecimal());
                    case PropertyType.DATE:
                        DateTimeFactory dateFactory = valueFactories.getDateFactory();
                        DateTime thisDateValue = dateFactory.create(this.value);
                        DateTime thatDateValue = dateFactory.create(that.value);
                        return thisDateValue.equals(thatDateValue);
                    case PropertyType.PATH:
                        PathFactory pathFactory = valueFactories.getPathFactory();
                        Path thisPathValue = pathFactory.create(this.value);
                        Path thatPathValue = pathFactory.create(that.value);
                        return thisPathValue.equals(thatPathValue);
                    case PropertyType.NAME:
                        NameFactory nameFactory = valueFactories.getNameFactory();
                        Name thisNameValue = nameFactory.create(this.value);
                        Name thatNameValue = nameFactory.create(that.value);
                        return thisNameValue.equals(thatNameValue);
                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                        return this.getString().equals(that.getString());
                    default:
                        throw new SystemFailureException(JcrI18n.invalidPropertyType.text(this.type));
                }
            } catch (RepositoryException e) {
                return false;
            }
            // will not get here
        }
        if (obj instanceof Value) {
            Value that = (Value)obj;
            if (this.type != that.getType()) return false;
            try {
                switch (this.type) {
                    case PropertyType.STRING:
                        return this.getString().equals(that.getString());
                    case PropertyType.BINARY:
                        return IoUtil.isSame(this.getStream(), that.getBinary().getStream());
                    case PropertyType.BOOLEAN:
                        return this.getBoolean() == that.getBoolean();
                    case PropertyType.DOUBLE:
                        return this.getDouble() == that.getDouble();
                    case PropertyType.LONG:
                        return this.getLong() == that.getLong();
                    case PropertyType.DECIMAL:
                        return this.getDecimal().equals(that.getDecimal());
                    case PropertyType.DATE:
                        return this.getDate().equals(that.getDate());
                    case PropertyType.PATH:
                        return this.getString().equals(that.getString());
                    case PropertyType.NAME:
                        return this.getString().equals(that.getString());
                    case PropertyType.REFERENCE:
                        return this.getString().equals(that.getString());
                    default:
                        throw new SystemFailureException(JcrI18n.invalidPropertyType.text(this.type));
                }
            } catch (IOException e) {
                return false;
            } catch (RepositoryException e) {
                return false;
            }
            // will not get here
        }
        return false;
    }

    private JcrValue withTypeAndValue( int type,
                                       Object value ) {
        return new JcrValue(this.valueFactories, this.sessionCache, type, value);
    }

    /**
     * Returns a copy of the current {@link JcrValue} cast to the JCR type specified by the <code>type</code> argument. If the
     * value cannot be converted base don the JCR type conversion rules, a {@link ValueFormatException} will be thrown.
     * 
     * @param type the JCR type from {@link PropertyType} that the new {@link JcrValue} should have.
     * @return a new {@link JcrValue} with the given JCR type and an equivalent value.
     * @throws ValueFormatException if the value contained by this {@link JcrValue} cannot be converted to the desired type.
     * @see PropertyType
     */
    JcrValue asType( int type ) throws ValueFormatException {
        if (type == this.type) {
            return this.withTypeAndValue(this.type, this.value);
        }

        Object value = this.value;
        switch (type) {
            case PropertyType.BOOLEAN:
                // Make sure the existing value is valid per the current type.
                // This is required if we rely upon the value factories to cast correctly.
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY) {
                    throw createValueFormatException(boolean.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getBooleanFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.DATE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.LONG) {
                    throw createValueFormatException(Calendar.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getDateFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.NAME:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.PATH) {
                    throw createValueFormatException(Name.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getNameFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.PATH:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.NAME) {
                    throw createValueFormatException(Path.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getPathFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.REFERENCE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY) {
                    throw createValueFormatException(Node.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getReferenceFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.WEAKREFERENCE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY) {
                    throw createValueFormatException(Node.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getWeakReferenceFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.DOUBLE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.LONG
                    && this.type != PropertyType.DATE) {
                    throw createValueFormatException(double.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getDoubleFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.LONG:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.DATE) {
                    throw createValueFormatException(long.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getLongFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.DECIMAL:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.LONG && this.type != PropertyType.DATE) {
                    throw createValueFormatException(BigDecimal.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getDecimalFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.URI:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.URI) {
                    throw createValueFormatException(URI.class);
                }
                try {
                    return this.withTypeAndValue(type, valueFactories.getUriFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

                // Anything can be converted to these types
            case PropertyType.BINARY:
                try {
                    return this.withTypeAndValue(type, valueFactories.getBinaryFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.STRING:
                try {
                    return this.withTypeAndValue(type, valueFactories.getStringFactory().create(value));
                } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.UNDEFINED:
                return this.withTypeAndValue(this.type, this.value);

            default:
                assert false : "Unexpected JCR property type " + type;
                // This should still throw an exception even if assertions are turned off
                throw new IllegalStateException("Invalid property type " + type);
        }
    }

    protected Object valueToType( int type,
                                  Value value ) throws RepositoryException {
        switch (type) {
            case PropertyType.BOOLEAN:
                return valueFactories.getBooleanFactory().create(value.getBoolean());
            case PropertyType.DATE:
                return valueFactories.getDateFactory().create(value.getDate());
            case PropertyType.NAME:
                return valueFactories.getNameFactory().create(value.getString());
            case PropertyType.PATH:
                return valueFactories.getPathFactory().create(value.getString());
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return valueFactories.getReferenceFactory().create(value.getString());
            case PropertyType.DOUBLE:
                return valueFactories.getDoubleFactory().create(value.getDouble());
            case PropertyType.LONG:
                return valueFactories.getLongFactory().create(value.getLong());
            case PropertyType.DECIMAL:
                return valueFactories.getDecimalFactory().create(value.getDecimal());
            case PropertyType.URI:
                return valueFactories.getUriFactory().create(value.getString());
            case PropertyType.BINARY:
                return valueFactories.getBinaryFactory().create(value.getBinary());
            case PropertyType.STRING:
                return valueFactories.getStringFactory().create(value.getString());
            case PropertyType.UNDEFINED:
                return value.getString();

            default:
                assert false : "Unexpected JCR property type " + type;
                // This should still throw an exception even if assertions are turned off
                throw new IllegalStateException("Invalid property type " + type);
        }
    }

    protected Object convertToType( int type,
                                    Object value ) {
        if (value == null) return null;
        switch (type) {
            case PropertyType.BOOLEAN:
                return valueFactories.getBooleanFactory().create(value);
            case PropertyType.DATE:
                return valueFactories.getDateFactory().create(value);
            case PropertyType.NAME:
                return valueFactories.getNameFactory().create(value);
            case PropertyType.PATH:
                return valueFactories.getPathFactory().create(value);
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return valueFactories.getReferenceFactory().create(value);
            case PropertyType.DOUBLE:
                return valueFactories.getDoubleFactory().create(value);
            case PropertyType.LONG:
                return valueFactories.getLongFactory().create(value);
            case PropertyType.DECIMAL:
                return valueFactories.getDecimalFactory().create(value);
            case PropertyType.URI:
                return valueFactories.getUriFactory().create(value);
            case PropertyType.BINARY:
                return valueFactories.getBinaryFactory().create(value);
            case PropertyType.STRING:
                return valueFactories.getStringFactory().create(value);
            case PropertyType.UNDEFINED:
                return value;

            default:
                assert false : "Unexpected JCR property type " + type;
                // This should still throw an exception even if assertions are turned off
                throw new IllegalStateException("Invalid property type " + type);
        }
    }

    @Override
    public String toString() {
        return (value == null ? "null" : value.toString()) + " (" + PropertyType.nameFromValue(type) + ")";
    }
}
