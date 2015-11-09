/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jdbc.rest;

import static org.modeshape.jdbc.rest.JSONHelper.valueFrom;
import static org.modeshape.jdbc.rest.JSONHelper.valuesFrom;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.version.OnParentVersionAction;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.DateTimeUtil;
import org.modeshape.common.util.HashCode;
import org.modeshape.jdbc.JdbcI18n;

/**
 * A {@link javax.jcr.nodetype.PropertyDefinition} implementation for the Modeshape client.
 */
@Immutable
public class PropertyDefinition extends ItemDefinition implements javax.jcr.nodetype.PropertyDefinition {
    private static final Map<String, Integer> PROPERTY_TYPES_BY_LOWERCASE_NAME;

    private final Id id;
    private final List<String> queryOps;
    private final List<String> defaultValues;
    private final List<String> valueConstraints;
    private final boolean isFullTextSearchable;
    private final boolean isQueryOrderable;

    static {
        Map<String, Integer> types = new HashMap<>();
        registerType(PropertyType.BINARY, types);
        registerType(PropertyType.BOOLEAN, types);
        registerType(PropertyType.DATE, types);
        registerType(PropertyType.DECIMAL, types);
        registerType(PropertyType.DOUBLE, types);
        registerType(PropertyType.LONG, types);
        registerType(PropertyType.NAME, types);
        registerType(PropertyType.PATH, types);
        registerType(PropertyType.REFERENCE, types);
        registerType(PropertyType.STRING, types);
        registerType(PropertyType.UNDEFINED, types);
        registerType(PropertyType.URI, types);
        registerType(PropertyType.WEAKREFERENCE, types);
        PROPERTY_TYPES_BY_LOWERCASE_NAME = Collections.unmodifiableMap(types);
    }

    private static void registerType( int propertyType,
                                      Map<String, Integer> types ) {
        String name = PropertyType.nameFromValue(propertyType);
        types.put(name.toLowerCase(), propertyType);
    }

    protected PropertyDefinition( String declaringNodeTypeName,
                                  JSONObject json,
                                  NodeTypes nodeTypes ) {
        super(declaringNodeTypeName, json, nodeTypes);
        String name = valueFrom(json, "jcr:name", "*");
        boolean isMultiple = valueFrom(json, "jcr:multiple", false);
        int requiredType = typeValueFrom(json, "jcr:requiredType", PropertyType.UNDEFINED);
        this.id = new Id(name, isMultiple, requiredType);
        this.isFullTextSearchable = valueFrom(json, "jcr:isFullTextSearchable", false);
        this.isQueryOrderable = valueFrom(json, "jcr:isQueryOrderable", false);
        this.queryOps = valuesFrom(json, "jcr:availableQueryOperators");
        this.defaultValues = valuesFrom(json, "jcr:defaultValues");
        this.valueConstraints = valuesFrom(json, "jcr:valueConstraints");
    }

    protected Id id() {
        return id;
    }

    @Override
    public String getName() {
        return id.name;
    }

    @Override
    public String[] getAvailableQueryOperators() {
        return queryOps.toArray(new String[queryOps.size()]);
    }

    @Override
    public Value[] getDefaultValues() {
        if (defaultValues.isEmpty()) return new Value[0];
        int numValues = defaultValues.size();
        int i = 0;
        Value[] result = new Value[numValues];
        for (String value : defaultValues) {
            result[i++] = new StringValue(value);
        }
        return result;
    }

    @Override
    public int getRequiredType() {
        return id.requiredType;
    }

    @Override
    public String[] getValueConstraints() {
        return valueConstraints.toArray(new String[valueConstraints.size()]);
    }

    @Override
    public boolean isFullTextSearchable() {
        return isFullTextSearchable;
    }

    @Override
    public boolean isMultiple() {
        return id.isMultiple;
    }

