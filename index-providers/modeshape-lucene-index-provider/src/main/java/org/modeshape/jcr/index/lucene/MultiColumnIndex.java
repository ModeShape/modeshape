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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
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
                logger.debug("Updating the property '{0}' of document '{1}' in the Lucene index '{2}' with the values '{3}'", values,
                             propertyName, nodeKey, name, values);

                Document document = searcher.loadDocumentById(nodeKey);
                // remove the old values for the property from the document
                removeProperty(propertyName, document);
                // add the new values 
                addProperty(nodeKey, document, propertyName, values);
                writer.updateDocument(FieldUtil.idTerm(nodeKey), document);
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
   
    @Override
    protected void preCommit( Map<String, String> commitData ) {
        super.preCommit(commitData);
        cache.updateCommmitData(commitData);
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
            removeProperty(propertyName, document);
            if (document.getFields().isEmpty()) {
                // there are no more fields, so remove the entire document....
                writer.deleteDocuments(FieldUtil.idTerm(nodeKey));
                // mark the node key as removed
                cache.remove(nodeKey);
            } else {
                // update the document after adding back the ID (which was removed during removeProperty)
                document.add(FieldUtil.idField(nodeKey));
                writer.updateDocument(FieldUtil.idTerm(nodeKey), document);
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
    
    private void removeProperty( String propertyName, Document document ) {
        Iterator<IndexableField> fieldIterator = document.iterator();
        while (fieldIterator.hasNext()) {
            IndexableField field = fieldIterator.next();
            String name = field.name();
            if (name.equals(propertyName)) {
                // there can be multiple properties with the same name (multivalued) so we need to call remove for each one
                fieldIterator.remove();
            } else if (FieldUtil.ID.equals(name)) {
                // for some inexplicable reason (Lucene bug ??) this has to be removed and readded with each update 
                fieldIterator.remove();
            }
        }
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

        protected synchronized void updateCommmitData(Map<String, String> commitData) {
            if (!removed.isEmpty()) {
                for (String key : removed) {
                    commitData.remove(key);
                }
            }
            if (!added.isEmpty()) {
                for (String key : added) {
                    commitData.put(key, "");
                }
            }
        }
    }
}
