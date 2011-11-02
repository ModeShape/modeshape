/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.schema;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.infinispan.schematic.DocumentLibrary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.JsonSchema;
import org.infinispan.schematic.document.ThreadSafe;
import org.infinispan.schematic.internal.document.Paths;

/**
 * A cache of {@link SchemaDocument} instances that each have a {@link SchemaDocument#getValidator() validator} that can
 * {@link Validator#validate(Object, String, Document, org.infinispan.schematic.document.Path, Problems, org.infinispan.schematic.internal.schema.Validator.SchemaDocumentResolver)
 * validate} JSON Documents. When a SchemaDocument is needed and has not yet been loaded, this cache finds the corresponding JSON
 * Schema {@link Document documents} from the {@link DocumentLibrary}, and uses a {@link Validator.Factory factory} to create the
 * {@link Validator}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@ThreadSafe
public class SchemaDocumentCache implements Validator.SchemaDocumentResolver, Serializable {

    private static final long serialVersionUID = 1L;

    private final String defaultMetaSchemaUri;
    private final DocumentLibrary jsonSchemaDocuments;
    private transient Map<String, SchemaDocument> schemaDocumentsByUri = new ConcurrentHashMap<String, SchemaDocument>();

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
            schemaOfSchema.getValidator().validate(null, null, doc, Paths.rootPath(), problems, this);
        }

        // The schema was valid ...
        URI schemaRefUri = null;
        try {
            schemaRefUri = new URI(schemaRef);
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
