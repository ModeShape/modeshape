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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LegacyDoubleField;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.value.PropertyType;

/**
 * Lucene index which supports multiple heterogeneous columns for any given document. This is more complicated and performs 
 * worse in some cases than {@link SingleColumnIndex} because Lucene doesn't support updates, so this index has to deal with merging fields.
 * <p>
 * Whenever possible, prefer the {@link SingleColumnIndex} implementation to this one. 
 * </p>
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@ThreadSafe
@Immutable
@SuppressWarnings("deprecation")
class MultiColumnIndex extends LuceneIndex {

    private final DocumentIdCache cache;
    
    protected MultiColumnIndex( String name,
                                String workspaceName, 
                                LuceneConfig config,
                                Map<String, PropertyType> propertyTypesByName,
                                ExecutionContext context ) {
        super(name, workspaceName, config, propertyTypesByName, context);
        // we keep track of the node keys which are added/removed in the commit data
        // this is an optimization to avoid searching for a document each time an update or partial remove is performed
        this.cache = new DocumentIdCache();
    }

    @Override
    public void add( final String nodeKey, final String propertyName, final Object[] values ) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(values, "values");
       
        try {
            // first look at the cache and commit data to check if a document exists or not with this key. If a document does not
            // exist, this operation will be a lot faster. Otherwise the document needs to be loaded which is costly....
            // THIS IS A COSTLY OPERATION...
            if (documentExists(nodeKey)) {
                // we're updating an existing document
                logger.debug("Updating the property '{0}' of document '{1}' in the Lucene index '{2}' with the values '{3}'",
                             propertyName, nodeKey, name, values);
                Document oldDocument = searcher.loadDocumentById(nodeKey);
                Document newDocument = clone(oldDocument, propertyName);
                                            
                //add the fields for the new property
                List<Field> fields = valuesToFields(propertyName, values);
                fields.stream().forEach(newDocument::add);
                writer.updateDocument(FieldUtil.idTerm(nodeKey), newDocument);
            } else {
                // we're creating the document for the first time...
                logger.debug("Adding the document '{0}' in the Lucene Index '{1}' with the property '{2}' and values '{3}",
                             nodeKey, name, propertyName, values);                 
                Document document = new Document();
                addProperty(nodeKey, document, propertyName, values);
                writer.addDocument(document);
                // mark the node key as added
                cache.add(nodeKey);
            }
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    private Document clone(Document oldDocument, String... excludeProps) {
        List<String> excluded = Arrays.asList(excludeProps);

        Document newDocument = new Document();
        oldDocument.getFields()
                   .stream()
                   .filter((field) -> (!excluded.contains(field.name())))
                   .map((field) -> (clone((Field) field)))
                   .forEach(newDocument::add);
        return newDocument;
    }
    
    private Field clone(Field existing) {
        String name = existing.name();

        if (name.startsWith(FieldUtil.LENGTH_PREFIX)) {
            // these are always stored as longs
            return new LegacyLongField(name, Long.valueOf(existing.stringValue()), Field.Store.YES);
        }
        Number numberValue = existing.numericValue();
        if (numberValue instanceof Integer) {
            return new LegacyIntField(name, numberValue.intValue(), Field.Store.YES);
        } else if (numberValue instanceof Long) {
            return new LegacyLongField(name, numberValue.longValue(), Field.Store.YES);
        } else if (numberValue instanceof Double) {
            return new LegacyDoubleField(name, numberValue.doubleValue(), Field.Store.YES);
        }
        String stringValue = existing.stringValue();
        if (stringValue != null) {
            return new StringField(name, stringValue, Field.Store.YES);
        }
        BytesRef bytesRef = existing.binaryValue();
        if (bytesRef != null) {
            // we don't really store any binary fields
            return new StringField(name, bytesRef, Field.Store.YES);
        }
        throw new LuceneIndexException("Cannot clone existing field: " + existing);
    }
   
    @Override
    protected void preCommit( Map<String, String> commitData ) {
        super.preCommit(commitData);
        cache.updateCommitData(commitData);
    }

    @Override
    protected void remove(final String nodeKey, final String propertyName) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");
       
        if (!documentExists(nodeKey)) {
            // no document found so nothing to do
            return;
        }
        try {
            Document document = searcher.loadDocumentById(nodeKey);
            boolean hasProperty = document.getField(propertyName) != null;
            if (!hasProperty) {
                return;
            }
            Term idTerm = FieldUtil.idTerm(nodeKey);
            if (document.getFields().size() == 1) {
                // there are no more fields, so remove the entire document....
                writer.deleteDocuments(idTerm);
                // mark the node key as removed
                cache.remove(nodeKey);
            } else {
                // create a clone without the property
                Document newDocument = clone(document, propertyName);
                writer.updateDocument(idTerm, newDocument);
            }
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    @Override
    protected LuceneQueryFactory queryFactory( Map<String, Object> variables ) {
        return LuceneQueryFactory.forMultiColumnIndex(context.getValueFactories(), variables, propertyTypesByName);
    }

    @Override
    public void remove( String nodeKey ) {
        super.remove(nodeKey);
        if (documentExists(nodeKey)) {
            cache.remove(nodeKey);
        }
    }

    private boolean documentExists( String nodeKey ) {
        return cache.hasNode(nodeKey) || writer.getCommitData().containsKey(nodeKey);
    }

    /**
     * A simple holder which tracks for each index writer session the document keys which exist in the index
     * and then writes this information in the commit data. This avoids the document searching required when updating the column
     * of an existing document.
     */
    private class DocumentIdCache {
        private final Set<String> removed;
        private final Set<String> added;

        private DocumentIdCache() {
            this.removed = new HashSet<>();
            this.added = new HashSet<>();
        }

        protected synchronized boolean hasNode(String nodeKey) {
            return added.contains(nodeKey) || removed.contains(nodeKey);
        }

        protected synchronized void add(String nodeKey) {
            this.removed.remove(nodeKey);
            this.added.add(nodeKey);
        }

        protected synchronized void remove(String nodeKey) {
            this.added.remove(nodeKey);
            this.removed.add(nodeKey);
        }

        protected synchronized void clear() {
            this.added.clear();
            this.removed.clear();
        }

        protected synchronized void updateCommitData(Map<String, String> commitData) {
            removed.forEach(commitData::remove);
            added.forEach(key -> commitData.put(key, ""));
            clear();
        }
    }
}
