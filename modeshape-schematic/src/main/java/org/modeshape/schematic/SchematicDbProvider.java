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
package org.modeshape.schematic;

import org.modeshape.schematic.document.Document;

/**
 * Service interface for {@link SchematicDb} providers. A particular provider is expected to implement this interface
 * and make itself available for discovery using the standard {@link java.util.ServiceLoader} mechanism. 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface SchematicDbProvider<T extends SchematicDb> {

    /**
     * Returns a schematic DB instance for a given db type and configuration document
     * 
     * @param type the type of DB; may not be {@code null}
     * @param configuration a {@code Document} instance which contains the configuration of a particular DB.
     * @return a {@link SchematicDb} instance or {@code null} if a particular provider does not recognize the type.
     * 
     * @throws RuntimeException if a DB provider recognizes the type, but fails in an expected way to provide a valid DB 
     * instance.
     */
    T getDB(String type, Document configuration);
}
