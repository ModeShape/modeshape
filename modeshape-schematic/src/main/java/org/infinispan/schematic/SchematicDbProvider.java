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
package org.infinispan.schematic;

import java.util.Collections;
import java.util.Map;

/**
 * Service interface for {@link SchematicDb} providers. A particular provider is expected to implement this interface
 * and make itself available for discovery using the standard {@link java.util.ServiceLoader} mechanism. 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface SchematicDbProvider {

    /**
     * Returns a schematic DB instance for a given alias and list of parameters
     * 
     * @param alias an alias for the DB; may not be {@code null}
     * @param parameters a {@code Map} of parameters which will be passed down to a particular provider implementation.
     * @return a {@link SchematicDb} instance or {@code null} if a particular provider does not recognize the alias
     */
    SchematicDb getDB(String alias, Map<String, ?> parameters);
    
    default SchematicDb getDB(String alias) {
        return getDB(alias, Collections.emptyMap());
    }
}
