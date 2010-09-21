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
package org.modeshape.web.jcr.rest.client.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.Immutable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * An immutable representation of a JCR PropertyDefinition.
 */
@Immutable
public class PropertyDefinition extends ItemDefinition implements javax.jcr.nodetype.PropertyDefinition {

    public static Calendar parseDate( String dateString ) throws IllegalArgumentException {
        DateTime result = new DateTime(dateString);
        return result.toCalendar(null);
    }

    private final Id id;
    private final List<String> queryOps;
    private final List<String> defaultValues;
    private final List<String> valueConstraints;
    private final boolean isFullTextSearchable;
    private final boolean isQueryOrderable;

    public PropertyDefinition( String declaringNodeTypeName,
                               String name,
                               int requiredType,
                               boolean isAutoCreated,
                               boolean isMandatory,
                               boolean isProtected,
                               boolean isFullTextSearchable,
                               boolean isMultiple,
                               boolean isQueryOrderable,
                               int onParentVersion,
                               List<String> defaultValues,
                               List<String> valueConstraints,
                               List<String> availableQueryOperations,
                               Map<String, NodeType> nodeTypes ) {
        super(declaringNodeTypeName, isAutoCreated, isMandatory, isProtected, onParentVersion, nodeTypes);
        this.id = new Id(name, isMultiple, requiredType);
        this.isFullTextSearchable = isFullTextSearchable;
        this.isQueryOrderable = isQueryOrderable;
        this.defaultValues = defaultValues != null ? defaultValues : Collections.<String>emptyList();
        this.valueConstraints = valueConstraints != null ? valueConstraints : Collections.<String>emptyList();
        this.queryOps = availableQueryOperations != null ? availableQueryOperations : Collections.<String>emptyList();
    }

