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

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.internal.document.MutableArray.Entry;

public class ArrayEditor implements EditableArray {

    private static final long serialVersionUID = 1L;

    private final MutableArray array;
    private final DocumentValueFactory factory;

    public ArrayEditor( MutableArray array,
                        DocumentValueFactory factory ) {
        assert array != null;
        this.array = array;
        this.factory = factory != null ? factory : DefaultDocumentValueFactory.INSTANCE;
    }

    @Override
    public ArrayEditor clone() {
        return new ArrayEditor((MutableArray)this.array.clone(), factory);
    }

    @Override
    public Array unwrap() {
        return array;
    }

    /**
     * Return the array that was edited.
     * 
     * @return the edited array; never null
     */
    public MutableArray asMutableArray() {
        return array;
    }

    @Override
    public Object get( String name ) {
        return array.get(name);
    }

    @Override
    public Boolean getBoolean( String name ) {
        return array.getBoolean(name);
    }

    @Override
    public boolean getBoolean( String name,
                               boolean defaultValue ) {
        return array.getBoolean(name, defaultValue);
    }

    public Object put( String name,
                       Object value ) {
        return array.put(name, value);
    }

    @Override
    public void putAll( Document object ) {
        array.putAll(object);
    }

    @Override
    public void putAll( Map<? extends String, ? extends Object> map ) {
        array.putAll(map);
    }

    @Override
    public Object remove( String name ) {
        return array.remove(name);
    }

    @Override
    public Integer getInteger( String name ) {
        return array.getInteger(name);
    }

    @Override
    public int getInteger( String name,
                           int defaultValue ) {
        return array.getInteger(name, defaultValue);
    }

    @Override
    public Long getLong( String name ) {
        return array.getLong(name);
    }

    @Override
    public long getLong( String name,
                         long defaultValue ) {
        return array.getLong(name, defaultValue);
    }

    @Override
    public Double getDouble( String name ) {
        return array.getDouble(name);
    }

    @Override
    public double getDouble( String name,
                             double defaultValue ) {
        return array.getDouble(name, defaultValue);
    }

    @Override
    public Number getNumber( String name ) {
        return array.getNumber(name);
    }

    @Override
    public Number getNumber( String name,
                             Number defaultValue ) {
        return array.getNumber(name, defaultValue);
    }

    @Override
    public String getString( String name ) {
        return array.getString(name);
    }

    @Override
    public String getString( String name,
                             String defaultValue ) {
        return array.getString(name, defaultValue);
    }

    @Override
    public EditableArray getArray( String name ) {
        return editable(array.getArray(name), indexFrom(name));
    }

    @Override
    public EditableArray getOrCreateArray( String name ) {
        List<?> existing = array.getArray(name);
        return existing != null ? editable(existing, indexFrom(name)) : setArray(name);
    }

    @Override
    public EditableDocument getDocument( String name ) {
        return editable(array.getDocument(name), indexFrom(name));
    }

    @Override
    public EditableDocument getOrCreateDocument( String name ) {
        Document existing = array.getDocument(name);
        return existing != null ? editable(existing, indexFrom(name)) : setDocument(name);
    }

    @Override
    public boolean isNull( String name ) {
        return array.isNull(name);
    }

    @Override
    public boolean isNullOrMissing( String name ) {
        return array.isNullOrMissing(name);
    }

    @Override
    public MaxKey getMaxKey( String name ) {
        return array.getMaxKey(name);
    }

    @Override
    public MinKey getMinKey( String name ) {
        return array.getMinKey(name);
    }

    @Override
    public Code getCode( String name ) {
        return array.getCode(name);
    }

    @Override
    public CodeWithScope getCodeWithScope( String name ) {
        return array.getCodeWithScope(name);
    }

    @Override
    public ObjectId getObjectId( String name ) {
        return array.getObjectId(name);
    }

    @Override
    public Binary getBinary( String name ) {
        return array.getBinary(name);
    }

    @Override
    public Symbol getSymbol( String name ) {
        return array.getSymbol(name);
    }

    @Override
    public Pattern getPattern( String name ) {
        return array.getPattern(name);
    }

    @Override
    public UUID getUuid( String name ) {
        return array.getUuid(name);
    }

