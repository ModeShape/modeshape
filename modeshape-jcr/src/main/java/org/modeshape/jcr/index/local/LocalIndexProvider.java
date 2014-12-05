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

package org.modeshape.jcr.index.local;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.NodeTypes.Supplier;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;
import org.modeshape.jcr.api.query.qom.Relike;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.IndexCostCalculator.Costs;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexUsage;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;

/**
 * An {@link IndexProvider} implementation that maintains indexes on the local file system using MapDB.
 * <p>
 * This provider is instantiated with:
 * <ul>
 * <li>an {@code directory} attribute, or</li>
 * <li>an {@code path} attribute <i>and</i> an {@code relativeTo} attribute</li>
 * </ul>
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class LocalIndexProvider extends IndexProvider {

    private static final Float MAX_SELECTIVITY = new Float(1.0f);
    private static final String DB_FILENAME = "local-indexes.db";

    /**
     * The directory in which the indexes are to be stored. This needs to be set, or the {@link #path} and {@link #relativeTo}
     * need to be set.
     */
    private String directory;
    /**
     * The path in which the indexes are to be stored, relative to {@link #relativeTo}. Both of these need to be set, or the
     * {@link #directory} needs to be set.
     */
    private String path;
    /**
     * The directory relative to which the {@link #path} specifies where the indexes are to be stored. Both of these need to be
     * set, or the {@link #directory} needs to be set.
     */
    private String relativeTo;
    private DB db;

    public LocalIndexProvider() {
    }

    /**
     * Get the absolute or relative path to the directory where this provider should store the indexes.
     *
     * @return the path to the directory
     */
    public String getDirectory() {
        return directory;
    }

    @Override
    protected void doInitialize() throws RepositoryException {
        if (directory == null && relativeTo != null && path != null) {
            // Try to set the directory using relativeTo and path ...
            try {
                File rel = new File(relativeTo);
                File dir = Paths.get(rel.toURI()).resolve(path).toFile();
                directory = dir.getAbsolutePath();
            } catch (RuntimeException e) {
                throw new RepositoryException(e);
            }
        }
        if (directory == null) {
            throw new RepositoryException(JcrI18n.localIndexProviderMustHaveDirectory.text(getRepositoryName()));
        }
        logger().debug("Initializing the local index provider '{0}' in repository '{1}' at: {2}", getName(), getRepositoryName(),
                       directory);

        // Find the directory and make sure it exists and we have read and write permission ...
        File dir = new File(directory);
        if (!dir.exists()) {
            // Try to make it ...
            logger().debug("Attempting to create directory for local indexes in repository '{1}' at: {0}", dir.getAbsolutePath(),
                           getRepositoryName());
            if (dir.mkdirs()) {
                logger().debug("Created directory for local indexes in repository '{1}' at: {0}", dir.getAbsolutePath(),
                               getRepositoryName());
            } else {
                logger().debug("Unable to create directory for local indexes in repository '{1}' at: {0}", dir.getAbsolutePath(),
                               getRepositoryName());
            }
        }
        if (!dir.canRead()) {
            throw new RepositoryException(JcrI18n.localIndexProviderDirectoryMustBeReadable.text(dir, getRepositoryName()));
        }
        if (!dir.canWrite()) {
            throw new RepositoryException(JcrI18n.localIndexProviderDirectoryMustBeWritable.text(dir, getRepositoryName()));
        }

        // Find the file for the indexes ...
        File file = new File(dir, DB_FILENAME);

        if (logger().isDebugEnabled()) {
            String action = file.exists() ? "Opening" : "Creating";
            logger().debug("{0} the local index provider database for repository '{1}' at: {2}", action, getRepositoryName(),
                           file.getAbsolutePath());
        }
        // Get the database ...
		DBMaker maker = DBMaker.newFileDB(file);
		maker = maker.cacheSize(100); // ensure small cache size
		
//		maker = maker.asyncWriteEnable();
//		maker = maker.asyncWriteFlushDelay(500);
		maker = maker.cacheLRUEnable();
//		maker = maker.freeSpaceReclaimQ(1);
//		maker = maker.freeSpaceReclaimQ(10);
		maker = maker.mmapFileEnable();
		maker = maker.commitFileSyncDisable();
//		maker = maker.transactionDisable();
		
        this.db = maker.make();
        logger().trace("Found the index files {0} in index database for repository '{1}' at: {2}", db.getCatalog(),
                       getRepositoryName(), file.getAbsolutePath());
    }

    @Override
    protected void postShutdown() {
        logger().debug("Shutting down the local index provider '{0}' in repository '{1}'", getName(), getRepositoryName());
        if (db != null) {
            try {
                db.commit();
                db.close();
            } finally {
                db = null;
            }
        }
    }

    @Override
    public void validateProposedIndex( ExecutionContext context,
                                       IndexDefinition defn,
                                       NodeTypes.Supplier nodeTypeSupplier,
                                       Problems problems ) {
        ManagedLocalIndexBuilder.create(context, defn, nodeTypeSupplier, null).validate(problems);
    }

    @Override
    protected ManagedIndex createIndex( final IndexDefinition defn,
                                        final String workspaceName,
                                        Supplier nodeTypesSupplier,
                                        NodeTypePredicate matcher,
                                        IndexFeedback feedback ) {
        ManagedLocalIndexBuilder<?> builder = ManagedLocalIndexBuilder.create(context(), defn, nodeTypesSupplier, matcher);
        logger().debug("Index provider '{0}' is creating index in workspace '{1}': {2}", getName(), workspaceName, defn);
        final ManagedLocalIndex index = builder.build(workspaceName, db);
        // this is a new [index definition, workspace] pair so we should always scan
        feedback.scan(workspaceName, new IndexFeedback.IndexingCallback() {

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void beforeIndexing() {
                logger().debug(
                        "Disabling index '{0}' from provider '{1}' in workspace '{2}' while it is reindexed. It will not be used in queries until reindexing has completed",
                        defn.getName(), defn.getProviderName(), workspaceName);
                index.enable(false);
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void afterIndexing() {
                index.enable(true);
                logger().debug("Enabled index '{0}' from provider '{1}' in workspace '{2}' after reindexing has completed",
                               defn.getName(), defn.getProviderName(), workspaceName);
            }
        });
        return index;
    }

    @Override
    protected ManagedIndex updateIndex( IndexDefinition oldDefn,
                                        final IndexDefinition updatedDefn,
                                        ManagedIndex existingIndex,
                                        final String workspaceName,
                                        Supplier nodeTypesSupplier,
                                        NodeTypePredicate matcher,
                                        IndexFeedback feedback ) {
        if (!isChanged(oldDefn, updatedDefn)) {
            // Nothing about the index definition that we care about really changed, so don't do anything ...
            logger().debug("Index provider '{0}' is not updating index in workspace '{1}' because there were no changes: {2}",
                           getName(), workspaceName, updatedDefn);
            return existingIndex;
        }
        // This is very crude, but we'll just destroy the old index and rebuild the new one ...
        existingIndex.shutdown(true);
        ManagedLocalIndexBuilder<?> builder = ManagedLocalIndexBuilder.create(context(), updatedDefn, nodeTypesSupplier, matcher);
        logger().debug("Index provider '{0}' is updating index in workspace '{1}': {2}", getName(), workspaceName, updatedDefn);
        final ManagedLocalIndex index = builder.build(workspaceName, db);
        if (index.isNew()) {
            feedback.scan(workspaceName, new IndexFeedback.IndexingCallback() {

                @SuppressWarnings( "synthetic-access" )
                @Override
                public void beforeIndexing() {
                    logger().debug("Disabling index '{0}' from provider '{1}' in workspace '{2}' while it is reindexed. It will not be used in queries until reindexing has completed",
                                   updatedDefn.getName(), updatedDefn.getProviderName(), workspaceName);
                    index.enable(false);
                }

                @SuppressWarnings( "synthetic-access" )
                @Override
                public void afterIndexing() {
                    index.enable(true);
                    logger().debug("Enabled index '{0}' from provider '{1}' in workspace '{2}' after reindexing has completed",
                                   updatedDefn.getName(), updatedDefn.getProviderName(), workspaceName);
                }
            });
        }
        return index;
    }

    @Override
    protected void removeIndex( IndexDefinition oldDefn,
                                ManagedIndex existingIndex,
                                String workspaceName ) {
        logger().debug("Index provider '{0}' is removing index in workspace '{1}': {2}", getName(), workspaceName, oldDefn);
        existingIndex.shutdown(true);
    }

    private boolean isChanged( IndexDefinition defn1,
                               IndexDefinition defn2 ) {
        if (defn1.getKind() != defn2.getKind()) return true;
        if (defn1.size() != defn2.size()) return true;
        for (int i = 0; i != defn1.size(); ++i) {
            IndexColumnDefinition col1 = defn1.getColumnDefinition(i);
            IndexColumnDefinition col2 = defn2.getColumnDefinition(i);
            if (isChanged(col1, col2)) return true;
        }
        // We don't care about any properties ...
        return false;
    }

    private boolean isChanged( IndexColumnDefinition defn1,
                               IndexColumnDefinition defn2 ) {
        if (defn1.getColumnType() != defn2.getColumnType()) return true;
        if (!defn1.getPropertyName().equals(defn2.getPropertyName())) return true;
        return false;
    }

    @Override
    protected void planUseOfIndex( QueryContext context,
                                   IndexCostCalculator calculator,
                                   String workspaceName,
                                   ManagedIndex index,
                                   final IndexDefinition defn ) {
        ManagedLocalIndex localIndex = (ManagedLocalIndex)index;
        IndexUsage planner = new IndexUsage(context, calculator, defn) {
            @Override
            protected boolean applies( FullTextSearch search ) {
                // We don't support full text search criteria ...
                return false;
            }

            @Override
            protected boolean indexAppliesTo( Relike constraint ) {
                // Relike can only work if the column types are all strings ...
                for (IndexColumnDefinition columnDefn : defn) {
                    if (columnDefn.getColumnType() != PropertyType.STRING) return false;
                }
                return super.indexAppliesTo(constraint);
            }

            @Override
            protected boolean indexAppliesTo( Comparison constraint ) {
                if (QueryObjectModelConstants.JCR_OPERATOR_LIKE.equals(constraint.getOperator())) {
                    // Our indexes don't handle LIKE operations ...
                    return false;
                }
                return super.indexAppliesTo(constraint);
            }

        };
        // Does this index apply to any of the ANDed constraints?
        for (Constraint constraint : calculator.andedConstraints()) {
            if (planner.indexAppliesTo(constraint)) {
                logger().trace("Index '{0}' in '{1}' provider applies to query in workspace '{2}' with constraint: {3}",
                               defn.getName(), getName(), workspaceName, constraint);
                // The index does apply to this constraint ...
                long cardinality = localIndex.estimateCardinality(constraint, context.getVariables());
                long total = localIndex.estimateTotalCount();
                Float selectivity = null;
                if (total >= 0L) {
                    double ratio = (double)cardinality / (double)total;
                    selectivity = cardinality <= total ? new Float(ratio) : MAX_SELECTIVITY;
                }
                calculator.addIndex(defn.getName(), workspaceName, getName(), Collections.singleton(constraint), Costs.LOCAL,
                                    cardinality, selectivity);
            }
        }

        // Does this index apply to any of the join conditions ...
        for (JoinCondition joinCondition : calculator.joinConditions()) {
            if (planner.indexAppliesTo(joinCondition)) {
                logger().trace("Index '{0}' in '{1}' provider applies to query in workspace '{2}' with constraint: {3}",
                               defn.getName(), getName(), workspaceName, joinCondition);
                // The index does apply to this constraint, but the number of values corresponds to the total number of values
                // in the index (this is a JOIN CONDITON for which there is no literal values) ...
                long total = localIndex.estimateTotalCount();
                calculator.addIndex(defn.getName(), workspaceName, getName(), Collections.singleton(joinCondition), Costs.LOCAL,
                                    total);
            }

        }
    }
}
