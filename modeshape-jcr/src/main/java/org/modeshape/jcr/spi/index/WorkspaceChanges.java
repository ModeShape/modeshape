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

import java.util.Collection;
import java.util.Set;
import org.modeshape.jcr.api.index.IndexDefinition;

/**
 * A description of the changes in workspaces applicable to {@link IndexDefinition}s.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface WorkspaceChanges {

    /**
     * Get the {@link IndexDefinition}s.
     * 
     * @return the index definitions; never null
     */
    Collection<IndexDefinition> getIndexDefinitions();

    /**
     * Get the names of the new workspaces that were added.
     * 
     * @return the added workspace names; never null but possibly empty
     */
    Set<String> getAddedWorkspaces();

    /**
     * Get the names of the existing workspaces that were removed.
     * 
     * @return the removed workspace names; never null but possibly empty
     */
    Set<String> getRemovedWorkspaces();

}