    /**
     * @return id
     */
    protected Id id() {
        return id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    @Override
    public String getName() {
        return id.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
     */
    @Override
    public String[] getAvailableQueryOperators() {
        return toArray(queryOps);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    @Override
    public Value[] getDefaultValues() {
        if (defaultValues == null || defaultValues.isEmpty()) return new Value[0];
        int numValues = defaultValues.size();
        int i = 0;
        Value[] result = new Value[numValues];
        for (String value : defaultValues) {
            result[i++] = new StringValue(value);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
     */
    @Override
    public int getRequiredType() {
        return id.requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
     */
    @Override
    public String[] getValueConstraints() {
        return toArray(valueConstraints);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable()
     */
    @Override
    public boolean isFullTextSearchable() {
        return isFullTextSearchable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return id.isMultiple;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isQueryOrderable()
     */
    @Override
    public boolean isQueryOrderable() {
        return isQueryOrderable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyDefinition) {
            PropertyDefinition that = (PropertyDefinition)obj;
            return this.id.equals(that.id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" - ");
        sb.append(id.name);
        sb.append(" (");
        sb.append(PropertyType.nameFromValue(id.requiredType));
        sb.append(')');
        if (getDefaultValues().length != 0) {
            sb.append(" = ");
            boolean first = true;
            for (Value defaultValue : getDefaultValues()) {
                if (defaultValue == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append(defaultValue);
            }
        }
        if (isAutoCreated()) sb.append(" autocreated");
        if (isMandatory()) sb.append(" mandatory");
        if (!isFullTextSearchable()) sb.append(" nofulltext");
        if (!isQueryOrderable()) sb.append(" noqueryorder");
        if (isMultiple()) sb.append(" multiple");
        if (isProtected()) sb.append(" protected");
        sb.append(' ').append(OnParentVersionAction.nameFromValue(getOnParentVersion()));
        if (getAvailableQueryOperators().length != 0) {
            sb.append(" queryops ");
            boolean first = true;
            for (String constraint : getAvailableQueryOperators()) {
                if (constraint == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append('\'');
                sb.append(constraint);
                sb.append('\'');
            }
        }
        if (getValueConstraints().length != 0) {
            sb.append(" < ");
            boolean first = true;
            for (String constraint : getValueConstraints()) {
                if (constraint == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append(constraint);
            }
        }
        return sb.toString();
    }

    protected class StringValue implements Value {

        private final String value;

        protected StringValue( String value ) {
            this.value = value;
            assert this.value != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getBoolean()
         */
        @Override
        public boolean getBoolean() throws ValueFormatException, RepositoryException {
            if (value.equals(Boolean.TRUE.toString())) return true;
            if (value.equals(Boolean.FALSE.toString())) return true;
            String lower = value.toLowerCase();
            if (lower.equals(Boolean.TRUE.toString())) return true;
            if (lower.equals(Boolean.FALSE.toString())) return true;
            throw new ValueFormatException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getDate()
         */
        @Override
        public Calendar getDate() throws ValueFormatException, RepositoryException {
            try {
                return parseDate(value);
            } catch (IllegalArgumentException e) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.LONG);
                throw new ValueFormatException(RestClientI18n.unableToConvertValue.text(value, from, to), e);
            }
        }

        public Calendar getDateInUtc() throws ValueFormatException, RepositoryException {
            try {
                DateTime result = new DateTime(value);
                DateTimeZone utc = DateTimeZone.forID("UTC");
                if (!result.getZone().equals(utc)) {
                    result = result.withZone(utc);
                }
                return result.toCalendar(null);
            } catch (IllegalArgumentException e) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.LONG);
                throw new ValueFormatException(RestClientI18n.unableToConvertValue.text(value, from, to), e);
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
                if (getRequiredType() == PropertyType.DATE) {
                    return new BigDecimal(getDateInUtc().getTime().getTime());
                }
                return new BigDecimal(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.DECIMAL);
                throw new ValueFormatException(RestClientI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getDouble()
         */
        @Override
        public double getDouble() throws ValueFormatException, RepositoryException {
            try {
                if (getRequiredType() == PropertyType.DATE) {
                    return getDateInUtc().getTime().getTime();
                }
                return Double.parseDouble(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.DOUBLE);
                throw new ValueFormatException(RestClientI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getLong()
         */
        @Override
        public long getLong() throws ValueFormatException, RepositoryException {
            try {
                if (getRequiredType() == PropertyType.DATE) {
                    return getDateInUtc().getTime().getTime();
                }
                return Long.parseLong(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.LONG);
                throw new ValueFormatException(RestClientI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getStream()
         */
        @Override
        public InputStream getStream() throws RepositoryException {
            return getBinary().getStream();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getString()
         */
        @Override
        public String getString() {
            return value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getType()
         */
        @Override
        public int getType() {
            int type = getRequiredType();
            return type == PropertyType.UNDEFINED ? PropertyType.STRING : type;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Value#getBinary()
         */
        @Override
        public Binary getBinary() {
            final byte[] bytes = value.getBytes();
            return new Binary() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see javax.jcr.Binary#dispose()
                 */
                @Override
                public void dispose() {
                    // do nothing
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see javax.jcr.Binary#getSize()
                 */
                @Override
                public long getSize() {
                    return bytes.length;
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see javax.jcr.Binary#getStream()
                 */
                @Override
                public InputStream getStream() {
                    return new ByteArrayInputStream(bytes);
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see javax.jcr.Binary#read(byte[], long)
                 */
                @Override
                public int read( byte[] b,
                                 long position ) throws IOException {
                    if (getSize() <= position) return -1;
                    InputStream stream = null;
                    IOException error = null;
                    try {
                        stream = getStream();
                        // Read/skip the next 'position' bytes ...
                        long skip = position;
                        while (skip > 0) {
                            long skipped = stream.skip(skip);
                            if (skipped <= 0) return -1;
                            skip -= skipped;
                        }
                        return stream.read(b);
                    } catch (IOException e) {
                        error = e;
                        throw e;
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (RuntimeException t) {
                                // Only throw if we've not already thrown an exception ...
                                if (error == null) throw t;
                            } catch (IOException t) {
                                // Only throw if we've not already thrown an exception ...
                                if (error == null) throw t;
                            }
                        }
                    }
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    protected static class Id {
        protected final String name;
        protected final boolean isMultiple;
        protected final int requiredType;

        protected Id( String name,
                      boolean isMultiple,
                      int requiredType ) {
            this.name = name;
            this.isMultiple = isMultiple;
            this.requiredType = requiredType;
            assert this.name != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Id) {
                Id that = (Id)obj;
                if (this.isMultiple != that.isMultiple) return false;
                if (this.requiredType != that.requiredType) return false;
                if (!this.name.equals(that.name)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return name + "(" + PropertyType.nameFromValue(requiredType) + ")" + (isMultiple ? '*' : '1');
        }
    }

}
