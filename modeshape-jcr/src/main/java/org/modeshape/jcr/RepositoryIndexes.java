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

package org.modeshape.jcr;

import java.util.Collections;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.index.IndexDefinition;

/**
 * An immutable set of {@link IndexDefinition} instances describing all of the currently available indexes.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public abstract class RepositoryIndexes {

    /**
     * A {@link RepositoryIndexes} implementation that contains no indexes.
     */
    public static final RepositoryIndexes NO_INDEXES = new RepositoryIndexes() {
        private final Map<String, IndexDefinition> defns = Collections.emptyMap();

        @Override
        public Iterable<IndexDefinition> indexesFor( String nodeTypeName,
                                                     String providerName ) {
            return null;
        }

        @Override
        public Map<String, IndexDefinition> getIndexDefinitions() {
            return defns;
        }

        @Override
        public boolean hasIndexDefinitions() {
            return false;
        }
    };

    /**
     * Determine whether there is at least one index definition for the repository.
     *
     * @return true if there is at least one index definition, or false if there are none
     */
    public abstract boolean hasIndexDefinitions();

    /**
     * Get a map of the registered index definitions keyed by their names.
     *
     * @return the index definitions; never null but possibly empty
     */
    public abstract Map<String, IndexDefinition> getIndexDefinitions();

    /**
     * Get all of the index definitions that deal with properties on the node type with the given name. Note that the result will
     * implicitly include all indexes defined on the named node type or subtypes of the named node type.
     *
     * @param nodeTypeName the name of the node type for which the indexes are to be obtained; may not be null
     * @param providerName the name of the provider that owns the indexes; may not be null
     * @return the iterable collection of index definitions; may be null or empty if there are none
     */
    public abstract Iterable<IndexDefinition> indexesFor( String nodeTypeName,
                                                          String providerName );
}