    @Override
    public UUID getUuid( String name,
                         UUID defaultValue ) {
        return array.getUuid(name, defaultValue);
    }

    @Override
    public int getType( String name ) {
        return array.getType(name);
    }

    @Override
    public Map<String, ? extends Object> toMap() {
        return array.toMap();
    }

    @Override
    public Iterable<Field> fields() {
        return array.fields();
    }

    @Override
    public boolean containsField( String name ) {
        return array.containsField(name);
    }

    @Override
    public boolean containsAll( Document document ) {
        return array.containsAll(document);
    }

    @Override
    public Set<String> keySet() {
        return array.keySet();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public void removeAll() {
        array.removeAll();
    }

    @Override
    public EditableArray setBoolean( String name,
                                     boolean value ) {
        return setValue(name, factory.createBoolean(value));
    }

    @Override
    public EditableArray setNumber( String name,
                                    int value ) {
        return setValue(name, factory.createInt(value));
    }

    @Override
    public EditableArray setNumber( String name,
                                    long value ) {
        return setValue(name, factory.createLong(value));
    }

    @Override
    public EditableArray setNumber( String name,
                                    float value ) {
        return setValue(name, factory.createDouble(value));
    }

    @Override
    public EditableArray setNumber( String name,
                                    double value ) {
        return setValue(name, factory.createDouble(value));
    }

    @Override
    public EditableArray setString( String name,
                                    String value ) {
        return setValue(name, factory.createString(value));
    }

    @Override
    public EditableArray setSymbol( String name,
                                    String value ) {
        return setValue(name, factory.createSymbol(value));
    }

    @Override
    public EditableDocument setDocument( String name ) {
        BasicDocument doc = new BasicDocument();
        setValue(name, doc);
        return editable(doc, indexFrom(name));
    }

    @Override
    public EditableDocument setDocument( String name,
                                         Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).unwrap();
        setValue(name, document);
        return editable(document, indexFrom(name));
    }

    @Override
    public EditableArray setArray( String name ) {
        List<?> array = new BasicArray();
        setValue(name, array);
        return editable(array, indexFrom(name));
    }

    @Override
    public EditableArray setArray( String name,
                                   Array array ) {
        setValue(name, array);
        return editable((List<?>)array, indexFrom(name));
    }

    @Override
    public EditableArray setDate( String name,
                                  Date value ) {
        return setValue(name, value);
    }

    @Override
    public EditableArray setDate( String name,
                                  String isoDate ) throws ParseException {
        return setValue(name, factory.createDate(isoDate));
    }

    @Override
    public EditableArray setTimestamp( String name,
                                       int timeInSeconds,
                                       int increment ) {
        return setValue(name, factory.createTimestamp(timeInSeconds, increment));
    }

    @Override
    public EditableArray setObjectId( String name,
                                      String hex ) {
        return setValue(name, factory.createObjectId(hex));
    }

    @Override
    public EditableArray setObjectId( String name,
                                      byte[] bytes ) {
        return setValue(name, factory.createObjectId(bytes));
    }

    @Override
    public EditableArray setObjectId( String name,
                                      int time,
                                      int machine,
                                      int process,
                                      int inc ) {
        return setValue(name, factory.createObjectId(time, machine, process, inc));
    }

    @Override
    public EditableArray setRegularExpression( String name,
                                               String pattern ) {
        return setValue(name, factory.createRegex(pattern, null));
    }

    @Override
    public EditableArray setRegularExpression( String name,
                                               String pattern,
                                               int flags ) {
        return setValue(name, factory.createRegex(pattern, BsonUtils.regexFlagsFor(flags)));
    }

    @Override
    public EditableArray setNull( String name ) {
        return setValue(name, factory.createNull());
    }

    @Override
    public EditableArray setBinary( String name,
                                    byte type,
                                    byte[] data ) {
        return setValue(name, factory.createBinary(type, data));
    }

    @Override
    public EditableArray setUuid( String name,
                                  UUID uuid ) {
        return setValue(name, uuid);
    }

