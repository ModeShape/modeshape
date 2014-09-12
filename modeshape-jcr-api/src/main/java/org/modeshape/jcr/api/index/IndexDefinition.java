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
import java.util.NoSuchElementException;

/**
 * An immutable description of an index definition.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexDefinition extends Iterable<IndexColumnDefinition> {

    /**
     * The kind of indexes.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static enum IndexKind {
        VALUE,
        UNIQUE_VALUE,
        ENUMERATED_VALUE,
        TEXT,
        NODE_TYPE,
    }

    /**
     * Get the name of this index.
     *
     * @return the name of this index; never null
     */
    String getName();

    /**
     * Get the name of the provider that owns this index.
     *
     * @return the provider name; never null
     */
    String getProviderName();

    /**
     * Get the kind of index.
     *
     * @return the index kind; never null
     */
    IndexKind getKind();

    /**
     * Get the name of the node type for which this index applies.
     *
     * @return the node type name; never null
     */
    String getNodeTypeName();

    /**
     * Get the description of this index.
     *
     * @return the description; never null
     */
    String getDescription();

    /**
     * Determine whether this index is currently enabled.
     *
     * @return true if this index is enabled and active, or false if it is not currently being used
     */
    boolean isEnabled();

    /**
     * Determine whether this index is updated synchronously. If true, then its indexes are updated within the scope of the save
     * operations. Otherwise, the indexes are updated asynchronously.
     * 
     * @return true if the index is synchronously updated, or false otherwise
     */
    boolean isSynchronous();

    /**
     * Determine whether this index has a single column.
     *
     * @return true if this index has a single column, or false if it has more
     */
    boolean hasSingleColumn();

    /**
     * Get the number of columns in this index.
     *
     * @return the number of columns; always positive
     */
    int size();

    /**
     * Get the definition for the index column at the given 0-based position.
     *
     * @param position the 0-based position; must be less than {@link #size()}
     * @return the index definition
     * @throws NoSuchElementException if the position is negative or greater than or equal to {@link #size()}.
     */
    IndexColumnDefinition getColumnDefinition( int position ) throws NoSuchElementException;

    /**
     * Get the index property with the given name. These properties are usually non-standard.
     *
     * @param propertyName the property name; may not be null
     * @return the value or values of the property, or null if there is no property with that name
     */
    Object getIndexProperty( String propertyName );

    /**
     * Get the index property values keyed by their name. These properties are usually non-standard.
     *
     * @return the properties; never null but possibly empty
     */
    Map<String, Object> getIndexProperties();

    /**
     * Get the rule that defines the workspaces to which this index definition applies.
     *
     * @return the matching rule for the workspace names; never null
     */
    WorkspaceMatchRule getWorkspaceMatchRule();

    /**
     * Determine if this index definition has a column that applies to the named property. This is a convenience method that is
     * equivalent to:
     *
     * <pre>
     * for (IndexColumnDefinition columnDefn : this) {
     *     if (columnDefn.getPropertyName().equals(propertyName)) return true;
     * }
     * return false;
     * </pre>
     *
     * @param propertyName the name of the property
     * @return true if this definition contains a column that applies to a property with the given name
     */
    boolean appliesToProperty( String propertyName );

    public static interface WorkspaceMatchRule {

        /**
         * Determine if the index applies to the workspace with the given name.
         *
         * @param workspaceName the name of the workspace; may not be null
         * @return true if the index applies to the workspace, or false if the rule does not match the name
         */
        boolean usedInWorkspace( String workspaceName );

        /**
         * Get the string representation of this rule.
         *
         * @return the string representation of this rule; never null
         */
        String getDefinition();
    }
}
