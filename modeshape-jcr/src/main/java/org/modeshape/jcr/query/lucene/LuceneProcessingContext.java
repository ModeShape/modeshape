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
package org.modeshape.jcr.query.lucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.search.SearchFactory;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.query.QueryContext;

@ThreadSafe
public class LuceneProcessingContext {
    private final String repositoryName;
    private final SearchFactory searchFactory;
    private final Map<String, IndexReader> readerByIndexName = new HashMap<String, IndexReader>();
    private final Map<String, IndexSearcher> searcherByIndexName = new HashMap<String, IndexSearcher>();
    private final Lock lock = new ReentrantLock();
    private final QueryContext queryContext;
    private final LuceneQueryFactory queryFactory;

    protected LuceneProcessingContext( QueryContext queryContext,
                                       String repositoryName,
                                       SearchFactory searchFactory,
                                       LuceneSchema schema ) {
        assert queryContext != null;
        assert searchFactory != null;
        assert repositoryName != null;
        this.queryContext = queryContext;
        this.searchFactory = searchFactory;
        this.repositoryName = repositoryName;
        this.queryFactory = schema.createLuceneQueryFactory(queryContext, searchFactory);
    }

    /**
     * @return queryContext
     */
    public QueryContext getQueryContext() {
        return queryContext;
    }

    /**
     * @return queryFactory
     */
    public LuceneQueryFactory getQueryFactory() {
        return queryFactory;
    }

    /**
     * Return the name of each workspace to be queried, or an empty set if all the workspaces should be queried.
     * 
     * @return workspaceNames the workspace names; never null
     */
    public Set<String> getWorkspaceNames() {
        return queryContext.getWorkspaceNames();
    }

    /**
     * @return repositoryName
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get an IndexReader for the Lucene index with the supplied name. Note that this method is thread-safe such that there will
     * only be one IndexReader instance for a given named index.
     * 
     * @param indexName the name of the index; may not be null
     * @return the IndexReader; never null
     * @throws LuceneException if the index manager to which the named index belongs failed to start
     */
    public IndexReader getReader( String indexName ) throws LuceneException {
        assert indexName != null;
        IndexReader reader = readerByIndexName.get(indexName);
        if (reader == null) {
            try {
                lock.lock();
                reader = readerByIndexName.get(indexName);
                if (reader == null) {
                    try {
                        reader = searchFactory.getIndexReaderAccessor().open(indexName);
                    } catch (org.hibernate.search.SearchException e) {
                        throw new LuceneException(e);
                    }
                    readerByIndexName.put(indexName, reader);
                }
            } finally {
                lock.unlock();
            }
        }
        return reader;
    }

    /**
     * Get an IndexSearcher for the Lucene index with the supplied name. Note that this method is thread-safe such that there will
     * only be one IndexSearcher instance (and underlying IndexSearcher instance) for a given named index.
     * 
     * @param indexName the name of the index; may not be null
     * @return the IndexSearcher; never null
     * @throws LuceneException if the index manager to which the named index belongs failed to start
     */
    public IndexSearcher getSearcher( String indexName ) throws LuceneException {
        IndexSearcher searcher = searcherByIndexName.get(indexName);
        if (searcher == null) {
            try {
                lock.lock();
                searcher = searcherByIndexName.get(indexName);
                if (searcher == null) {
                    searcher = new IndexSearcher(getReader(indexName));
                    searcherByIndexName.put(indexName, searcher);
                }
            } finally {
                lock.unlock();
            }
        }
        return searcher;
    }

    /**
     * Close all of the readers and searchers that are associated with this processing context.
     */
    public void close() {
        // Make sure we always attempt to close all readers even if there is a problem with one of them ...
        RuntimeException firstError = null;
        try {
            lock.lock();
            for (Map.Entry<String, IndexReader> entry : readerByIndexName.entrySet()) {
                String indexName = entry.getKey();
                IndexReader reader = entry.getValue();
                assert reader != null;
                try {
                    searchFactory.getIndexReaderAccessor().close(reader);
                } catch (RuntimeException e) {
                    if (firstError == null) firstError = e;
                    Logger.getLogger(getClass()).error(firstError,
                                                       JcrI18n.errorClosingLuceneReaderForIndex,
                                                       repositoryName,
                                                       indexName);
                }
            }
            readerByIndexName.clear();
            searcherByIndexName.clear();
        } finally {
            lock.unlock();
        }
        if (firstError != null) throw firstError;
    }
}