    @Override
    public EditableDocument setCode( String name,
                                     String code,
                                     boolean includeScope ) {
        if (includeScope) {
            BasicDocument scope = new BasicDocument();
            setValue(name, factory.createCode(code, scope));
            return editable(scope, indexFrom(name));
        }
        return setValue(name, factory.createCode(code));
    }

    @Override
    public EditableDocument setCode( String name,
                                     String code,
                                     Document scope ) {
        if (scope != null) {
            setValue(name, factory.createCode(code, scope));
            return editable(scope, indexFrom(name));
        }
        return setValue(name, factory.createCode(code));
    }

    protected EditableArray setValue( String name,
                                      Object value ) {
        doSetValue(name, value);
        return this;
    }

    @Override
    public EditableArray setValue( int index,
                                   Object value ) {
        doSetValue(index, value);
        return this;
    }

    @Override
    public EditableArray addValue( Object value ) {
        doAddValue(value);
        return this;
    }

    @Override
    public EditableArray addValueIfAbsent( Object value ) {
        doAddValueIfAbsent(value);
        return this;
    }

    @Override
    public EditableArray addValue( int index,
                                   Object value ) {
        doAddValue(index, value);
        return this;
    }

    protected final int indexFrom( String name ) {
        return Integer.parseInt(name);
    }

    protected Object doSetValue( String name,
                                 Object value ) {
        int index = indexFrom(name);
        return doSetValue(index, value);
    }

    protected Object doSetValue( int index,
                                 Object value ) {
        value = unwrap(value);
        return array.setValue(index, value);
    }

    protected int doAddValue( Object value ) {
        value = unwrap(value);
        return array.addValue(value);
    }

    protected void doAddValue( int index,
                               Object value ) {
        value = unwrap(value);
        array.addValue(index, value);
    }

    protected boolean doAddValueIfAbsent( Object value ) {
        value = unwrap(value);
        return array.addValueIfAbsent(value);
    }

    protected boolean doRemoveValue( Object value ) {
        value = unwrap(value);
        return array.removeValue(value);
    }

    protected Object doRemoveValue( int index ) {
        return array.removeValue(index);
    }

    protected boolean doAddAll( Collection<? extends Object> c ) {
        return array.addAllValues(c);
    }

    protected boolean doAddAll( int index,
                                Collection<? extends Object> c ) {
        return array.addAllValues(index, c);
    }

    protected List<Entry> doRemoveAll( Collection<?> c ) {
        return array.removeAllValues(c);
    }

    protected List<Entry> doRetainAll( Collection<?> c ) {
        return array.retainAllValues(c);
    }

    protected void doClear() {
        array.removeAll();
    }

    protected EditableDocument editable( Document doc,
                                         int index ) {
        if (doc == null) return null;
        assert !(doc instanceof DocumentEditor) : "The document value should not be a DocumentEditor instance";
        if (doc instanceof MutableArray) {
            return createEditableArray((MutableArray)doc, index, factory);
        }
        assert doc instanceof MutableDocument;
        return createEditableDocument((MutableDocument)doc, index, factory);
    }

    protected EditableArray editable( List<?> array,
                                      int index ) {
        if (array == null) return null;
        assert !(array instanceof ArrayEditor) : "The array value should not be an ArrayEditor instance";
        return createEditableArray((BasicArray)array, index, factory);
    }

    private EditableArray editableSublist( List<?> sublist ) {
        assert sublist != null;
        if (sublist instanceof EditableArray) return (EditableArray)array;
        return createEditableSublist((BasicArray)sublist, factory);
    }

    protected EditableDocument createEditableDocument( MutableDocument document,
                                                       int index,
                                                       DocumentValueFactory factory ) {
        return new DocumentEditor(document, factory);
    }

    protected EditableArray createEditableArray( MutableArray array,
                                                 int index,
                                                 DocumentValueFactory factory ) {
        return new ArrayEditor(array, factory);
    }

    protected EditableArray createEditableSublist( MutableArray array,
                                                   DocumentValueFactory factory ) {
        return new ArrayEditor(array, factory);
    }

    public static Array unwrap( Array array ) {
        if (array instanceof ArrayEditor) {
            return unwrap(((ArrayEditor)array).unwrap());
        }
        return array;
    }

