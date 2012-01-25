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
package org.modeshape.jcr.query.lucene.basic;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.TermVector;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.query.lucene.FieldUtil;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.Reference;

/**
 * The Hibernate Search {@link FieldBridge} implementation that generates a field in the Lucene Document for each property.
 */
public class NodeInfoBridge implements FieldBridge {

    private static final Integer TRUE_INT = new Integer(1);
    private static final Integer FALSE_INT = new Integer(0);

    /**
     * This class always always adds fields that are {@link Index#NOT_ANALYZED}, since we want to search based up the fields
     * actual (non-analyzed) values. Only the fields used in full-text searching need to be analyzed, and those fields are handled
     * in other ways.
     */
    private static final LuceneOptions NON_ANALYZED_OPTIONS = new LuceneOptionsImpl(Store.NO, Index.NOT_ANALYZED, TermVector.NO,
                                                                                    null);

    @Override
    public void set( String name,
                     Object value,
                     Document document,
                     LuceneOptions luceneOptions ) {
        @SuppressWarnings( "unchecked" )
        Map<String, Object> properties = (Map<String, Object>)value;
        AtomicReference<Set<String>> sha1s = new AtomicReference<Set<String>>();
        AtomicReference<Set<String>> strongRefs = new AtomicReference<Set<String>>();
        AtomicReference<Set<String>> weakRefs = new AtomicReference<Set<String>>();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String propName = property.getKey();
            Object propValue = property.getValue();
            if (propValue instanceof Object[]) {
                for (Object arrayValue : (Object[])propValue) {
                    addField(propName, arrayValue, document, sha1s, strongRefs, weakRefs);
                }
            } else {
                addField(propName, propValue, document, sha1s, strongRefs, weakRefs);
            }
        }

        // Add the fields for the binary and reference values ...
        addField(NodeInfoIndex.FieldName.BINARY_SHA1S, sha1s, document);
        addField(NodeInfoIndex.FieldName.STRONG_REFERENCES, strongRefs, document);
        addField(NodeInfoIndex.FieldName.WEAK_REFERENCES, weakRefs, document);
    }

    protected final void addField( String name,
                                   AtomicReference<Set<String>> valuesRef,
                                   Document document ) {
        Set<String> values = valuesRef.get();
        if (values != null) {
            assert !values.isEmpty();
            for (String value : values) {
                NON_ANALYZED_OPTIONS.addFieldToDocument(name, value, document);
            }
        }
    }

    protected final void addField( String name,
                                   Object value,
                                   Document document,
                                   AtomicReference<Set<String>> sha1s,
                                   AtomicReference<Set<String>> strongRefs,
                                   AtomicReference<Set<String>> weakRefs ) {
        if (value instanceof String) {
            // We always want to store the string value is non-analyzed format, since it will not be used for
            // exact criteria matching and not for full-text searching ...
            NON_ANALYZED_OPTIONS.addFieldToDocument(name, (String)value, document);
        } else if (value instanceof Boolean) {
            // Boolean values are stored using integer values '1' and '0' ...
            Boolean bValue = (Boolean)value;
            Integer iValue = bValue.booleanValue() ? TRUE_INT : FALSE_INT;
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, iValue, document);
        } else if (value instanceof Integer) {
            // Integer values are stored as integers ...
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, value, document);
        } else if (value instanceof Long) {
            // Long values are stored as integers ...
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, value, document);
        } else if (value instanceof Float) {
            // Float values are stored as integers ...
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, value, document);
        } else if (value instanceof Double) {
            // Double values are stored as integers ...
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, value, document);
        } else if (value instanceof BigDecimal) {
            // Convert big decimals to a string, using a specific format ...
            String strValue = FieldUtil.decimalToString((BigDecimal)value);
            NON_ANALYZED_OPTIONS.addFieldToDocument(name, strValue, document);
        } else if (value instanceof Binary) {
            Binary binary = (Binary)value;
            String sha1 = binary.getHexHash();
            addValueTo(sha1, sha1s);
            // Now add a field for the property that has a value of the SHA-1 ...
            NON_ANALYZED_OPTIONS.addFieldToDocument(name, sha1, document);
        } else if (value instanceof DateTime) {
            // ModeShape 2.x indexed timestamps as longs, enabling use of range queries.
            // Note that Hibernate Search generally uses string representations.
            DateTime timestamp = (DateTime)value;
            NON_ANALYZED_OPTIONS.addNumericFieldToDocument(name, timestamp.getMillisecondsInUtc(), document);
        } else if (value instanceof Reference) {
            Reference ref = (Reference)value;
            String strRef = ref.getString();
            if (ref.isWeak()) {
                addValueTo(strRef, weakRefs);
            } else {
                addValueTo(strRef, strongRefs);
            }
            // Now add a reference field to the document (so we can use constraints in queries) ...
            NON_ANALYZED_OPTIONS.addFieldToDocument(name, strRef, document);
        }
    }

    protected final void addValueTo( String value,
                                     AtomicReference<Set<String>> ref ) {
        assert value != null;
        assert value.length() != 0;
        Set<String> values = ref.get();
        if (values == null) {
            values = new HashSet<String>();
            ref.set(values);
        }
        values.add(value);
    }

}
