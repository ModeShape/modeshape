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
package org.modeshape.jcr.index.lucene;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LegacyDoubleField;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.StringFactory;

/**
 * Bases class for indexes stored in Lucene
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@ThreadSafe
@Immutable
@SuppressWarnings("deprecation")
public abstract class LuceneIndex implements ProvidedIndex<Object> {

    protected final Logger logger = Logger.getLogger(getClass());
    protected final String name;
    protected final ExecutionContext context;
    protected final IndexWriter writer;
    protected final Map<String, PropertyType> propertyTypesByName;
    protected final LuceneConfig config;
    protected final StringFactory stringFactory;
    protected final Searcher searcher;
   
    protected LuceneIndex( String name,
                           String workspaceName, 
                           LuceneConfig config,
                           Map<String, PropertyType> propertyTypesByName,
                           ExecutionContext context ) {
        assert !propertyTypesByName.isEmpty();
        this.propertyTypesByName = propertyTypesByName;                
        this.name = name;
        this.context = context;
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.config = config;
        this.writer = config.newWriter(workspaceName, name);
        this.searcher = new Searcher(config, writer, name);
    }

    @Override
    public void add( String nodeKey, String propertyName, Object value ) {
        add(nodeKey, propertyName, new Object[]{value});      
    }

    @Override
    public void remove( String nodeKey ) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        try {
            // mark the nodekey as removed
            writer.deleteDocuments(FieldUtil.idTerm(nodeKey));
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    @Override
    public void remove( String nodeKey, String propertyName, Object value ) {
        // we don't really care about the value...
        remove(nodeKey, propertyName);
    }

    @Override
    public void remove( String nodeKey, String propertyName, Object[] values ) {
        // we don't really care about the values...
        remove(nodeKey, propertyName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long estimateCardinality( List<Constraint> andedConstraints, Map<String, Object> variables ) {
        try {
            return searcher.estimateCardinality(andedConstraints, queryFactory(variables));
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    @Override
    public long estimateTotalCount() {
        return writer.numDocs();
    }

    @Override
    public Results filter(IndexConstraints constraints, long cardinalityEstimate) {
        return searcher.filter(constraints, queryFactory(constraints.getVariables()), cardinalityEstimate);
    }

    @Override
    public boolean requiresReindexing() {
        try {
            return !DirectoryReader.indexExists(writer.getDirectory());
        } catch (IOException e) {
            logger.debug(e, "cannot determine if lucene index exists...");
            return false;
        }
    }

    public void commit() {
        if (!writer.hasUncommittedChanges()) {
            return;
        }
        try {
            Map<String, String> oldData = writer.getCommitData();
            Map<String, String> newData = new HashMap<>(oldData);
            preCommit(newData);
            writer.setCommitData(newData);
            writer.commit();
            postCommit();
        } catch (IOException e) {
            throw new LuceneIndexException("Cannot commit index writer", e);
        }
    }

    protected void postCommit() {
        //nothing by default     
    }

    protected void preCommit(Map<String, String> commitData) {
        commitData.put(LuceneConfig.LAST_SUCCESSFUL_COMMIT_TIME, String.valueOf(System.currentTimeMillis()));
    }

    public void shutdown( boolean destroyed ) {
        if (destroyed) {
            clearAllData();
        }
        try {
            searcher.close();
            writer.close();
        } catch (IOException e) {
            throw new LuceneIndexException("Cannot shutdown lucene index", e);
        }
    }

    public void clearAllData() {
        try {
            writer.deleteAll();
            writer.commit();
        } catch (IOException e) {
            throw new LuceneIndexException("Cannot remove all documents from the index");
        }
    }
    
    protected void addProperty( String nodeKey, Document document, String property, Object... values ) {
        if (values != null && values.length > 0) {
            // add the new fields for the given values to the document
            List<Field> fields = valuesToFields(property, values);
            for (Field field : fields) {
                document.add(field);
            }
        }
        // always add the ID (which in the case of an update is removed first)
        document.add(FieldUtil.idField(nodeKey));
    }
    
    protected abstract void remove(final String nodeKey, final String propertyName);
    
    protected abstract LuceneQueryFactory queryFactory( Map<String, Object> variables );
    
    protected List<Field> valuesToFields( String propertyName, Object... values ) {
        assert values.length > 0;
        PropertyType type = propertyTypesByName.get(propertyName);
        assert type != null;
        List<Field> fields = new ArrayList<>(); 
        for (Object value : values) {
            switch (type) {
                case NAME:
                case PATH:    
                case REFERENCE:
                case SIMPLEREFERENCE:
                case WEAKREFERENCE:
                case URI:
                case STRING: {
                    addStringField(propertyName, stringFactory.create(value), fields);
                    break;
                }
                case BOOLEAN: {
                    addBooleanField(propertyName, (Boolean)value, fields);
                    break;
                }
                case BINARY: {
                    // don't cast to anything, because depending on the index type this may be a string or a binary 
                    addBinaryField(propertyName, value, fields);
                    break;
                }
                case DATE: {
                    addDateField(propertyName, ((DateTime)value), fields);
                    break;
                }
                case DECIMAL: {
                    addDecimalField(propertyName,(BigDecimal) value, fields);
                    break;
                }
                case DOUBLE: {
                    addDoubleField(propertyName, (Double) value, fields);
                    break;
                }
                case LONG: {
                    addLongField(propertyName, (Long) value, fields);
                    break;    
                }
                default:
                    throw new LuceneIndexException("Unsupported property type: " + type);
            }
        }
        return fields;
    }
    
    protected void addStringField( String propertyName, String value, List<Field> fields ) {
        fields.add(new StringField(propertyName, value, Field.Store.YES));
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), value.length(), Field.Store.YES));
    }

    protected void addBooleanField( String propertyName, Boolean value, List<Field> fields ) {
        String valueString = stringFactory.create(value);
        int intValue = value ? 1 : 0;
        fields.add(new LegacyIntField(propertyName, intValue, Field.Store.YES));
        // add the length
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), valueString.length(), Field.Store.YES));
    } 
    
    protected void addDateField( String propertyName, DateTime value, List<Field> fields ) {
        // dates are stored as millis
        fields.add(new LegacyLongField(propertyName, value.getMilliseconds(), Field.Store.YES));
        // add the length
        String valueString = stringFactory.create(value);
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), valueString.length(), Field.Store.YES));
    }
    
    protected void addBinaryField( String propertyName, Object value, List<Field> fields ) {
        // only the length is indexed for binary values by default....
        try {
            Binary binary = (Binary) value;
            fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), binary.getSize(), Field.Store.YES));
        } catch (RepositoryException e) {
            throw new LuceneIndexException(e);
        }
    }

    protected void addDecimalField( String propertyName, BigDecimal value, List<Field> fields ) {
        // big decimals are stored as string, because Lucene doesn't support these natively...
        String stringValue = FieldUtil.decimalToString(value);
        fields.add(new StringField(propertyName, stringValue, Field.Store.YES));
        // add the length using the JCR string format
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), stringFactory.create(value).length(), Field.Store.YES));
    }

    protected void addDoubleField( String propertyName, Double value, List<Field> fields ) {
        fields.add(new LegacyDoubleField(propertyName, value, Field.Store.YES));
        // add the length
        String valueString = stringFactory.create(value);
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), valueString.length(), Field.Store.YES));
    }
    
    protected void addLongField( String propertyName, Long value, List<Field> fields ) {
        fields.add(new LegacyLongField(propertyName, value.longValue(), Field.Store.YES));
        // add the length
        String valueString = stringFactory.create(value);
        fields.add(new LegacyLongField(FieldUtil.lengthField(propertyName), valueString.length(), Field.Store.YES));
    }
}
