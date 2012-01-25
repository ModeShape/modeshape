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
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * ModeShape implementation of a {@link Value JCR Value}.
 */
@NotThreadSafe
final class JcrValue implements javax.jcr.Value {

    static final JcrValue[] EMPTY_ARRAY = new JcrValue[] {};

    private final ValueFactories factories;
    private final int type;
    private final Object value;
    private InputStream asStream = null;

    JcrValue( ValueFactories factories,
              int type,
              Object value ) {
        assert factories != null;
        this.factories = factories;

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

        this.type = type;
        this.value = convertToType(this.type, value);
    }

    JcrValue( ValueFactories factories,
              Value value ) throws RepositoryException {
        assert factories != null;
        assert value != null;
        this.factories = factories;
        this.type = value.getType();
        this.value = valueToType(this.type, value);
    }

    private ValueFormatException createValueFormatException( Class<?> type ) {
        return new ValueFormatException(JcrI18n.cannotConvertValue.text(value.getClass().getSimpleName(), type.getSimpleName()));
    }

    private ValueFormatException createValueFormatException( org.modeshape.jcr.value.ValueFormatException vfe ) {
        return new ValueFormatException(vfe);
    }

    final ValueFactories factories() {
        return factories;
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

    @Override
    public boolean getBoolean() throws ValueFormatException {
        try {
            boolean convertedValue = factories().getBooleanFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(boolean.class);
        }
    }

    @Override
    public Calendar getDate() throws ValueFormatException {
        if (value == null) return null;
        try {
            Calendar convertedValue = factories().getDateFactory().create(value).toCalendar();
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(Calendar.class);
        }
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        if (value == null) return null;
        try {
            BigDecimal convertedValue = factories().getDecimalFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(double.class);
        }
    }

    @Override
    public double getDouble() throws ValueFormatException {
        try {
            double convertedValue = factories().getDoubleFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(double.class);
        }
    }

    long getLength() throws RepositoryException {
        if (value == null) return 0L;
        if (type == PropertyType.BINARY) {
            return factories().getBinaryFactory().create(value).getSize();
        }
        return getString().length();
    }

    @Override
    public long getLong() throws ValueFormatException {
        try {
            long convertedValue = factories().getLongFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(long.class);
        }
    }

    @Override
    public InputStream getStream() throws ValueFormatException {
        if (value == null) return null;
        try {
            if (asStream == null) {
                Binary binary = factories().getBinaryFactory().create(value);
                asStream = new SelfClosingInputStream(binary);
            }
            return asStream;
        } catch (RuntimeException error) {
            throw createValueFormatException(InputStream.class);
        }
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        if (value == null) return null;
        try {
            Binary binary = factories().getBinaryFactory().create(value);
            return binary;
        } catch (RuntimeException error) {
            throw createValueFormatException(InputStream.class);
        }
    }

    @Override
    public String getString() throws ValueFormatException {
        try {
            String convertedValue = factories().getStringFactory().create(value);
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(String.class);
        }
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int hashCode() {
        // Use the value's hash code
        return value.hashCode();
    }

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
                        BinaryFactory binaryFactory = factories().getBinaryFactory();
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
                        DateTimeFactory dateFactory = factories().getDateFactory();
                        DateTime thisDateValue = dateFactory.create(this.value);
                        DateTime thatDateValue = dateFactory.create(that.value);
                        return thisDateValue.equals(thatDateValue);
                    case PropertyType.PATH:
                        PathFactory pathFactory = factories().getPathFactory();
                        Path thisPathValue = pathFactory.create(this.value);
                        Path thatPathValue = pathFactory.create(that.value);
                        return thisPathValue.equals(thatPathValue);
                    case PropertyType.NAME:
                        NameFactory nameFactory = factories().getNameFactory();
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
        return new JcrValue(factories, type, value);
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
                    return this.withTypeAndValue(type, factories().getBooleanFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.DATE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.LONG) {
                    throw createValueFormatException(Calendar.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getDateFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.NAME:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.PATH) {
                    throw createValueFormatException(Name.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getNameFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.PATH:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.NAME) {
                    throw createValueFormatException(Path.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getPathFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

            case PropertyType.REFERENCE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY
                    && this.type != PropertyType.WEAKREFERENCE) {
                    throw createValueFormatException(Node.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getReferenceFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.WEAKREFERENCE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.REFERENCE) {
                    throw createValueFormatException(Node.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getWeakReferenceFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.DOUBLE:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.LONG
                    && this.type != PropertyType.DATE) {
                    throw createValueFormatException(double.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getDoubleFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.LONG:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.DATE) {
                    throw createValueFormatException(long.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getLongFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.DECIMAL:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.DOUBLE
                    && this.type != PropertyType.LONG && this.type != PropertyType.DATE) {
                    throw createValueFormatException(BigDecimal.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getDecimalFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.URI:
                if (this.type != PropertyType.STRING && this.type != PropertyType.BINARY && this.type != PropertyType.URI) {
                    throw createValueFormatException(URI.class);
                }
                try {
                    return this.withTypeAndValue(type, factories().getUriFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

                // Anything can be converted to these types
            case PropertyType.BINARY:
                try {
                    return this.withTypeAndValue(type, factories().getBinaryFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.STRING:
                try {
                    return this.withTypeAndValue(type, factories().getStringFactory().create(value));
                } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
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
                return factories().getBooleanFactory().create(value.getBoolean());
            case PropertyType.DATE:
                return factories().getDateFactory().create(value.getDate());
            case PropertyType.NAME:
                return factories().getNameFactory().create(value.getString());
            case PropertyType.PATH:
                return factories().getPathFactory().create(value.getString());
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return factories().getReferenceFactory().create(value.getString());
            case PropertyType.DOUBLE:
                return factories().getDoubleFactory().create(value.getDouble());
            case PropertyType.LONG:
                return factories().getLongFactory().create(value.getLong());
            case PropertyType.DECIMAL:
                return factories().getDecimalFactory().create(value.getDecimal());
            case PropertyType.URI:
                return factories().getUriFactory().create(value.getString());
            case PropertyType.BINARY:
                return factories().getBinaryFactory().create(value.getBinary());
            case PropertyType.STRING:
                return factories().getStringFactory().create(value.getString());
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
                return factories().getBooleanFactory().create(value);
            case PropertyType.DATE:
                return factories().getDateFactory().create(value);
            case PropertyType.NAME:
                return factories().getNameFactory().create(value);
            case PropertyType.PATH:
                return factories().getPathFactory().create(value);
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return factories().getReferenceFactory().create(value);
            case PropertyType.DOUBLE:
                return factories().getDoubleFactory().create(value);
            case PropertyType.LONG:
                return factories().getLongFactory().create(value);
            case PropertyType.DECIMAL:
                return factories().getDecimalFactory().create(value);
            case PropertyType.URI:
                return factories().getUriFactory().create(value);
            case PropertyType.BINARY:
                return factories().getBinaryFactory().create(value);
            case PropertyType.STRING:
                return factories().getStringFactory().create(value);
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
