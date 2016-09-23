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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.ParsingException;
import org.modeshape.schematic.internal.InMemorySchemaLibrary;

public class Schematic extends DocumentFactory {
    
    public static final String TYPE_FIELD = "type";
    
    /**
     * Returns a DB with reads the given input stream as a configuration document {@code Document}. This document is 
     * expected to contain a {@link Schematic#TYPE_FIELD type field} to indicate the type
     * of DB.
     * 
     * @see #getDb(Document, ClassLoader)
     * @throws ParsingException if the given input stream is not a valid JSON document
     */
    public static <T extends SchematicDb> T getDb(InputStream configInputStream) throws ParsingException, RuntimeException {
        Document document = Json.read(configInputStream).withVariablesReplacedWithSystemProperties();
        return getDb(document, Schematic.class.getClassLoader());
    }
     
    /**
     * Returns a DB with the given configuration document {@code Document}. This document is expected to contain 
     * a {@link Schematic#TYPE_FIELD type field} to indicate the type of DB.
     * 
     * @see #getDb(Document, ClassLoader)
     */
    public static <T extends SchematicDb> T getDb(Document document) throws RuntimeException {
        return getDb(document, Schematic.class.getClassLoader());
    }
    
    /**
     * Returns a DB with the given type and configuration document, by delegating to all the available {@link SchematicDbProvider} 
     * services. This document is expected to contain a {@link Schematic#TYPE_FIELD type field} to indicate the type
     *
     * @param document a {@link Document} containing the configuration of a particular DB type; may not be null
     * @param cl a {@link ClassLoader} instance to be used when searching for available DB provider.
     * @return a {@link SchematicDb} instance with the given alias, never {@code null}
     * @throws RuntimeException if a DB with the given alias cannot be found or it fails during initialization 
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public static <T extends SchematicDb> T getDb(Document document, ClassLoader cl) throws RuntimeException {
        String type = document.getString(TYPE_FIELD);
        if (type == null) {
            throw new IllegalArgumentException("The configuration document '" + document + "' does not contain a '" + TYPE_FIELD + "' field");
        }
        ServiceLoader<SchematicDbProvider> providers = ServiceLoader.load(SchematicDbProvider.class, cl);
        List<RuntimeException> raisedExceptions = new ArrayList<>();
        return (T) StreamSupport.stream(providers.spliterator(), false)
                                .map(provider -> getDbFromProvider(type, document, raisedExceptions, provider))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> {
                                    if (!raisedExceptions.isEmpty()) {
                                        return raisedExceptions.get(0);
                                    } else {
                                        return new RuntimeException(
                                                "None of the existing persistence providers could return a Schematic DB with type '" + type + "'");
                                    }
                                });
    }

    private static SchematicDb getDbFromProvider(String alias, Document document,
                                                               List<RuntimeException> raisedExceptions,
                                                               SchematicDbProvider<?> provider) {
        try {
            return provider.getDB(alias, document);
        } catch (RuntimeException re) {
            raisedExceptions.add(re);
            return null;
        } catch (Exception e) {
            raisedExceptions.add(new RuntimeException(e));
            return null;
        }
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
