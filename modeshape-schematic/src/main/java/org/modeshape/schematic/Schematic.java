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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import org.modeshape.schematic.internal.InMemorySchemaLibrary;

public class Schematic extends DocumentFactory {

    /**
     * Returns a DB with the given alias and list of parameters, by delegating to all the available {@link SchematicDbProvider} 
     * services using the default CL of this class.
     * 
     * @see #getDb(String, Map, ClassLoader) 
     */
    public static SchematicDb getDb(String alias, Map<String, ?> parameters) throws RuntimeException {
        return getDb(alias, parameters, SchematicDb.class.getClassLoader());
    }
    
    /**
     * Returns a DB with the given alias and list of parameters, by delegating to all the available {@link SchematicDbProvider} 
     * services.
     *
     * @param alias a {@link String} the DB alias; may not be null
     * @param parameters a {@link Map} of optional parameters use for initializing a particular provider; may not be null
     * @param cl a {@link ClassLoader} instance to be used when searching for available DB provider.
     * @return a {@link SchematicDb} instance with the given alias, never {@code null}
     * @throws RuntimeException if a DB with the given alias cannot be found or it fails during initialization 
     */
    public static SchematicDb getDb(String alias, Map<String, ?> parameters, ClassLoader cl) throws RuntimeException {
        ServiceLoader<SchematicDbProvider> providers = ServiceLoader.load(SchematicDbProvider.class, cl);
        List<RuntimeException> raisedExceptions = new ArrayList<>();
        return StreamSupport.stream(providers.spliterator(), false)
                            .map(provider -> getDbFromProvider(alias, parameters, raisedExceptions, provider))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow(() -> {
                                if (!raisedExceptions.isEmpty()) {
                                    return raisedExceptions.get(0);
                                } else {
                                    return new RuntimeException(
                                            "None of the existing DB providers could return a DB with alias '" + alias + "' and parameters " + parameters);
                                }
                            });
    }

    private static SchematicDb getDbFromProvider(String alias, Map<String, ?> parameters,
                                                 List<RuntimeException> raisedExceptions, SchematicDbProvider provider) {
        try {
            return provider.getDB(alias, parameters);
        } catch (RuntimeException re) {
            raisedExceptions.add(re);
            return null;
        } catch (Exception e) {
            raisedExceptions.add(new RuntimeException(e));
            return null;
        }
    }

    /**
     * Returns a DB with the given alias.
     *
     * @param alias a {@link String} the DB alias; may not be null
     * @return a {@link SchematicDb} instance with the given alias, never {@code null}
     * @throws RuntimeException if a DB with the given alias cannot be found or it fails during initialization 
     */
    public static SchematicDb getDb(String alias) throws RuntimeException {
        return getDb(alias, Collections.emptyMap());
    }

    /**
     * Create an in-memory schema library.
     *
     * @return the empty, in-memory schema library
     */
    public static SchemaLibrary createSchemaLibrary() {
        return new InMemorySchemaLibrary("In-memory schema library");
    }

    /**
     * Create an in-memory schema library.
     *
     * @param name the name of the library; may be null if a default name is to be used
     * @return the empty, in-memory schema library
     */
    public static SchemaLibrary createSchemaLibrary( String name ) {
        return new InMemorySchemaLibrary(name != null ? name : "In-memory schema library");
    }
}