    public static Document unwrap( Document document ) {
        if (document instanceof DocumentEditor) {
            return unwrap(((DocumentEditor)document).unwrap());
        }
        return document;
    }

    public static Object unwrap( Object value ) {
        if (value instanceof DocumentEditor) {
            return unwrap(((DocumentEditor)value).unwrap());
        }
        if (value instanceof ArrayEditor) {
            return unwrap(((ArrayEditor)value).unwrap());
        }
        return value;
    }

    @Override
    public boolean isEmpty() {
        return array.isEmpty();
    }

    @Override
    public boolean contains( Object o ) {
        return array.contains(o);
    }

    @Override
    public Iterator<Object> iterator() {
        return array.iterator();
    }

    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @Override
    public <T> T[] toArray( T[] a ) {
        return array.toArray(a);
    }

    @Override
    public boolean add( Object e ) {
        return doAddValue(e) != -1;
    }

    @Override
    public boolean remove( Object o ) {
        return doRemoveValue(o);
    }

    @Override
    public boolean containsAll( Collection<?> c ) {
        return array.containsAll(c);
    }

    @Override
    public boolean addAll( Collection<? extends Object> c ) {
        return doAddAll(c);
    }

    @Override
    public boolean addAll( int index,
                           Collection<? extends Object> c ) {
        return doAddAll(index, c);
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        List<Entry> removed = doRemoveAll(c);
        return !removed.isEmpty();
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        List<Entry> removed = doRetainAll(c);
        return !removed.isEmpty();
    }

    @Override
    public void clear() {
        doClear();
    }

    @Override
    public Object get( int index ) {
        return array.get(index);
    }

    @Override
    public Object set( int index,
                       Object element ) {
        return doSetValue(index, element);
    }

    @Override
    public void add( int index,
                     Object element ) {
        doAddValue(index, element);
    }

    @Override
    public Object remove( int index ) {
        return doRemoveValue(index);
    }

    @Override
    public int indexOf( Object o ) {
        return indexOf(o);
    }

    @Override
    public int lastIndexOf( Object o ) {
        return array.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return array.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator( int index ) {
        return array.listIterator(index);
    }

    @Override
    public List<Object> subList( int fromIndex,
                                 int toIndex ) {
        return editableSublist(array.subList(fromIndex, toIndex));
    }

    @Override
    public EditableArray set( String name,
                              Object value ) {
        return setValue(name, value);
    }

    @Override
    public EditableArray setBoolean( int index,
                                     boolean value ) {
        return setValue(index, factory.createBoolean(value));
    }

    @Override
    public EditableArray setNumber( int index,
                                    int value ) {
        return setValue(index, factory.createInt(value));
    }

    @Override
    public EditableArray setNumber( int index,
                                    long value ) {
        return setValue(index, factory.createLong(value));
    }

    @Override
    public EditableArray setNumber( int index,
                                    float value ) {
        return setValue(index, factory.createDouble(value));
    }

    @Override
    public EditableArray setNumber( int index,
                                    double value ) {
        return setValue(index, factory.createDouble(value));
    }

    @Override
    public EditableArray setString( int index,
                                    String value ) {
        return setValue(index, factory.createString(value));
    }

    @Override
    public EditableArray setSymbol( int index,
                                    String value ) {
        return setValue(index, factory.createSymbol(value));
    }

    @Override
    public EditableDocument setDocument( int index ) {
        BasicDocument doc = new BasicDocument();
        setValue(index, doc);
        return editable(doc, index);
    }

    @Override
    public EditableDocument setDocument( int index,
                                         Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).unwrap();
        setValue(index, document);
        return editable(document, index);
    }

    @Override
    public EditableArray setArray( int index ) {
        List<?> array = new BasicArray();
        setValue(index, array);
        return editable(array, index);
    }

    @Override
    public EditableArray setArray( int index,
                                   Array array ) {
        setValue(index, array);
        return editable((List<?>)array, index);
    }

    @Override
    public EditableArray setDate( int index,
                                  Date value ) {
        setValue(index, value);
        return this;
    }

    @Override
    public EditableArray setDate( int index,
                                  String isoDate ) throws ParseException {
        setValue(index, factory.createDate(isoDate));
        return this;
    }

