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
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.spi.index.provider.IndexProviderExistsException;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

/**
 * An interface for programmatically adding or removing index providers and index definitions.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexManager {

    /**
     * Get the names of the registered {@link IndexProvider}s.
     * 
     * @return the names of the providers; never null but possibly empty
     */
    Set<String> getProviderNames();

    /**
     * Get the query index provider registered with the given name.
     * 
     * @param name the name of the query index provider; may not be null
     * @return the query index provider instance, or null if there is no provider registered with the supplied name
     */
    IndexProvider getProvider( String name );

    /**
     * Get a map of the registered index definitions keyed by their names.
     * 
     * @return the index definitions; never null but possibly empty
     */
    Map<String, IndexDefinition> getIndexDefinitions();

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
     * Register a new definition for an index.
     * 
     * @param indexDefinition the definition; may not be null
     * @param allowUpdate true if the definition can update or ovewrite an existing definition with the same name, or false if
     *        calling this method when a definition with the same name already exists should result in an exception
     * @throws InvalidIndexDefinitionException if the new definition is invalid
     * @throws IndexExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>NodeTypeDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws RepositoryException if there is a problem registering the new definition, or if an existing index
     */
    void registerIndex( IndexDefinition indexDefinition,
                        boolean allowUpdate ) throws InvalidIndexDefinitionException, IndexExistsException, RepositoryException;

    /**
     * Register new definitions for several indexes.
     * 
     * @param indexDefinitions the definitions; may not be null
     * @param allowUpdate true if each of the definition can update or ovewrite an existing definition with the same name, or
     *        false if calling this method when a definition with the same name already exists should result in an exception
     * @throws InvalidIndexDefinitionException if the new definition is invalid
     * @throws IndexExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>NodeTypeDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws RepositoryException if there is a problem registering the new definition, or if an existing index
     */
    void registerIndexes( IndexDefinition[] indexDefinitions,
                          boolean allowUpdate ) throws InvalidIndexDefinitionException, IndexExistsException, RepositoryException;

    /**
     * Removes an existing index definition.
     * 
     * @param indexName the name of the index definition to be removed; may not be null
     * @throws NoSuchIndexException there is no index with the supplied name
     * @throws RepositoryException if there is a problem registering the new definition, or if an existing index
     */
    void unregisterIndex( String indexName ) throws NoSuchIndexException, RepositoryException;

    /**
     * Create a new template that can be used to programmatically define an index.
     * 
     * @return the new template; may not be null
     */
    IndexDefinitionTemplate createIndexDefinitionTemplate();

    /**
     * Create a new template that can be used to programmatically define a column on an index.
     * 
     * @return the new template; may not be null
     */
    IndexColumnDefinitionTemplate createIndexColumnDefinitionTemplate();

}
