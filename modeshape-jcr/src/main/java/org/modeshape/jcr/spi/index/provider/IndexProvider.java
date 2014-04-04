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
package org.modeshape.jcr.spi.index.provider;

import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Logger;
import org.modeshape.jcr.spi.index.IndexDefinitionChanges;

/**
 * A component that provides access to and manages one or more indexes.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexProvider {

    /**
     * The logger instance, set via reflection
     */
    private Logger logger;

    /**
     * The name of this provider, set via reflection
     */
    private String name;

    /**
     * The name of the repository that owns this provider, set via reflection
     */
    private String repositoryName;

    private boolean initialized = false;

    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Get the name for this provider.
     * 
     * @return the name; never null
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; never null
     */
    public final String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Initialize the provider. This is called automatically by ModeShape once for each provider instance, and should not be
     * called by the provider itself.
     * <p>
     * By default this method does nothing, so it should be overridden by implementations to do a one-time initialization of any
     * internal components after all of the fields have been set during instantiation.
     * </p>
     * 
     * @throws RepositoryException if there is a problem initializing the provider fail
     */
    public void initialize() throws RepositoryException {
        // Subclasses may not necessarily call 'super.initialize(...)', but if they do then we can make this assertion ...
        assert !initialized : "The QueryIndexProvider.initialize(...) method should not be called by subclasses; ModeShape has already (and automatically) initialized the provider";
    }

    /**
     * Method called by the code calling {@link #initialize()} (typically via reflection) to signal that the initialize method is
     * completed. This method cannot be overridden by subclasses.
     */
    @SuppressWarnings( "unused" )
    private void postInitialize() {
        if (!initialized) {
            initialized = true;

            // ------------------------------------------------------------------------------------------------------------
            // Add any code here that needs to run after #initialize(...), which will be overwritten by subclasses
            // ------------------------------------------------------------------------------------------------------------
        }
    }

    /**
     * Determine whether the indexes owned by this provider must be rebuilt. This method is normally called by ModeShape upon
     * repository startup, although it may be called at other times.
     * 
     * @return true if the repository should be re-indexed and this provider's {@link #getIndexWriter() writer} be called
     */
    public abstract boolean isReindexingRequired();

    /**
     * Signal this provider that it is no longer needed and can release any resources that are being held.
     * 
     * @throws RepositoryException if there is a problem shutting down the provider
     */
    public void shutdown() throws RepositoryException {
        // do nothing by default
    }

    /**
     * Get the writer that ModeShape can use to update the indexes when repository content changes.
     * 
     * @return the index writer; may be null if the indexes are updated outside of ModeShape
     */
    public abstract IndexWriter getIndexWriter();

    /**
     * Get the queryable index with the given name.
     * 
     * @param indexName the name of the index in this provider; never null
     * @return the queryable index, or null if there is no such index
     */
    public abstract Index getIndex( String indexName );

    /**
     * Get the planner that, during the query planning/optimization phase, evaluates for a single source the AND-ed query
     * constraints and defines indexes that may be used.
     * <p>
     * This method is typically called only once after the provider has been {@link #initialize() initialized}.
     * </p>
     * 
     * @return the index planner; may not be null
     */
    public abstract IndexPlanner getIndexPlanner();

    /**
     * Signal that some of the definitions of indexes owned by this provider were changed. This method is also called upon startup
     * of this repository instance so that the provider understands the index definitions that are available. The provider should
     * adapt to these changes as best as possible.
     * 
     * @param changes the changes in the definitions; never null
     */
    public abstract void notify( IndexDefinitionChanges changes );
}
