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
import java.io.InterruptedIOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.jcr.query.qom.Constraint;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.Bits;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.provider.Filter;

/**
 * Class which handles the actual Lucene searching for the {@link LuceneIndexProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
@ThreadSafe
public class Searcher {
    // the implicit score that will be used when no explicit scoring is requested
    protected static final float DEFAULT_SCORE = 1.0f;

    private static final Logger LOGGER = Logger.getLogger(Searcher.class);
    private static final int MAX_QUERIES_TO_CACHE = 200;
    private static final long MAX_RAM_BYTES_TO_USE = 50 * 1024L * 1024L;

    private static final Set<String> ID_FIELD_SET = Collections.singleton(FieldUtil.ID);
    
    private final SearcherManager searchManager;
    private final ScheduledExecutorService searchManagerRefreshService;
    private final ScheduledFuture<?> searchManagerRefreshResult;
    private final QueryCache queryCache;

    protected Searcher( LuceneConfig config, IndexWriter writer, String name ) {
        this.searchManager = config.searchManager(writer);
        this.queryCache = new LRUQueryCache(MAX_QUERIES_TO_CACHE, MAX_RAM_BYTES_TO_USE);
        this.searchManagerRefreshService = Executors.newScheduledThreadPool(1, new NamedThreadFactory(
                name + "-lucene-search-manager-refresher"));
        this.searchManagerRefreshResult = this.searchManagerRefreshService.scheduleWithFixedDelay(this::refreshSearchManager,
                                                                                                  0,
                                                                                                  config.refreshTimeSeconds(),
                                                                                                  TimeUnit.SECONDS);
    }
    
    protected void close() {
        try {
            searchManagerRefreshResult.cancel(false);
            searchManagerRefreshService.shutdown();
            searchManager.close();
        } catch (IOException e) {
            LOGGER.warn(e, LuceneIndexProviderI18n.warnErrorWhileClosingSearcher);
        }
    }
    
    protected Filter.Results filter(IndexConstraints indexConstraints, 
                                    LuceneQueryFactory queryFactory,
                                    long cardinalityEstimate) {
        Query query = createQueryFromConstraints(indexConstraints.getConstraints(), queryFactory);
        return new LuceneResults(query, queryFactory.scoreDocuments(), cardinalityEstimate);
    }
    
    protected long estimateCardinality( final List<Constraint> andedConstraints, final LuceneQueryFactory queryFactory ) throws IOException {
        return search(searcher -> {
            Query query = createQueryFromConstraints(andedConstraints, queryFactory);
            return (long) searcher.count(query);
        }, true);
    }
    
    protected Document loadDocumentById(final String id) throws IOException {
        // this is a potentially costly operation
        return search(searcher -> {
            DocumentByIdCollector collector = new DocumentByIdCollector();
            searcher.search(FieldUtil.idQuery(id), collector);
            return collector.document();
        }, true);
    }

    private Query createQueryFromConstraints( Collection<Constraint> andedConstraints, LuceneQueryFactory queryFactory ) {
        if (andedConstraints.isEmpty()) {
            // if there are no anded constraint but this index was called to filter results, simply return everything...
            return new MatchAllDocsQuery();
        } else if (andedConstraints.size() == 1) {
            return queryFactory.createQuery(andedConstraints.iterator().next());
        } else {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setDisableCoord(true);
            for (Constraint constraint : andedConstraints) {
                builder.add(queryFactory.createQuery(constraint), BooleanClause.Occur.MUST);
            }
            return builder.build();
        }
    }

    protected void refreshSearchManager()  {
        try {
            searchManager.maybeRefreshBlocking();
        } catch (InterruptedIOException ie) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOGGER.warn(e, LuceneIndexProviderI18n.warnErrorWhileClosingSearcher);
        }
    }

    protected <T> T search(Searchable<T> searchable, boolean refreshReader) {
        if (refreshReader) {
            refreshSearchManager();
        }
        IndexSearcher searcher = null;
        try {
            searcher = searchManager.acquire();
            searcher.setQueryCache(queryCache);
            return searchable.search(searcher);
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        } finally {
            if (searcher != null) {
                try {
                    searchManager.release(searcher);
                } catch (IOException e) {
                    LOGGER.debug(e, "Cannot release Lucene searcher");
                }
            }
        }
    }
   
    private class LuceneResults implements Filter.Results {
        
        private final boolean scoreDocuments;
        private final long size;
        
        private Query query;
        private Iterator<NodeKey> keysIterator;
        private Iterator<Float> scoresIterator;
        private int currentBatch;

        protected LuceneResults( Query query, boolean scoreDocuments, long size ) {
            this.scoreDocuments = scoreDocuments;
            this.query = query;
            this.currentBatch = 0;
            this.size = size;
        }

        @Override
        public Filter.ResultBatch getNextBatch(final int batchSize) {
            int startPosition = currentBatch++ * batchSize;
            int endPosition = (int) Math.min(size, startPosition + batchSize);
            boolean hasNextBatch = endPosition != size;
            int size = endPosition - startPosition;
            return new Filter.ResultBatch() {
                private int keysCount = 0;
                private int scoresCount = 0;
                
                @Override
                public Iterable<NodeKey> keys() {
                   return () -> new Iterator<NodeKey>() {
                       @Override
                       public boolean hasNext() {
                           if (keysCount == size) {
                               return false;
                           }
                           if (keysIterator == null) {
                               runQuery();
                           }
                           return keysIterator.hasNext();
                       }

                       @Override
                       public NodeKey next() {
                           if (keysCount++ == size) {
                               throw new NoSuchElementException();
                           }  
                           if (keysIterator == null) {
                               runQuery();
                           }
                           return keysIterator.next();
                       }
                   };
                }

                @Override
                public Iterable<Float> scores() {
                    return () -> new Iterator<Float>() {
                        @Override
                        public boolean hasNext() {
                            if (scoresCount == size) {
                                return false;
                            }
                            if (scoresIterator == null) {
                                runQuery();
                            }
                            return scoresIterator.hasNext();
                        }

                        @Override
                        public Float next() {
                            if (scoresCount++ == size) {
                                throw new NoSuchElementException();
                            }
                            if (scoresIterator == null) {
                                runQuery();
                            }
                            return scoresIterator.next();
                        }
                    };
                }

                @Override
                public boolean hasNext() {
                    return hasNextBatch;
                }

                @Override
                public int size() {
                    return size;
                }

                private void runQuery() {
                    if (keysIterator == null && scoresIterator == null) {
                        Map<NodeKey, Float> results = search(searcher -> getSearchResults(searcher), true);
                        keysIterator = results.keySet().iterator();
                        scoresIterator = results.values().iterator();
                    }
                }
            };
        }

        private Map<NodeKey, Float> getSearchResults(IndexSearcher searcher) throws IOException {
            IdsCollector collector = new IdsCollector(scoreDocuments, searcher.getIndexReader().maxDoc());
            searcher.search(query, collector);
            BitSet docIds = collector.documents();
            Map<NodeKey, Float> results = new LinkedHashMap<>();
            for (int i = docIds.nextSetBit(0); i >= 0; i = docIds.nextSetBit(i + 1)) {
                try {
                    // this is a valid document which we have to load...
                    Document document = searcher.doc(i, ID_FIELD_SET);
                    String id = document.getBinaryValue(FieldUtil.ID).utf8ToString();
                    Float score = collector.scoreFor(i);
                    results.put(new NodeKey(id), score);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return results;
        }

        @Override
        public void close() {
            keysIterator = null;
            scoresIterator = null;
            query = null;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(query.toString());
            sb.append("=").append("[").append(size).append( " keys]");
            return sb.toString();
        }
    }
    
    private static class IdsCollector extends SimpleCollector {

        private final float[] scores;
        
        private BitSet docHits;
        private Scorer scorer;
        private int docBase;
        private Bits liveDocs;

        protected IdsCollector(boolean scoring, int maxDoc) {
            this.scores = scoring ? new float[maxDoc] : null;
            this.docHits = new BitSet(maxDoc);
        }

        @Override
        protected void doSetNextReader( LeafReaderContext context ) throws IOException {
            this.docBase = context.docBase;
            this.liveDocs = context.reader().getLiveDocs();
        }

        @Override
        public void setScorer( Scorer scorer ) throws IOException {
            if (isScoring()) {
                this.scorer = scorer;
            }
        }

        @Override
        public void collect( int doc ) throws IOException {
            if (liveDocs != null && !liveDocs.get(doc)) {
                // 'doc' has been deleted, so ignore it
                return;
            }
            int docId = doc + docBase;
            if (isScoring()) {
                scores[docId] = scorer.score();
            }
            docHits.set(docId);
        }
    
        @Override
        public boolean needsScores() {
            return isScoring();
        }

        protected BitSet documents() {
            return docHits;
        }
        
        protected Float scoreFor(int docId) {
            return isScoring() ? scores[docId] : DEFAULT_SCORE;
        }
        
        private boolean isScoring() {
            return scores != null;
        }
    }
    
    @FunctionalInterface
    protected interface Searchable<T> {
        T search(IndexSearcher searcher) throws IOException;
    }
  
    private static class DocumentByIdCollector extends SimpleCollector {

        private LeafReader currentReader;
        private Document document;

        @Override
        public void collect( int doc ) throws IOException {
            if (document == null) {
                document = currentReader.document(doc);
            } else {
                throw new CollectionTerminatedException();
            }
        }

        @Override
        protected void doSetNextReader( LeafReaderContext context ) throws IOException {
            if (document != null) {
                // we already found our document, so terminate
                throw new CollectionTerminatedException();
            }
            currentReader = context.reader();
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        protected Document document() {
            return document;
        }
    }
}
