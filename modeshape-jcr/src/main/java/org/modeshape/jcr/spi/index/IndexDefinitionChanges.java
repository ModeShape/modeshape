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

import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.api.index.IndexDefinition;

/**
 * A description of the changes to {@link IndexDefinition}s.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexDefinitionChanges {

    /**
     * Get the {@link IndexDefinition}s that were added or updated.
     * 
     * @return the map of index definitions keyed by their names; never null but possibly empty
     */
    Map<String, IndexDefinition> getUpdatedIndexDefinitions();

    /**
     * Get the names of the {@link IndexDefinition}s that were removed.
     * 
     * @return the set of removed index definition names; never null but possibly empty
     */
    Set<String> getRemovedIndexDefinitions();
}
