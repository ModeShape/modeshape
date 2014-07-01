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
import javax.jcr.RepositoryException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.jcr.NodeTypes.Supplier;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;

/**
 * An {@link IndexProvider} implementation that maintains indexes on the local file system using MapDB.
 * <p>
 * This provider maintains a separate
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class LocalIndexProvider extends IndexProvider {

    private static final String DB_FILENAME = "indexes.db";

    private String directory;
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
        assert directory != null;

        // Find the directory and make sure it exists and we have read and write permission ...
        File dir = new File(directory);
        if (!dir.exists()) {
            // Try to make it ...
            logger().debug("Attempting to create directory for local indexes in repository '{1}' at: {0}", dir.getAbsolutePath(),
                           getRepositoryName());
            dir.mkdirs();
        }
        if (!dir.canRead()) {
            throw new RepositoryException("The directory for local indexes at '{0}' in repository '{1}' must be readable.");
        }
        if (!dir.canWrite()) {
            throw new RepositoryException("The directory for local indexes at '{0}' in repository '{1}' must be writable.");
        }

        // Find the file for the indexes ...
        File file = new File(dir, DB_FILENAME);

        // Create the database ...
        this.db = DBMaker.newFileDB(file).make();
    }

    @Override
    protected void postShutdown() {
        if (db != null) {
            try {
                db.close();
            } finally {
                db = null;
            }
        }
    }

    @Override
    public IndexPlanner getIndexPlanner() {
        return null;
    }

    @Override
    protected ManagedIndex createIndex( IndexDefinition defn,
                                        String workspaceName,
                                        Supplier nodeTypesSupplier,
                                        IndexFeedback feedback ) {
        // Filter filter = null;
        // ChangeSetListener listener = createListener(defn, nodeTypesSupplier);
        // LocalIndex<?> index = null;
        // IndexSpec spec = IndexSpec.create(context(), defn);
        // switch (defn.getKind()) {
        // case DUPLICATES:
        // // index = LocalDuplicateIndex.create(defn.getName(), workspaceName, getName(), db, spec);
        // index = LocalDuplicateIndex.create(defn.getName(), workspaceName, getName(), db, spec.getConverter(),
        // spec.getSerializer(), spec.getComparator());
        // break;
        // case UNIQUE:
        // index = LocalUniqueIndex.create(defn.getName(), workspaceName, getName(), db, converter, treeSerializer);
        // break;
        // case ENUMERATED:
        // case NODETYPE:
        // case FULLTEXTSEARCH:
        // break;
        // }
        //
        // LocalIndexInfo info = new LocalIndexInfo(index);
        // return operations(filter, listener, info);
        return null;
    }

    @Override
    protected ManagedIndex updateIndex( IndexDefinition oldDefn,
                                        IndexDefinition updatedDefn,
                                        ManagedIndex existingIndex,
                                        String workspaceName,
                                        Supplier nodeTypesSupplier,
                                        IndexFeedback feedback ) {
        return null;
    }

    @Override
    protected void removeIndex( IndexDefinition oldDefn,
                                ManagedIndex existingIndex,
                                String workspaceName ) {
    }
}
