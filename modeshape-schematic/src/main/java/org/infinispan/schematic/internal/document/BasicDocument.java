/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.internal.document;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.internal.schema.DocumentTransformer.PropertiesTransformer;
import org.infinispan.schematic.internal.schema.DocumentTransformer.SystemPropertiesTransformer;

@SerializeWith( DocumentExternalizer.class )
public class BasicDocument extends LinkedHashMap<String, Object> implements MutableDocument {

    private static final long serialVersionUID = 1L;

    private transient Map<String, Object> unmodifiableView;

    public BasicDocument() {
        super();
    }

    public BasicDocument( int initialCapacity ) {
        super(initialCapacity);
    }

    public BasicDocument( String name,
                          Object value ) {
        super();
        if (name != null) put(name, value);
    }

    public BasicDocument( String name1,
                          Object value1,
                          String name2,
                          Object value2 ) {
        super();
        if (name1 != null) put(name1, value1);
        if (name2 != null) put(name2, value2);
    }

    public BasicDocument( String name1,
                          Object value1,
                          String name2,
                          Object value2,
                          String name3,
                          Object value3 ) {
        super();
        if (name1 != null) put(name1, value1);
        if (name2 != null) put(name2, value2);
        if (name3 != null) put(name3, value3);
    }

    public BasicDocument( String name1,
                          Object value1,
                          String name2,
                          Object value2,
                          String name3,
                          Object value3,
                          String name4,
                          Object value4 ) {
        super();
        if (name1 != null) put(name1, value1);
        if (name2 != null) put(name2, value2);
        if (name3 != null) put(name3, value3);
        if (name4 != null) put(name4, value4);
    }

    @Override
    public boolean containsField( String name ) {
        return containsKey(name);
    }

