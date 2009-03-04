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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.ValueFactories;

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
               || type == PropertyType.PATH || type == PropertyType.REFERENCE || type == PropertyType.STRING
               || type == PropertyType.UNDEFINED;
        assert value != null;
        this.valueFactories = valueFactories;
        this.type = type;
        this.value = value;
    }

    private State state = State.NEVER_CONSUMED;

    ValueFormatException createValueFormatException( Class<?> type ) {
        return new ValueFormatException(JcrI18n.cannotConvertValue.text(value.getClass().getSimpleName(), type.getSimpleName()));
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
