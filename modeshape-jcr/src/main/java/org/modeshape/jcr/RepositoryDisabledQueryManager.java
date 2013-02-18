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
package org.modeshape.jcr;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.hibernate.search.backend.TransactionContext;
import org.modeshape.common.util.ImmediateFuture;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.RepositoryConfiguration.QuerySystem;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.query.qom.QueryCommand;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.QueryIndexing;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Statistics;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.process.QueryResultColumns;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * An specialization of {@link RepositoryQueryManager} that is used when queries are not enabled.
 */
class RepositoryDisabledQueryManager extends RepositoryQueryManager {

    private final QueryIndexing NO_OP_INDEXING = new QueryIndexing() {
        @Override
        public void addBinaryToIndex( Binary binary,
                                      TransactionContext txnCtx ) {
        }

        @Override
        public void addToIndex( String workspace,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Collection<Property> properties,
                                NodeTypeSchemata schemata,
                                TransactionContext txnCtx ) {
        }

        @Override
        public Set<NodeKey> indexedNodes() {
            return Collections.emptySet();
        }

        @Override
        public void removeAllFromIndex( String workspace,
                                        TransactionContext txnCtx ) {
        }

        @Override
        public void removeBinariesFromIndex( Iterable<String> sha1s,
                                             TransactionContext txnCtx ) {
        }

        @Override
        public void removeFromIndex( String workspace,
                                     Iterable<NodeKey> keys,
                                     TransactionContext txnCtx ) {
        }

        @Override
        public void updateIndex( String workspace,
                                 NodeKey key,
                                 Path path,
                                 Name primaryType,
                                 Set<Name> mixinTypes,
                                 Iterator<Property> properties,
                                 NodeTypeSchemata schemata,
                                 TransactionContext txnCtx ) {
        }
    };
    private static final Future<Boolean> FINISHED_FUTURE = new ImmediateFuture<Boolean>(Boolean.TRUE);

    RepositoryDisabledQueryManager( RunningState runningState,
                                    QuerySystem querySystem ) {
        super(runningState, querySystem);
    }

    @Override
    void shutdown() {
        // do nothing ...
    }

    @Override
    public CancellableQuery query( ExecutionContext context,
                                   RepositoryCache repositoryCache,
                                   Set<String> workspaceNames,
                                   Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) {
        final QueryResultColumns resultColumns = QueryResultColumns.empty();
        Statistics stats = new Statistics();
        final QueryResults results = new org.modeshape.jcr.query.process.QueryResults(resultColumns, stats);
        return new CancellableQuery() {
            @Override
            public QueryResults getResults() {
                return results;
            }

            @Override
            public boolean cancel() {
                return false;
            }
        };
    }

    @Override
    public QueryIndexing getIndexes() {
        return NO_OP_INDEXING;
    }

    @Override
    public void reindexContent( JcrWorkspace workspace ) {
        // do nothing
    }

    @Override
    public void reindexContent( JcrWorkspace workspace,
                                Path path,
                                int depth ) {
        // do nothing
    }

    @Override
    public Future<Boolean> reindexContentAsync( JcrWorkspace workspace ) {
        return FINISHED_FUTURE;
    }

    @Override
    public Future<Boolean> reindexContentAsync( JcrWorkspace workspace,
                                                Path path,
                                                int depth ) {
        return FINISHED_FUTURE;
    }

    @Override
    protected void reindexContent( boolean indexOnlyIfMissing,
                                   boolean includeSystemContent,
                                   boolean async ) {
        // do nothing
    }

    @Override
    protected void reindexContent( String workspaceName,
                                   NodeTypeSchemata schemata,
                                   NodeCache cache,
                                   CachedNode node,
                                   int depth,
                                   boolean reindexSystemContent,
                                   Set<NodeKey> keysToExclude ) {
        // do nothing
    }

    @Override
    protected void reindexSystemContent( boolean async ) {
        // do nothing
    }

    @Override
    protected void reindexSystemContent( CachedNode nodeInSystemBranch,
                                         int depth,
                                         NodeTypeSchemata schemata,
                                         Set<NodeKey> keysToExclude ) {
        // do nothing
    }
}