    @Override
    public boolean containsAll( Document document ) {
        if (document == null) {
            return true;
        }
        for (Field field : document.fields()) {
            Object thisValue = this.get(field.getName());
            Object thatValue = field.getValue();
            if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object get( String name ) {
        return super.get(name); // calls Map.get(Object)
    }

    @Override
    public Map<String, ?> toMap() {
        if (unmodifiableView == null) unmodifiableView = Collections.unmodifiableMap(this);
        return unmodifiableView;
    }

    @Override
    public Iterable<Field> fields() {
        return new Iterable<Field>() {
            @Override
            public Iterator<Field> iterator() {
                final Iterator<Map.Entry<String, Object>> iter = BasicDocument.this.entrySet().iterator();
                return new Iterator<Field>() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Field next() {
                        Map.Entry<String, Object> entry = iter.next();
                        return new ImmutableField(entry.getKey(), entry.getValue());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public Boolean getBoolean( String name ) {
        Object value = get(name);
        return (value instanceof Boolean) ? (Boolean)value : null;
    }

    @Override
    public boolean getBoolean( String name,
                               boolean defaultValue ) {
        Object value = get(name);
        return (value instanceof Boolean) ? ((Boolean)value).booleanValue() : defaultValue;
    }

    @Override
    public Integer getInteger( String name ) {
        Object value = get(name);
        return (value instanceof Integer) ? (Integer)value : null;
    }

    @Override
    public int getInteger( String name,
                           int defaultValue ) {
        Object value = get(name);
        return (value instanceof Integer) ? ((Integer)value).intValue() : defaultValue;
    }

    @Override
    public Long getLong( String name ) {
        Object value = get(name);
        if (value instanceof Long) return (Long)value;
        if (value instanceof Integer) return new Long(((Integer)value).longValue());
        return null;
    }

    @Override
    public long getLong( String name,
                         long defaultValue ) {
        Object value = get(name);
        if (value instanceof Long) return ((Long)value).longValue();
        if (value instanceof Integer) return ((Integer)value).longValue();
        return defaultValue;
    }

    @Override
    public Double getDouble( String name ) {
        Object value = get(name);
        return (value instanceof Double) ? (Double)value : null;
    }

    @Override
    public double getDouble( String name,
                             double defaultValue ) {
        Object value = get(name);
        return (value instanceof Double) ? ((Double)value).doubleValue() : defaultValue;
    }

    @Override
    public Number getNumber( String name ) {
        Object value = get(name);
        return (value instanceof Number) ? (Number)value : null;
    }

    @Override
    public Number getNumber( String name,
                             Number defaultValue ) {
        Object value = get(name);
        return (value instanceof Number) ? (Number)value : defaultValue;
    }

    @Override
    public String getString( String name ) {
        return getString(name, null);
    }

    @Override
    public String getString( String name,
                             String defaultValue ) {
        Object value = get(name);
        if (value != null) {
            if (value instanceof String) {
                return (String)value;
            }
            if (value instanceof Symbol) {
                return ((Symbol)value).getSymbol();
            }
        }
        return defaultValue;
    }

    @Override
    public List<?> getArray( String name ) {
        Object value = get(name);
        return (value instanceof List) ? (List<?>)value : null;
    }

    @Override
    public Document getDocument( String name ) {
        Object value = get(name);
        return (value instanceof Document) ? (Document)value : null;
    }

    @Override
    public boolean isNull( String name ) {
        return get(name) instanceof Null;
    }

    @Override
    public boolean isNullOrMissing( String name ) {
        return Null.matches(get(name));
    }

    @Override
    public MaxKey getMaxKey( String name ) {
        Object value = get(name);
        return (value instanceof MaxKey) ? (MaxKey)value : null;
    }

    @Override
    public MinKey getMinKey( String name ) {
        Object value = get(name);
        return (value instanceof MinKey) ? (MinKey)value : null;
    }

    @Override
    public Code getCode( String name ) {
        Object value = get(name);
        return (value instanceof Code) ? (Code)value : null;
    }

    @Override
    public CodeWithScope getCodeWithScope( String name ) {
        Object value = get(name);
        return (value instanceof CodeWithScope) ? (CodeWithScope)value : null;
    }

    @Override
    public ObjectId getObjectId( String name ) {
        Object value = get(name);
        return (value instanceof ObjectId) ? (ObjectId)value : null;
    }

    @Override
    public Binary getBinary( String name ) {
        Object value = get(name);
        return (value instanceof Binary) ? (Binary)value : null;
    }

    @Override
    public Symbol getSymbol( String name ) {
        Object value = get(name);
        if (value != null) {
            if (value instanceof Symbol) {
                return (Symbol)value;
            }
            if (value instanceof String) {
                return new Symbol((String)value);
            }
        }
        return null;
    }

    @Override
    public Pattern getPattern( String name ) {
        Object value = get(name);
        return (value instanceof Pattern) ? (Pattern)value : null;
    }

    @Override
    public UUID getUuid( String name ) {
        return getUuid(name, null);
    }

    @Override
    public UUID getUuid( String name,
                         UUID defaultValue ) {
        Object value = get(name);
        if (value != null) {
            if (value instanceof UUID) {
                return (UUID)value;
            }
            if (value instanceof String) {
                try {
                    return UUID.fromString((String)value);
                } catch (IllegalArgumentException e) {
                    // do nothing ...
                }
            }
        }
        return defaultValue;
    }

    @Override
    public int getType( String name ) {
        return Bson.getTypeForValue(get(name));
    }

    @Override
    public void putAll( Document object ) {
        if (object != this) {
            // Prevent going through BasicBsonObject.unmodifiableView if we can ...
            Map<String, ?> original = object instanceof BasicDocument ? (BasicDocument)object : object.toMap();
            super.putAll(original);
        }
    }

    @Override
    public Object remove( String name ) {
        return super.remove(name); // calls Map.remove(Object)
    }

    @Override
    public void removeAll() {
        super.clear();
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Iterable) {
            // Probably an array
            return false;
        }
        if (obj instanceof Document) {
            Document that = (Document)obj;
            if (this.size() != that.size()) {
                return false;
            }
            for (Field thisField : fields()) {
                Object thisValue = thisField.getValue();
                Object thatValue = that.get(thisField.getName());
                if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
                    return false;
                }
            }
            return true;
        }
        if (obj instanceof Map) {
            Map<?, ?> that = (Map<?, ?>)obj;
            if (this.size() != that.size()) {
                return false;
            }
            if (!this.keySet().equals(that.keySet())) {
                return false;
            }
            for (Field thisField : fields()) {
                Object thisValue = thisField.getValue();
                Object thatValue = that.get(thisField.getName());
                if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return Json.write(this);
    }

    @Override
    public Document clone() {
        BasicDocument clone = new BasicDocument();
        for (Field field : this.fields()) {
            Object value = field.getValue();
            if (value instanceof Array) {
                value = ((Array)value).clone();
            } else if (value instanceof Document) {
                value = ((Document)value).clone();
            }// every other kind of value is immutable
            clone.put(field.getName(), value);
        }
        return clone;
    }

    @Override
    public Document with( Map<String, Object> changedFields ) {
        BasicDocument clone = new BasicDocument();
        for (Field field : this.fields()) {
            String name = field.getName();
            Object newValue = changedFields.get(name);
            if (newValue != null) {
                clone.put(name, newValue);
            } else {
                Object oldValue = field.getValue();
                clone.put(name, oldValue);
            }
        }
        return clone;
    }

    @Override
    public Document with( ValueTransformer transformer ) {
        boolean transformed = false;
        BasicDocument clone = new BasicDocument();
        for (Field field : this.fields()) {
            String name = field.getName();
            Object oldValue = field.getValue();
            Object newValue = null;
            if (oldValue instanceof Document) {
                newValue = ((Document)oldValue).with(transformer);
            } else {
                newValue = transformer.transform(name, oldValue);
            }
            if (newValue != oldValue) transformed = true;
            clone.put(name, newValue);
        }
        return transformed ? clone : this;
    }

    @Override
    public Document withVariablesReplaced( Properties properties ) {
        return with(new PropertiesTransformer(properties));
    }

    @Override
    public Document withVariablesReplacedWithSystemProperties() {
        return with(new SystemPropertiesTransformer());
    }

}
