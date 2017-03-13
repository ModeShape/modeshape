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
package org.modeshape.schematic.internal.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Bson;
import org.modeshape.schematic.document.Code;
import org.modeshape.schematic.document.CodeWithScope;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.MaxKey;
import org.modeshape.schematic.document.MinKey;
import org.modeshape.schematic.document.Null;
import org.modeshape.schematic.document.ObjectId;
import org.modeshape.schematic.document.Symbol;
import org.modeshape.schematic.internal.schema.DocumentTransformer;

/**
 * A {@link Bson.Type#ARRAY ordered array of values} for use as a value within a
 * {@link Document BSON Object}. Instances of this type are designed to be unmodifiable from a client's perspective, since clients
 * always modify the instances using an editor. There are several <code>internal*</code> methods that do modify the contents, but
 * these may not be used by client applications.
 * <p>
 * Since BSON and JSON documents can be simple arrays of values, this class implements the {@link Document} interface, where the
 * object's names are expected to be string values of integer indexes. This class also implements {@link List} interface, but only
 * supports the read methods.
 * </p>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public class BasicArray implements MutableArray {

    private static final long serialVersionUID = 1L;

    private final List<Object> values;

    public BasicArray() {
        this.values = new ArrayList<>();
    }

    public BasicArray( int initialCapacity ) {
        this.values = initialCapacity > 0 ? new ArrayList<>(initialCapacity) : new ArrayList<>();
    }

    public BasicArray( List<Object> values ) {
        this.values = values;
    }

    public BasicArray( Object... values ) {
        this.values = new ArrayList<>(Arrays.asList(values));
    }

    @Override
    public Object get( String name ) {
        int index = indexFrom(name);
        return isValidIndex(index) ? values.get(index) : null;
    }

    @Override
    public boolean containsField( String name ) {
        int index = indexFrom(name);
        return isValidIndex(index);
    }

    @Override
    public boolean containsAll( Document document ) {
        if (document instanceof Array) {
            return containsAll((List<?>)document);
        }
        if (document != null) {
            for (Field field : document.fields()) {
                Object thisValue = get(field.getName());
                Object thatValue = field.getValue();
                if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Set<String> keySet() {
        return new IndexSequence(size());
    }

    @Override
    public Map<String, ?> toMap() {
        Map<String, Object> result = new HashMap<>();
        int i = 0;
        for (String index : keySet()) {
            result.put(index, values.get(i++)); // we know that keySet().iterator() is ordered
        }
        return result;
    }

    @Override
    public Iterable<Field> fields() {
        return () -> {
            final Iterator<String> indexIter = IndexSequence.infiniteSequence();
            final Iterator<Object> valueIter = values.iterator();
            return new Iterator<Field>() {
                @Override
                public boolean hasNext() {
                    return valueIter.hasNext();
                }

                @Override
                public Field next() {
                    return new ImmutableField(indexIter.next(), valueIter.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        };
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean contains( Object o ) {
        return values.contains(o);
    }

    @Override
    public boolean containsAll( Collection<?> c ) {
        return values.containsAll(c);
    }

    @Override
    public Object get( int index ) {
        return values.get(index);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Iterable) {
            Iterable<?> that = (Iterable<?>)obj;
            Iterator<?> thisIter = values.iterator();
            Iterator<?> thatIter = null;
            if (obj instanceof List) {
                List<?> thatList = (List<?>)that;
                if (this.size() != thatList.size()) {
                    return false;
                }
                if (thatList instanceof BasicArray) {
                    thatIter = ((BasicArray)thatList).values.iterator();
                } else {
                    thatIter = that.iterator();
                }
            }
            assert thatIter != null;
            while (thisIter.hasNext() && thatIter.hasNext()) {
                Object thisValue = thisIter.next();
                Object thatValue = thatIter.next();
                if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
                    return false;
                }
            }
            return !thisIter.hasNext() && !thatIter.hasNext();
        }
        if (obj.getClass().isArray()) {
            if (this.size() != java.lang.reflect.Array.getLength(obj)) {
                return false;
            }
            Iterator<?> thisIter = values.iterator();
            int index = 0;
            while (thisIter.hasNext()) {
                Object thisValue = thisIter.next();
                Object thatValue = java.lang.reflect.Array.get(obj, index++);
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
    public int indexOf( Object o ) {
        return values.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public int lastIndexOf( Object o ) {
        return values.lastIndexOf(o);
    }

    @Override
    public List<Object> subList( int fromIndex,
                                 int toIndex ) {
        return new BasicArray(values.subList(fromIndex, toIndex));
    }

    @Override
    public Object[] toArray() {
        return values.toArray();
    }

    @Override
    public <T> T[] toArray( T[] a ) {
        return values.toArray(a);
    }

    @Override
    public Iterator<Object> iterator() {
        final Iterator<Object> delegate = values.iterator();
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Object next() {
                return delegate.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterable<Entry> getEntries() {
        return () -> new Iterator<Entry>() {
            @SuppressWarnings( "synthetic-access" )
            private final Iterator<Object> valueIter = BasicArray.this.values.iterator();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return valueIter.hasNext();
            }

            @Override
            public Entry next() {
                Object value = valueIter.next();
                return new BasicEntry(index++, value);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
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
    public Date getDate(String name) {
        Object value = get(name);
        return (value instanceof Date) ? (Date) value : null;
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
    public ListIterator<Object> listIterator() {
        return new UnmodifiableListIterator(values.listIterator());
    }

    @Override
    public ListIterator<Object> listIterator( int index ) {
        return new UnmodifiableListIterator(values.listIterator(index));
    }

    protected static final class UnmodifiableListIterator implements ListIterator<Object> {
        private final ListIterator<Object> delegate;

        protected UnmodifiableListIterator( ListIterator<Object> delegate ) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Object next() {
            return delegate.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add( Object e ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @Override
        public int nextIndex() {
            return delegate.nextIndex();
        }

        @Override
        public Object previous() {
            return delegate.previous();
        }

        @Override
        public int previousIndex() {
            return delegate.previousIndex();
        }

        @Override
        public void set( Object e ) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void add( int index,
                     Object element ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add( Object e ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll( int index,
                           Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove( int index ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove( Object o ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object set( int index,
                       Object element ) {
        throw new UnsupportedOperationException();
    }

    protected final int indexFrom( String name ) {
        return Integer.parseInt(name);
    }

    protected final boolean isValidIndex( int index ) {
        return index >= 0 && index < size();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mutation methods, for use only by the editor framework
    // ---------------------------------------------------------------------------------------------------------

    protected Object unwrap( Object value ) {
        if (value instanceof DocumentEditor) {
            return unwrap(((DocumentEditor)value).unwrap());
        }
        if (value instanceof ArrayEditor) {
            return unwrap(((ArrayEditor)value).unwrap());
        }
        return value;
    }

    @Override
    public boolean addValueIfAbsent( Object value ) {
        value = unwrap(value);
        return !this.values.contains(value) ? this.values.add(value) : false;
    }

    @Override
    public int addValue( Object value ) {
        value = unwrap(value);
        int index = this.values.size();
        this.values.add(index, value);
        return index;
    }

    @Override
    public void addValue( int index,
                          Object value ) {
        value = unwrap(value);
        this.values.add(index, value);
    }

    @Override
    public Object setValue( int index,
                            Object value ) {
        value = unwrap(value);
        return this.values.set(index, value);
    }

    @Override
    public boolean removeValue( Object value ) {
        value = unwrap(value);
        return this.values.remove(value);
    }

    @Override
    public Object removeValue( int index ) {
        return values.remove(index);
    }

    @Override
    public boolean addAllValues( Collection<?> values ) {
        if (values == null || values.isEmpty()) return false;
        this.values.addAll(values.stream().map(this::unwrap).collect(Collectors.toList()));
        return true;
    }

    @Override
    public boolean addAllValues( int index,
                                 Collection<?> values ) {
        if (values == null || values.isEmpty()) return false;
        for (Object value : values) {
            this.values.add(index, unwrap(value));
        }
        return true;
    }

    @Override
    public List<Entry> removeAllValues( Collection<?> valuesToBeRemoved ) {
        return removeValues(valuesToBeRemoved, true);
    }

    @Override
    public List<Entry> retainAllValues( Collection<?> valuesToBeRetained ) {
        return removeValues(valuesToBeRetained, false);
    }

    /**
     * Remove some of the values in this array.
     * 
     * @param values the values to be compared to this array's values
     * @param ifMatch true if this method should retain all values that match the supplied values, or false if this method should
     *        remove all values that match the supplied values
     * @return the entries that were removed; never null
     */
    private List<Entry> removeValues( Collection<?> values,
                                      boolean ifMatch ) {
        LinkedList<Entry> results = null;

        // Record the list of entries that are removed, but start at the end of the values (so the indexes are correct)
        ListIterator<?> iter = this.values.listIterator(size());
        while (iter.hasPrevious()) {
            int index = iter.previousIndex();
            Object value = iter.previous();
            if (ifMatch == values.contains(value)) {
                iter.remove();
                if (results == null) {
                    results = new LinkedList<>();
                }
                results.addFirst(new BasicEntry(index, value));
            }
        }

        return results != null ? results : Collections.<Entry>emptyList();
    }

    @Immutable
    public static final class BasicEntry implements Entry {
        private final int index;
        private final Object value;

        public BasicEntry( int index,
                           Object value ) {
            this.index = index;
            this.value = value;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public int compareTo( Entry o ) {
            return o == this ? 0 : o == null ? 1 : o.getIndex() - this.getIndex();
        }
    }

    @Override
    public Object remove( String name ) {
        try {
            int index = indexFrom(name);
            return isValidIndex(index) ? values.remove(index) : null;
        } catch (NumberFormatException e) {
            // Must be a value ...
            return removeValue(name);
        }
    }

    @Override
    public void removeAll() {
        values.clear();
    }

    @Override
    public Object put( String name,
                       Object value ) {
        return put(indexFrom(name), value);
    }

    protected final Object put( int index,
                                Object value ) {
        final int size = size();
        if (index == size) {
            values.add(unwrap(value));
            return value;
        }
        return values.set(index, unwrap(value)); // may throw IndexOutOfBoundsException
    }

    @Override
    public void putAll( Document object ) {
        if (object instanceof BasicArray) {
            BasicArray that = (BasicArray)object;
            addAll(that.values);
        }
    }

    @Override
    public void putAll( Map<? extends String, ?> map ) {
        // Attempt to convert all of the keys to integers ...
        List<IndexEntry> sortableEntries = new ArrayList<>(map.size());
        for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
            int index = indexFrom(entry.getKey());
            sortableEntries.add(new IndexEntry(index, entry.getValue()));
        }
        Collections.sort(sortableEntries);

        // Now add them in increasing order ...
        for (IndexEntry entry : sortableEntries) {
            put(entry.index, unwrap(entry.value));
        }
    }

    @Override
    public MutableArray clone() {
        BasicArray clone = new BasicArray();
        for (Object value : this) {
            value = unwrap(value);
            if (value instanceof Array) {
                value = ((Array)value).clone();
            } else if (value instanceof Document) {
                value = ((Document)value).clone();
            }// every other kind of value is immutable
            clone.addValue(value);
        }
        return clone;
    }

    @Override
    public Array with( Map<String, Object> changedFields ) {
        BasicArray clone = new BasicArray();
        for (Field field : this.fields()) {
            String name = field.getName();
            Object newValue = unwrap(changedFields.get(name));
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
    public Document with( String fieldName,
                          Object newValue ) {
        newValue = unwrap(newValue);
        BasicArray clone = new BasicArray();
        for (Field field : this.fields()) {
            String name = field.getName();
            if (name.equals(fieldName)) {
                clone.put(name, newValue);
            } else {
                Object oldValue = field.getValue();
                clone.put(name, oldValue);
            }
        }
        return clone;
    }

    @Override
    public Array with( ValueTransformer transformer ) {
        boolean transformed = false;
        BasicArray clone = new BasicArray();
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
            clone.put(name, unwrap(newValue));
        }
        return transformed ? clone : this;
    }

    @Override
    public Array withVariablesReplaced( Properties properties ) {
        return with(new DocumentTransformer.PropertiesTransformer(properties));
    }

    @Override
    public Array withVariablesReplacedWithSystemProperties() {
        return with(new DocumentTransformer.SystemPropertiesTransformer());
    }

    @Immutable
    protected static final class IndexEntry implements Comparable<IndexEntry> {
        protected final int index;
        protected final Object value;

        protected IndexEntry( int index,
                              Object value ) {
            this.index = index;
            this.value = value;
        }

        @Override
        public int compareTo( IndexEntry that ) {
            return this.index - that.index;
        }

        @Override
        public int hashCode() {
            return index;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof IndexEntry) {
                IndexEntry that = (IndexEntry)obj;
                if (this.index != that.index) return false;
                if (this.value == null) return that.value == null;
                return this.value.equals(that.value);
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + index + ',' + value + ']';
        }
    }

}
