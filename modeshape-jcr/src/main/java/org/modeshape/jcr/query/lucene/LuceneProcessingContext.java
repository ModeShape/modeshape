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
package org.modeshape.jcr.query.lucene;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.search.SearchFactory;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.query.QueryContext;

@ThreadSafe
public class LuceneProcessingContext {
    private final String repositoryName;
    private final String workspaceName;
    private final SearchFactory searchFactory;
    private final Map<String, IndexReader> readerByIndexName = new HashMap<String, IndexReader>();
    private final Map<String, IndexSearcher> searcherByIndexName = new HashMap<String, IndexSearcher>();
    private final Lock lock = new ReentrantLock();
    private final QueryContext queryContext;
    private final LuceneQueryFactory queryFactory;

    protected LuceneProcessingContext( QueryContext queryContext,
                                       String repositoryName,
                                       String workspaceName,
                                       SearchFactory searchFactory ) {
        assert queryContext != null;
        assert searchFactory != null;
        assert repositoryName != null;
        assert workspaceName != null;
        this.queryContext = queryContext;
        this.searchFactory = searchFactory;
        this.repositoryName = repositoryName;
        this.workspaceName = workspaceName;
        this.queryFactory = new LuceneQueryFactory(queryContext);
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
     * @return workspaceName
     */
    public String getWorkspaceName() {
        return workspaceName;
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