    @Override
    public EditableArray setTimestamp( int index,
                                       int timeInSeconds,
                                       int increment ) {
        return setValue(index, factory.createTimestamp(timeInSeconds, increment));
    }

    @Override
    public EditableArray setObjectId( int index,
                                      String hex ) {
        return setValue(index, factory.createObjectId(hex));
    }

    @Override
    public EditableArray setObjectId( int index,
                                      byte[] bytes ) {
        return setValue(index, factory.createObjectId(bytes));
    }

    @Override
    public EditableArray setObjectId( int index,
                                      int time,
                                      int machine,
                                      int process,
                                      int inc ) {
        return setValue(index, factory.createObjectId(time, machine, process, inc));
    }

    @Override
    public EditableArray setRegularExpression( int index,
                                               String pattern ) {
        return setValue(index, factory.createRegex(pattern, null));
    }

    @Override
    public EditableArray setRegularExpression( int index,
                                               String pattern,
                                               int flags ) {
        return setValue(index, factory.createRegex(pattern, flags));
    }

    @Override
    public EditableArray setNull( int index ) {
        return setValue(index, factory.createNull());
    }

    @Override
    public EditableArray setBinary( int index,
                                    byte type,
                                    byte[] data ) {
        return setValue(index, factory.createBinary(type, data));
    }

    @Override
    public EditableArray setUuid( int index,
                                  UUID uuid ) {
        return setValue(index, uuid);
    }

    @Override
    public EditableDocument setCode( int index,
                                     String code,
                                     boolean includeScope ) {
        if (includeScope) {
            BasicDocument scope = new BasicDocument();
            setValue(index, factory.createCode(code, scope));
            return editable(scope, index);
        }
        return setValue(index, factory.createCode(code));
    }

    @Override
    public EditableDocument setCode( int index,
                                     String code,
                                     Document scope ) {
        if (scope != null) {
            setValue(index, factory.createCode(code, scope));
            return editable(scope, index);
        }
        return setValue(index, factory.createCode(code));
    }

    @Override
    public EditableArray addBoolean( int index,
                                     boolean value ) {
        return addValue(index, factory.createBoolean(value));
    }

    @Override
    public EditableArray addNumber( int index,
                                    int value ) {
        return addValue(index, factory.createInt(value));
    }

    @Override
    public EditableArray addNumber( int index,
                                    long value ) {
        return addValue(index, factory.createLong(value));
    }

    @Override
    public EditableArray addNumber( int index,
                                    float value ) {
        return addValue(index, factory.createDouble(value));
    }

    @Override
    public EditableArray addNumber( int index,
                                    double value ) {
        return addValue(index, factory.createDouble(value));
    }

    @Override
    public EditableArray addString( int index,
                                    String value ) {
        return addValue(index, factory.createString(value));
    }

    @Override
    public EditableArray addSymbol( int index,
                                    String value ) {
        return addValue(index, factory.createSymbol(value));
    }

    @Override
    public EditableDocument addDocument( int index ) {
        BasicDocument doc = new BasicDocument();
        addValue(index, doc);
        return editable(doc, index);
    }

