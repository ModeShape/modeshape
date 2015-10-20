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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.lucene.search.CachingWrapperQuery;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.ResultWriter;
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
    
    private final SearcherManager searchManager;
    private final ScheduledExecutorService searchManagerRefreshService;
    private final ScheduledFuture<?> searchManagerRefreshResult;
    private final QueryCache queryCache;

    protected Searcher( LuceneConfig config, IndexWriter writer, String name ) {
        this.searchManager = config.searchManager(writer);
        this.queryCache = new LRUQueryCache(200, 50);
        this.searchManagerRefreshService = Executors.newScheduledThreadPool(1, new NamedThreadFactory(
                name + "-lucene-search-manager-refresher"));
        this.searchManagerRefreshResult = this.searchManagerRefreshService.scheduleWithFixedDelay(new SearchManagerRefresher(),
                                                                                                  0,
                                                                                                  config.refreshTimeSeconds(),
                                                                                                  TimeUnit.SECONDS);
    }
    
    protected void close() {
        try {
            searchManagerRefreshResult.cancel(true);
            searchManagerRefreshService.shutdown();
            searchManager.close();
        } catch (IOException e) {
            LOGGER.warn(e, LuceneIndexProviderI18n.warnErrorWhileClosingSearcher);
        }
    }
    
    protected Filter.Results filter( IndexConstraints indexConstraints, LuceneQueryFactory queryFactory) {
        Query query = createQueryFromConstraints(indexConstraints.getConstraints(), queryFactory);
        return new LuceneResults(query, queryFactory.scoreDocuments());
    }
    
    protected long estimateCardinality( final List<Constraint> andedConstraints, final LuceneQueryFactory queryFactory ) throws IOException {
        return search(new Searchable<Long>() {
            @Override
            public Long search( IndexSearcher searcher ) throws IOException {
                Query query = createQueryFromConstraints(andedConstraints, queryFactory);
                TotalHitCountCollector results = new TotalHitCountCollector();
                searcher.search(query, results);
                return (long)results.getTotalHits();
            }
        }, false, true);
    }
    
    protected Document loadDocumentById(final String id) throws IOException {
        // this is a potentially costly operation
        return search(new Searchable<Document>() {
            @Override
            public Document search( IndexSearcher searcher ) throws IOException {
                DocumentByIdCollector collector = new DocumentByIdCollector();
                searcher.search(FieldUtil.idQuery(id), collector);
                return collector.document();
            }
        }, false, true);
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

    protected void refreshSearchManager(boolean async)  {
        try {
            if (!async) {
                searchManager.maybeRefreshBlocking();
            } else if (!searchManager.maybeRefresh()){
                LOGGER.debug("Attempted to perform a search manager refresh, but another thread is already doing this.");
            }
        } catch (IOException e) {
            LOGGER.warn(e, LuceneIndexProviderI18n.warnErrorWhileClosingSearcher);
        }
    }

    protected <T> T search(Searchable<T> searchable, boolean useScoring, boolean refreshReader) {
        if (refreshReader) {
            refreshSearchManager(false);
        }
        IndexSearcher searcher = null;
        try {
            try {
                searcher = searchManager.acquire();
                if (!useScoring) {
                    searcher.setQueryCache(queryCache);
                }
                return searchable.search(searcher);
            } finally {
                if (searcher != null) {
                    searchManager.release(searcher);
                }
            }
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    private class LuceneResults implements Filter.Results {
        
        private final Query query;
        private final boolean scoreDocuments;
        
        private int currentBatch;
        private boolean runQuery;
        private List<Float> scores;
        private List<NodeKey> ids;
        private int size;

        protected LuceneResults( Query query, boolean scoreDocuments ) {
            this.scoreDocuments = scoreDocuments;
            this.query = scoreDocuments ? query : new CachingWrapperQuery(query, QueryCachingPolicy.ALWAYS_CACHE);
            this.currentBatch = 0;
            this.runQuery = true;
            this.scores = new ArrayList<>();
            this.ids = new ArrayList<>();
        }

        @Override
        public boolean getNextBatch( final ResultWriter writer, final int batchSize ) {
            if (runQuery) {
                search(new Searchable<Void>() {
                    @Override
                    public Void search( IndexSearcher searcher ) throws IOException {
                        IdsCollector collector = new IdsCollector(scoreDocuments);
                        searcher.search(query, collector);
                        for (Map.Entry<NodeKey, Float> entry : collector.getScoresById().entrySet()) {
                            ids.add(entry.getKey());
                            scores.add(entry.getValue());
                        }
                        size = ids.size();
                        runQuery = false;
                        return null;
                    }
                }, scoreDocuments, true);
            }

            int startPosition = currentBatch * batchSize;
            int endPosition = Math.min(size, startPosition + batchSize);
            for (int i = startPosition; i < endPosition; i++) {
                writer.add(ids.get(i), scores.get(i));
            }
            ++currentBatch;
            return endPosition < size;
        }

        @Override
        public void close() {
            scores.clear();
            ids.clear();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(query.toString());
            sb.append("=").append("[");
            for (int i = 0; i < size; i++) {
                sb.append("(").append(ids.get(i)).append(", ").append(scores.get(i)).append(")");
                if (i < size - 1) {
                    sb.append(", ");
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }
    
    private static class IdsCollector extends SimpleCollector {

        // a set which contains the ID field which is the only field we want to load
        private static final Set<String> ID_FIELD_SET = Collections.singleton(FieldUtil.ID);
        
        private final boolean useScore;
        private final Map<NodeKey, Float> scoresById;

        private LeafReader currentReader;
        private Scorer scorer;

        protected IdsCollector( boolean useScore ) {
            this.useScore = useScore;
            this.scoresById = new LinkedHashMap<>();
        }

        @Override
        protected void doSetNextReader( LeafReaderContext context ) throws IOException {
            currentReader = context.reader();
        }

        @Override
        public void setScorer( Scorer scorer ) throws IOException {
            if (useScore) {
                this.scorer = scorer;
            }
        }

        @Override
        public void collect( int doc ) throws IOException {
            // this is a valid document which we have to load...
            Document document = currentReader.document(doc, ID_FIELD_SET);
            if (document == null) {
                return;
            }
            String id = document.getBinaryValue(FieldUtil.ID).utf8ToString();
            Float score = useScore ? scorer.score() : DEFAULT_SCORE;
            scoresById.put(new NodeKey(id), score);
        }
        

        @Override
        public boolean needsScores() {
            return useScore;
        }

        protected Map<NodeKey, Float> getScoresById() {
            return scoresById;
        }
    }
    
    protected interface Searchable<T> {
        T search(IndexSearcher searcher) throws IOException;
    }
    
    protected class SearchManagerRefresher implements Runnable {
        @Override
        public void run() {
            refreshSearchManager(true);
        }
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
