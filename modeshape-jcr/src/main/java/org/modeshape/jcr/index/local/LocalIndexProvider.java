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
import javax.jcr.RepositoryException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.NodeTypes.Supplier;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.query.qom.ChildCount;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexUsage;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;

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
    private IndexUpdater indexUpdater;

    /**
     * A bunch of MapDB specific options which can be used to further tweak this provider
     */
    private boolean cacheLRUEnable = false;
    private boolean mmapFileEnable = false;
    private boolean commitFileSyncDisable = false;
    private boolean transactionDisable = false;
    private boolean asyncWrite = false;
    private Integer cacheSize;
    
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
        DBMaker<?> dbMaker = DBMaker.newFileDB(file);
        if (this.cacheSize != null) {
            dbMaker.cacheSize(cacheSize);
            logger().debug("MapDB cache size set to {0} for index provider {1}", cacheSize, getName());
        }
        if (this.cacheLRUEnable) {
            dbMaker.cacheLRUEnable();
            logger().debug("MapDB cacheLRU enabled for index provider {0}", getName());
        }
        if (this.mmapFileEnable) {
            dbMaker.mmapFileEnableIfSupported();
            logger().debug("MapDB mmapFiles enabled for index provider {0}", getName());
        }
        if (this.commitFileSyncDisable) {
            dbMaker.commitFileSyncDisable();
            logger().debug("MapDB commitFileSync enabled for index provider {0}", getName());
        }
        if (this.transactionDisable) {
            dbMaker.transactionDisable();
            logger().debug("MapDB transactions disabled for index provider {0}", getName());
        }
        if (this.asyncWrite) {
            dbMaker.asyncWriteEnable();
            logger().debug("MapDB async writes enabled for index provider {0}", getName());
        }
        // we always want to have the close via the shutdown hook; it should be idempotent
        dbMaker.closeOnJvmShutdown();
        this.db = dbMaker.make();
        this.indexUpdater = new IndexUpdater(db);
        
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
    public Long getLatestIndexUpdateTime() {
        return indexUpdater.latestIndexUpdateTime();         
    }

    @Override
    public void validateProposedIndex( ExecutionContext context,
                                       IndexDefinition defn,
                                       NodeTypes.Supplier nodeTypeSupplier,
                                       Problems problems ) {
        // first perform some custom validations
        LocalIndexBuilder.validate(defn, problems);
    }
    
    @Override
    protected ManagedIndexBuilder getIndexBuilder( IndexDefinition defn, 
                                                   String workspaceName,
                                                   Supplier nodeTypesSupplier,
                                                   NodeTypePredicate matcher ) {
        return LocalIndexBuilder.create(context(), defn, nodeTypesSupplier, workspaceName, matcher, db);
    }

    @Override
    protected IndexUsage evaluateUsage( QueryContext context, IndexCostCalculator calculator, IndexDefinition defn ) {
        return new IndexUsage(context, calculator, defn) {
            @Override
            protected boolean applies( FullTextSearch search ) {
                // We don't support full text search criteria ...
                return false;
            }

            @Override
            protected boolean indexAppliesTo( Comparison constraint ) {
                if (QueryObjectModelConstants.JCR_OPERATOR_LIKE.equals(constraint.getOperator())) {
                    // Our indexes don't handle LIKE operations ...
                    return false;
                }
                return super.indexAppliesTo(constraint);
            }

            @Override
            protected boolean applies( ChildCount operand ) {
                // this index can't handle this
                return false;
            }
        };
    }
}
