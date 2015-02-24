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

package org.modeshape.jcr.spi.index;

import java.security.NoSuchProviderException;
import java.util.List;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexProviderExistsException;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;

/**
 * An interface for programmatically adding or removing index providers and index definitions.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexManager extends org.modeshape.jcr.api.index.IndexManager {

    /**
     * Get the query index provider registered with the given name.
     * 
     * @param name the name of the query index provider; may not be null
     * @return the query index provider instance, or null if there is no provider registered with the supplied name
     */
    IndexProvider getProvider( String name );

    /**
     * Register a new already-instantiated {@link IndexProvider}.
     * 
     * @param provider the new provider instance; may not be null
     * @throws IndexProviderExistsException if there is already an existing index provider with the same name
     * @throws RepositoryException if there is a problem registering the provider
     */
    void register( IndexProvider provider ) throws IndexProviderExistsException, RepositoryException;

    /**
     * Unregister the {@link IndexProvider} with the supplied name.
     * 
     * @param providerName the name of the query index provider; may not be null
     * @throws NoSuchProviderException there is no index provider with the supplied name
     * @throws RepositoryException if there is a problem unregistering the provider
     */
    void unregister( String providerName ) throws NoSuchProviderException, RepositoryException;

    /**
     * Returns a list of {@code ManagedIndex} instances for a given provider and workspace, which have a certain status.
     * 
     * @param providerName a {@link String} the name of an index provider, may not be {@code null}
     * @param workspaceName a {@link String} the name of a workspace, may not be {@code null}
     * @param status a {@link org.modeshape.jcr.api.index.IndexManager.IndexStatus} instance, may not be {@code null}
     * @return a List of {@link org.modeshape.jcr.spi.index.provider.ManagedIndex} instances, never {@code null}
     */
    List<ManagedIndex> getIndexes(String providerName, String workspaceName, IndexStatus status);
}
