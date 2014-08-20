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

package org.modeshape.jcr.api.index;

import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * An interface for programmatically adding or removing index providers and index definitions. Even though this is accessed via
 * {@link org.modeshape.jcr.api.Workspace#getIndexManager}, like {@link NodeTypeManager} it represents a session's mechanism to
 * define, alter, or remove index definitions for the whole repository (not just the session's {@link Workspace#getName()
 * workspace}).
 * <p>
 * To create an IndexDefinition:
 * <ol>
 * <li>Call the {@link #createIndexDefinitionTemplate()} method to obtain a new blank template</li>
 * <li>Call the setter methods on the {@link IndexDefinitionTemplate} to make the template accurately describe the new or updated
 * index definition</li>
 * <li>For each of the index definition's columns, {@link #createIndexColumnDefinitionTemplate() create a column definition
 * template} and call the setters to define the {@link IndexColumnDefinitionTemplate#setPropertyName(String) property name} to
 * which the column applies and the {@link IndexColumnDefinitionTemplate#setColumnType(int) type for the column}.</li>
 * <li>Call the {@link IndexDefinitionTemplate#setColumnDefinitions(Iterable)} to assign the column definitions to the index
 * definition</li>
 * </ol>
 * The resulting {@link IndexDefinitionTemplate}, which is also an {@link IndexDefinition}, can then be passed to the
 * {@link #registerIndex(IndexDefinition, boolean)} or {@link #registerIndexes(IndexDefinition[], boolean)} methods to add/update
 * the index definition.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @see org.modeshape.jcr.api.Workspace#getIndexManager()
 */
public interface IndexManager {

    /**
     * Get the names of the available index providers.
     *
     * @return the immutable set of provider names; never null but possibly empty
     */
    Set<String> getProviderNames();

    /**
     * Get a map of the registered index definitions keyed by their names. The resulting map is immutable, but it is updated
     * whenever an index definition is added, updated, or removed. To add an index, use
     * {@link #registerIndex(IndexDefinition, boolean)} or {@link #registerIndexes(IndexDefinition[], boolean)}; to remove an
     * index, use {@link #unregisterIndexes}.
     *
     * @return the index definitions; never null but possibly empty
     */
    Map<String, IndexDefinition> getIndexDefinitions();

    /**
     * Register a new definition for an index.
     *
     * @param indexDefinition the definition; may not be null
     * @param allowUpdate true if the definition can update or ovewrite an existing definition with the same name, or false if
     *        calling this method should result in an exception when the repository already contains a definition with the same
     *        name already exists
     * @throws InvalidIndexDefinitionException if the new definition is invalid
     * @throws IndexExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>IndexDefinition</code>
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
     *        false if calling this method should result in an exception when the repository already contains any definition with
     *        names that match the supplied definitions
     * @throws InvalidIndexDefinitionException if the new definition is invalid
     * @throws IndexExistsException if <code>allowUpdate</code> is <code>false</code> and the <code>IndexDefinition</code>
     *         specifies a node type name that is already registered.
     * @throws RepositoryException if there is a problem registering the new definition, or if an existing index
     */
    void registerIndexes( IndexDefinition[] indexDefinitions,
                          boolean allowUpdate ) throws InvalidIndexDefinitionException, IndexExistsException, RepositoryException;

    /**
     * Removes an existing index definition.
     *
     * @param indexNames the names of the index definition to be removed; may not be null
     * @throws NoSuchIndexException there is no index with the supplied name
     * @throws RepositoryException if there is a problem registering the new definition, or if an existing index
     */
    void unregisterIndexes( String... indexNames ) throws NoSuchIndexException, RepositoryException;

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
