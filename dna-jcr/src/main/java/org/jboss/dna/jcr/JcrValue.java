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
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.Path.Segment;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrValue implements Value {

    private final ValueFactories valueFactories;
    private final int type;
    private final Object value;

    JcrValue( ValueFactories valueFactories,
              int type,
              Object value ) {
        assert valueFactories != null;
        assert type == PropertyType.BINARY || type == PropertyType.BOOLEAN || type == PropertyType.DATE
               || type == PropertyType.DOUBLE || type == PropertyType.LONG || type == PropertyType.NAME
               || type == PropertyType.PATH || type == PropertyType.REFERENCE || type == PropertyType.STRING;
        assert value != null;
        this.valueFactories = valueFactories;
        this.type = type;
        this.value = value;
    }

    private State state = State.NEVER_CONSUMED;

    private ValueFormatException createValueFormatException( Class<?> type ) {
        return new ValueFormatException(JcrI18n.cannotConvertValue.text(value.getClass().getSimpleName(), type.getSimpleName()));
    }

    private ValueFormatException createValueFormatException( org.jboss.dna.graph.property.ValueFormatException vfe ) {
        return new ValueFormatException(vfe);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException {
        nonInputStreamConsumed();
        try {
            boolean convertedValue = valueFactories.getBooleanFactory().create(value);
            state = State.NON_INPUT_STREAM_CONSUMED;
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
        nonInputStreamConsumed();
        try {
            Calendar convertedValue = valueFactories.getDateFactory().create(value).toCalendar();
            state = State.NON_INPUT_STREAM_CONSUMED;
            return convertedValue;
        } catch (RuntimeException error) {
            throw createValueFormatException(Calendar.class);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Value#getDouble()
     */
    public double getDouble() throws ValueFormatException {
        nonInputStreamConsumed();
        try {
            double convertedValue = valueFactories.getDoubleFactory().create(value);
            state = State.NON_INPUT_STREAM_CONSUMED;
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
        nonInputStreamConsumed();
        try {
            long convertedValue = valueFactories.getLongFactory().create(value);
            state = State.NON_INPUT_STREAM_CONSUMED;
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
        if (state == State.NON_INPUT_STREAM_CONSUMED) {
            throw new IllegalStateException(JcrI18n.nonInputStreamConsumed.text());
        }
        try {
            Binary binary = valueFactories.getBinaryFactory().create(value);
            InputStream convertedValue = new SelfClosingInputStream(binary);
            state = State.INPUT_STREAM_CONSUMED;
            return convertedValue;
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
        nonInputStreamConsumed();
        try {
            String convertedValue = valueFactories.getStringFactory().create(value);
            state = State.NON_INPUT_STREAM_CONSUMED;
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
            return new JcrValue(this.valueFactories, this.type, this.value);
        }

        switch (type) {
            case PropertyType.BOOLEAN:
                try {
                    if (this.type == PropertyType.STRING || this.type == PropertyType.BINARY) {
                        return new JcrValue(this.valueFactories, type, valueFactories.getBooleanFactory().create(value));
                    }
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
                throw createValueFormatException(boolean.class);

            case PropertyType.DATE:
                try {
                    if (this.type == PropertyType.DOUBLE || this.type == PropertyType.LONG || this.type == PropertyType.STRING
                        || this.type == PropertyType.BINARY) {
                        return new JcrValue(this.valueFactories, type, valueFactories.getDateFactory().create(value));
                    }
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

                throw createValueFormatException(Calendar.class);

            case PropertyType.NAME:
                try {
                    if (this.type == PropertyType.STRING) {
                        return new JcrValue(this.valueFactories, type, this.valueFactories.getNameFactory().create(value));
                    }

                    String valueAsString = this.valueFactories.getStringFactory().create(value);
                    if (this.type == PropertyType.BINARY) {
                        return new JcrValue(this.valueFactories, type, this.valueFactories.getNameFactory().create(valueAsString));

                    }

                    if (this.type == PropertyType.PATH) {
                        Path path = valueFactories.getPathFactory().create(valueAsString);

                        Segment[] segments = path.getSegmentsArray();
                        if (!path.isAbsolute() && segments.length == 1 && !segments[0].hasIndex()) {
                            return new JcrValue(this.valueFactories, type, this.valueFactories.getNameFactory()
                                                                                              .create(valueAsString));

                        }
                    }
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }

                throw createValueFormatException(Name.class);

            case PropertyType.PATH:
                try {
                    if (this.type == PropertyType.STRING) {
                        return new JcrValue(this.valueFactories, type, this.valueFactories.getPathFactory().create(value));
                    }
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
                throw createValueFormatException(Path.class);

                // Nothing can be converted to these types (except themselves)
            case PropertyType.REFERENCE:
                throw createValueFormatException(Node.class);
            case PropertyType.DOUBLE:
                throw createValueFormatException(double.class);
            case PropertyType.LONG:
                throw createValueFormatException(long.class);

                // Anything can be converted to these types
            case PropertyType.BINARY:
                try {
                    return new JcrValue(this.valueFactories, type, valueFactories.getBinaryFactory().create(value));
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.STRING:
                try {
                    return new JcrValue(this.valueFactories, type, valueFactories.getStringFactory().create(value));
                } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                    throw createValueFormatException(vfe);
                }
            case PropertyType.UNDEFINED:
                return new JcrValue(this.valueFactories, this.type, this.value);
            default:
                assert false : "Unexpected JCR property type " + type;
                // This should still throw an exception even if assertions are turned off
                throw new IllegalStateException("Invalid property type " + type);
        }
    }

    void nonInputStreamConsumed() {
        if (state == State.INPUT_STREAM_CONSUMED) {
            throw new IllegalStateException(JcrI18n.inputStreamConsumed.text());
        }
    }

    private enum State {
        NEVER_CONSUMED,
        INPUT_STREAM_CONSUMED,
        NON_INPUT_STREAM_CONSUMED
    }
}
