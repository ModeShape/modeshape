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
package org.modeshape.jcr.query.lucene.basic;

import java.math.BigDecimal;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.TermVector;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.modeshape.jcr.query.lucene.FieldUtil;

/**
 * The Hibernate Search {@link FieldBridge} implementation that generates a field in the Lucene Document for each
 * {@link DynamicField}.
 */
public class DynamicFieldBridge implements FieldBridge {

    private static final Integer TRUE_INT = 1;
    private static final Integer FALSE_INT = 0;

    /**
     * This class always always adds fields that are {@link Index#NOT_ANALYZED}, since we want to search based up the fields
     * actual (non-analyzed) values. Only the fields used in full-text searching need to be analyzed, and those fields are handled
     * in other ways. All fields using this option are {@link org.apache.lucene.document.Field.Store#YES stored}, allowing the
     * values to be evaluated in comparison criteria.
     */
    private static final LuceneOptions STORED_NOT_ANALYZED = new LuceneOptionsImpl(Store.YES, Index.NOT_ANALYZED, TermVector.NO,
                                                                                   1.0f);

    /**
     * This class always adds fields that are {@link Index#NOT_ANALYZED}, since we want to search based up the fields
     * actual (non-analyzed) values. Only the fields used in full-text searching need to be analyzed, and those fields are handled
     * in other ways. All fields using this option are {@link org.apache.lucene.document.Field.Store#NO not stored}.
     */
    private static final LuceneOptions NOT_STORED_NOT_ANALYZED = new LuceneOptionsImpl(Store.NO, Index.NOT_ANALYZED,
                                                                                       TermVector.NO, 1.0f);

    /**
     * 
     */
    public DynamicFieldBridge() {
    }

    @Override
    public final void set( String name,
                           Object value,
                           Document document,
                           LuceneOptions luceneOptions ) {
        DynamicField field = (DynamicField)value;
        while (field != null) {
            addField(field, document, luceneOptions);
            field = field.getNext();
        }
    }

    protected final void addField( DynamicField field,
                                   Document document,
                                   LuceneOptions analyzedOptions ) {
        boolean analyzed = field.isAnalyzed();
        boolean stored = analyzed ? field.isStored() : true; // if we don't store non-analyzed fields, we can't query them
        LuceneOptions options = analyzed ? analyzedOptions : STORED_NOT_ANALYZED;
        Object value = field.getValue();
        String name = field.getFieldName();
        // Add the field value for the property ...
        if (value instanceof Object[]) {
            for (Object arrayValue : (Object[])value) {
                addField(options, name, arrayValue, document, stored);
            }
        } else {
            addField(options, name, value, document, stored);
        }
    }

    protected final void addField( LuceneOptions options,
                                   String fieldName,
                                   Object value,
                                   Document document,
                                   boolean stored ) {
        if (value instanceof String) {
            options.addFieldToDocument(fieldName, (String)value, document);
            return;
        }
        options = stored ? STORED_NOT_ANALYZED : NOT_STORED_NOT_ANALYZED;
        if (value instanceof Boolean) {
            // Boolean values are stored using integer values '1' and '0' ...
            Boolean bValue = (Boolean)value;
            Integer iValue = bValue ? TRUE_INT : FALSE_INT;
            options.addNumericFieldToDocument(fieldName, iValue, document);
        } else if (value instanceof Integer) {
            // Integer values are stored as longs ...
            options.addNumericFieldToDocument(fieldName, Long.valueOf((Integer)value), document);
        } else if (value instanceof Long) {
            // Long values are stored as longs ...
            options.addNumericFieldToDocument(fieldName, value, document);
        } else if (value instanceof Float) {
            // Float values are stored as doubles ...
            options.addNumericFieldToDocument(fieldName, Double.valueOf((Float)value), document);
        } else if (value instanceof Double) {
            // Double values are stored as doubles ...
            options.addNumericFieldToDocument(fieldName, value, document);
        } else if (value instanceof BigDecimal) {
            // Convert big decimals to a string, using a specific format ...
            String strValue = FieldUtil.decimalToString((BigDecimal)value);
            options.addFieldToDocument(fieldName, strValue, document);
        } else {
            assert value != null;
            assert false : "Unexpected field value of type " + value.getClass() + ": " + value;
        }
    }
}