    @Override
    public EditableDocument addDocument( int index,
                                         Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).unwrap();
        addValue(index, document);
        return editable(document, index);
    }

    @Override
    public EditableArray addArray( int index ) {
        List<?> array = new BasicArray();
        addValue(index, array);
        return editable(array, index);
    }

    @Override
    public EditableArray addArray( int index,
                                   Array array ) {
        addValue(index, array);
        return editable((List<?>)array, index);
    }

    @Override
    public EditableArray addDate( int index,
                                  Date value ) {
        addValue(index, value);
        return this;
    }

    @Override
    public EditableArray addDate( int index,
                                  String isoDate ) throws ParseException {
        addValue(index, factory.createDate(isoDate));
        return this;
    }

    @Override
    public EditableArray addTimestamp( int index,
                                       int timeInSeconds,
                                       int increment ) {
        return addValue(index, factory.createTimestamp(timeInSeconds, increment));
    }

    @Override
    public EditableArray addObjectId( int index,
                                      String hex ) {
        return addValue(index, factory.createObjectId(hex));
    }

    @Override
    public EditableArray addObjectId( int index,
                                      byte[] bytes ) {
        return addValue(index, factory.createObjectId(bytes));
    }

    @Override
    public EditableArray addObjectId( int index,
                                      int time,
                                      int machine,
                                      int process,
                                      int inc ) {
        return addValue(index, factory.createObjectId(time, machine, process, inc));
    }

    @Override
    public EditableArray addRegularExpression( int index,
                                               String pattern ) {
        return addValue(index, factory.createRegex(pattern, null));
    }

    @Override
    public EditableArray addRegularExpression( int index,
                                               String pattern,
                                               int flags ) {
        return addValue(index, factory.createRegex(pattern, flags));
    }

    @Override
    public EditableArray addNull( int index ) {
        return addValue(index, factory.createNull());
    }

    @Override
    public EditableArray addBinary( int index,
                                    byte type,
                                    byte[] data ) {
        return addValue(index, factory.createBinary(type, data));
    }

    @Override
    public EditableArray addUuid( int index,
                                  UUID uuid ) {
        return addValue(index, uuid);
    }

    @Override
    public EditableDocument addCode( int index,
                                     String code,
                                     boolean includeScope ) {
        if (includeScope) {
            BasicDocument scope = new BasicDocument();
            addValue(index, factory.createCode(code, scope));
            return editable(scope, index);
        }
        return addValue(index, factory.createCode(code));
    }

    @Override
    public EditableDocument addCode( int index,
                                     String code,
                                     Document scope ) {
        if (scope != null) {
            addValue(index, factory.createCode(code, scope));
            return editable(scope, index);
        }
        return addValue(index, factory.createCode(code));
    }

    @Override
    public EditableArray addBoolean( boolean value ) {
        return addValue(factory.createBoolean(value));
    }

    @Override
    public EditableArray addNumber( int value ) {
        return addValue(factory.createInt(value));
    }

    @Override
    public EditableArray addNumber( long value ) {
        return addValue(factory.createLong(value));
    }

    @Override
    public EditableArray addNumber( float value ) {
        return addValue(factory.createDouble(value));
    }

    @Override
    public EditableArray addNumber( double value ) {
        return addValue(factory.createDouble(value));
    }

    @Override
    public EditableArray addString( String value ) {
        return addValue(factory.createString(value));
    }

    @Override
    public EditableArray addSymbol( String value ) {
        return addValue(factory.createSymbol(value));
    }

    @Override
    public EditableDocument addDocument() {
        BasicDocument doc = new BasicDocument();
        addValue(doc);
        return editable(doc, size());
    }

    @Override
    public EditableDocument addDocument( Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).unwrap();
        addValue(document);
        return editable(document, size());
    }

    @Override
    public EditableArray addArray() {
        List<?> array = new BasicArray();
        addValue(array);
        return editable(array, size());
    }

    @Override
    public EditableArray addArray( Array array ) {
        addValue(array);
        return editable((List<?>)array, size());
    }

    @Override
    public EditableArray addDate( Date value ) {
        return addValue(value);
    }

    @Override
    public EditableArray addDate( String isoDate ) throws ParseException {
        return addValue(factory.createDate(isoDate));
    }

    @Override
    public EditableArray addTimestamp( int timeInSeconds,
                                       int increment ) {
        return addValue(factory.createTimestamp(timeInSeconds, increment));
    }

    @Override
    public EditableArray addObjectId( String hex ) {
        return addValue(factory.createObjectId(hex));
    }

    @Override
    public EditableArray addObjectId( byte[] bytes ) {
        return addValue(factory.createObjectId(bytes));
    }

    @Override
    public EditableArray addObjectId( int time,
                                      int machine,
                                      int process,
                                      int inc ) {
        return addValue(factory.createObjectId(time, machine, process, inc));
    }

    @Override
    public EditableArray addRegularExpression( String pattern ) {
        return addValue(factory.createRegex(pattern, null));
    }

    @Override
    public EditableArray addRegularExpression( String pattern,
                                               int flags ) {
        return addValue(factory.createRegex(pattern, flags));
    }

    @Override
    public EditableArray addNull() {
        return addValue(factory.createNull());
    }

    @Override
    public EditableArray addBinary( byte type,
                                    byte[] data ) {
        return addValue(factory.createBinary(type, data));
    }

    @Override
    public EditableArray addUuid( UUID uuid ) {
        return addValue(uuid);
    }

    @Override
    public EditableDocument addCode( String code,
                                     boolean includeScope ) {
        if (includeScope) {
            BasicDocument scope = new BasicDocument();
            addValue(factory.createCode(code, scope));
            return editable(scope, size());
        }
        return addValue(factory.createCode(code));
    }

    @Override
    public EditableDocument addCode( String code,
                                     Document scope ) {
        if (scope != null) {
            addValue(factory.createCode(code, scope));
            return editable(scope, size());
        }
        return addValue(factory.createCode(code));
    }

    @Override
    public EditableArray addBooleanIfAbsent( boolean value ) {
        return addValueIfAbsent(factory.createBoolean(value));
    }

    @Override
    public EditableArray addNumberIfAbsent( int value ) {
        return addValueIfAbsent(factory.createInt(value));
    }

    @Override
    public EditableArray addNumberIfAbsent( long value ) {
        return addValueIfAbsent(factory.createLong(value));
    }

    @Override
    public EditableArray addNumberIfAbsent( float value ) {
        return addValueIfAbsent(factory.createDouble(value));
    }

    @Override
    public EditableArray addNumberIfAbsent( double value ) {
        return addValueIfAbsent(factory.createDouble(value));
    }

    @Override
    public EditableArray addStringIfAbsent( String value ) {
        return addValueIfAbsent(factory.createString(value));
    }

    @Override
    public EditableArray addSymbolIfAbsent( String value ) {
        return addValueIfAbsent(factory.createSymbol(value));
    }

    @Override
    public EditableDocument addDocumentIfAbsent( Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).unwrap();
        return doAddValueIfAbsent(document) ? editable(document, size()) : null;
    }

    @Override
    public EditableArray addArrayIfAbsent( Array array ) {
        return doAddValueIfAbsent(array) ? editable((List<?>)array, size()) : null;
    }

    @Override
    public EditableArray addDateIfAbsent( Date value ) {
        return addValueIfAbsent(value);
    }

    @Override
    public EditableArray addDateIfAbsent( String isoDate ) throws ParseException {
        return addValueIfAbsent(factory.createDate(isoDate));
    }

    @Override
    public EditableArray addTimestampIfAbsent( int timeInSeconds,
                                               int increment ) {
        return addValueIfAbsent(factory.createTimestamp(timeInSeconds, increment));
    }

    @Override
    public EditableArray addObjectIdIfAbsent( String hex ) {
        return addValueIfAbsent(factory.createObjectId(hex));
    }

    @Override
    public EditableArray addObjectIdIfAbsent( byte[] bytes ) {
        return addValueIfAbsent(factory.createObjectId(bytes));
    }

    @Override
    public EditableArray addObjectIdIfAbsent( int time,
                                              int machine,
                                              int process,
                                              int inc ) {
        return addValueIfAbsent(factory.createObjectId(time, machine, process, inc));
    }

    @Override
    public EditableArray addRegularExpressionIfAbsent( String pattern ) {
        return addValueIfAbsent(factory.createRegex(pattern, null));
    }

    @Override
    public EditableArray addRegularExpressionIfAbsent( String pattern,
                                                       int flags ) {
        return addValueIfAbsent(factory.createRegex(pattern, flags));
    }

    @Override
    public EditableArray addNullIfAbsent() {
        return addValueIfAbsent(factory.createNull());
    }

    @Override
    public EditableArray addBinaryIfAbsent( byte type,
                                            byte[] data ) {
        return addValueIfAbsent(factory.createBinary(type, data));
    }

    @Override
    public EditableArray addUuidIfAbsent( UUID uuid ) {
        return addValueIfAbsent(uuid);
    }

    @Override
    public EditableDocument addCodeIfAbsent( String code,
                                             Document scope ) {
        if (scope != null) {
            return doAddValueIfAbsent(factory.createCode(code, scope)) ? editable(scope, size()) : null;
        }
        return addValueIfAbsent(factory.createCode(code));
    }

    @Override
    public String toString() {
        return array.toString();
    }

}
