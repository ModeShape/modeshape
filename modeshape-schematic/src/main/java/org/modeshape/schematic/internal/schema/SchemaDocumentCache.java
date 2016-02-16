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
package org.modeshape.schematic.internal.schema;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.modeshape.schematic.DocumentLibrary;
import org.modeshape.schematic.annotation.ThreadSafe;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.JsonSchema;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.document.Paths;

/**
 * A cache of {@link SchemaDocument} instances that each have a {@link SchemaDocument#getValidator() validator} that can
 * {@link Validator#validate(Object, String, Document, Path, Problems, Validator.SchemaDocumentResolver)
 * validate} JSON Documents. When a SchemaDocument is needed and has not yet been loaded, this cache finds the corresponding JSON
 * Schema {@link Document documents} from the {@link DocumentLibrary}, and uses a {@link Validator.Factory factory} to create the
 * {@link Validator}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@ThreadSafe
public class SchemaDocumentCache implements Validator.SchemaDocumentResolver, Serializable {

    private static final long serialVersionUID = 1L;

    private final String defaultMetaSchemaUri;
    private final DocumentLibrary jsonSchemaDocuments;
    private transient Map<String, SchemaDocument> schemaDocumentsByUri = new ConcurrentHashMap<>();

    public SchemaDocumentCache( DocumentLibrary jsonSchemaDocuments,
                                String defaultMetaSchemaUri ) {
        this.jsonSchemaDocuments = jsonSchemaDocuments;
        this.defaultMetaSchemaUri = defaultMetaSchemaUri != null ? defaultMetaSchemaUri : JsonSchema.Version.Latest.CORE_METASCHEMA_URL;
    }

    /**
     * Find the JSON Schema document with the supplied URI, and build the {@link SchemaDocument in-memory representation}
     * (including the {@link Validator validator}.
     * 
     * @param uri the URI of the schema being read
     * @param problems the object for recording any problems encountered while loading the schema
     * @return the in-memory {@link Document} representation, or null if there were fatal errors
     */
    @Override
    public SchemaDocument get( String uri,
                               Problems problems ) {
        if (uri == null) {
            throw new IllegalArgumentException("The 'uri' parameter may not be null");
        }

        // Check the cache first ...
        SchemaDocument result = schemaDocumentsByUri.get(uri);
        if (result != null) {
            return result;
        }

        // Find the JSON Schema document ...
        Document doc = jsonSchemaDocuments.get(uri);
        if (doc == null) {
            problems.recordError(Paths.rootPath(), "Unable to find the JSON Schema document for '" + uri + "'");
            return null;
        }

        // Validate the JSON document, if required ...
        String id = doc.getString("id");
        String schemaRef = doc.getString("$schema");
        if (schemaRef == null) {
            schemaRef = defaultMetaSchemaUri;
        }
        if (!schemaRef.equals(id)) {
            // This is not the meta-schema, so we need to validate it ...
            SchemaDocument schemaOfSchema = get(schemaRef, problems);
            Document schemaOfSchemaDoc = schemaOfSchema.getDocument();
            schemaOfSchema.getValidator().validate(null, null, schemaOfSchemaDoc, Paths.rootPath(), problems, this);
        }

        // The schema was valid ...
        URI schemaRefUri = null;
        try {
            schemaRefUri = new URI(uri);
        } catch (URISyntaxException e) {
            problems.recordWarning(Paths.path("$schema"), "The URI of the referenced schema '" + uri + "' is not a valid URI");
        }

        // Now build a validator for this JSON Schema (if we can) ...
        Validator validator = createFactory(schemaRefUri, problems).create(doc, Paths.rootPath());
        if (validator != null) {
            // Create the schema representation and cache it ...
            result = new SchemaDocument(schemaRef, doc, validator);
            schemaDocumentsByUri.put(uri, result);
        }

        return result;
    }

    /**
     * Remove any {@link SchemaDocument} that was loaded into this cache.
     * 
     * @param uri the URI of the schema being read
     * @return true if a SchemaDocument was removed from the cache, or false if this cache did not contain a SchemaDocument for
     *         the supplied URI
     */
    public boolean remove( String uri ) {
        return schemaDocumentsByUri.remove(uri) != null;
    }

    /**
     * Remove all {@link SchemaDocument} loaded into this cache.
     */
    public void removeAll() {
        schemaDocumentsByUri.clear();
    }

    protected Validator.Factory createFactory( URI schemaRefUri,
                                               Problems problems ) {
        return new JsonSchemaValidatorFactory(schemaRefUri, problems);
    }
}