    @Override
    public boolean isQueryOrderable() {
        return isQueryOrderable;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyDefinition) {
            PropertyDefinition that = (PropertyDefinition)obj;
            return this.id.equals(that.id);
        }
        return false;
    }

    protected int typeValueFrom( JSONObject json,
                                 String name,
                                 int defaultType ) {
        try {
            if (!json.has(name)) return defaultType;
            String typeName = json.getString(name);
            Integer result = PROPERTY_TYPES_BY_LOWERCASE_NAME.get(typeName.toLowerCase());
            return result != null ? result : defaultType;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

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

    protected final class StringValue implements Value {

        protected final String value;

        protected StringValue( String value ) {
            this.value = value;
            assert this.value != null;
        }

        @Override
        public boolean getBoolean() {
            return Boolean.parseBoolean(value.trim());
        }

        @Override
        public Calendar getDate() throws ValueFormatException {
            return valueToCalendar(null);
        }

        private Calendar valueToCalendar(String zoneId) throws ValueFormatException {
            try {
                ZonedDateTime zonedDateTime = zoneId == null ?
                                              DateTimeUtil.jodaParse(value) :
                                              DateTimeUtil.jodaParse(value).withZoneSameInstant(ZoneId.of(zoneId));
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(zonedDateTime.toInstant().toEpochMilli());
                return calendar;
            } catch (DateTimeParseException e) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.LONG);
                throw new ValueFormatException(JdbcI18n.unableToConvertValue.text(value, from, to), e);
            }
        }

        public Calendar getDateInUtc() throws ValueFormatException {
            return valueToCalendar("UTC");
        }

        @Override
        public BigDecimal getDecimal() throws ValueFormatException {
            try {
                if (getRequiredType() == PropertyType.DATE) {
                    return new BigDecimal(getDateInUtc().getTime().getTime());
                }
                return new BigDecimal(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.DECIMAL);
                throw new ValueFormatException(JdbcI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        @Override
        public double getDouble() throws ValueFormatException {
            try {
                if (getRequiredType() == PropertyType.DATE) {
                    return getDateInUtc().getTime().getTime();
                }
                return Double.parseDouble(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.DOUBLE);
                throw new ValueFormatException(JdbcI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        @Override
        public long getLong() throws ValueFormatException {
            try {
                if (getRequiredType() == PropertyType.DATE) {
                    return getDateInUtc().getTime().getTime();
                }
                return Long.parseLong(value);
            } catch (NumberFormatException t) {
                String from = PropertyType.nameFromValue(getType());
                String to = PropertyType.nameFromValue(PropertyType.LONG);
                throw new ValueFormatException(JdbcI18n.unableToConvertValue.text(value, from, to), t);
            }
        }

        @Override
        public InputStream getStream() throws RepositoryException {
            return getBinary().getStream();
        }

        @Override
        public String getString() {
            return value;
        }

        @Override
        public int getType() {
            int type = getRequiredType();
            return type == PropertyType.UNDEFINED ? PropertyType.STRING : type;
        }

        @Override
        public Binary getBinary() {
            return new Binary() {
                private byte[] bytes = value.getBytes();

                @Override
                public void dispose() {
                    // do nothing
                    this.bytes = null;
                }

                @Override
                public long getSize() {
                    return bytes.length;
                }

                @Override
                public InputStream getStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public int read( byte[] b,
                                 long position ) throws IOException {
                    if (getSize() <= position) return -1;
                    try (InputStream stream = getStream()) {
                        // Read/skip the next 'position' bytes ...
                        long skip = position;
                        while (skip > 0) {
                            long skipped = stream.skip(skip);
                            if (skipped <= 0) return -1;
                            skip -= skipped;
                        }
                        return stream.read(b);
                    }
                }
            };
        }

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

        @Override
        public int hashCode() {
            return HashCode.compute(isMultiple, requiredType, name);
        }

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

        @Override
        public String toString() {
            return name + "(" + PropertyType.nameFromValue(requiredType) + ")" + (isMultiple ? '*' : '1');
        }
    }

}
