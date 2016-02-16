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
package org.modeshape.schematic.internal;

import org.modeshape.schematic.DocumentLibrary;
import org.modeshape.schematic.SchemaLibrary;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.internal.document.Paths;
import org.modeshape.schematic.internal.schema.DocumentTransformer;
import org.modeshape.schematic.internal.schema.SchemaDocument;
import org.modeshape.schematic.internal.schema.SchemaDocumentCache;
import org.modeshape.schematic.internal.schema.ValidationResult;

public class InMemorySchemaLibrary implements SchemaLibrary {

    private final DocumentLibrary documents;
    private final SchemaDocumentCache schemaDocuments;

    public InMemorySchemaLibrary( String name ) {
        this.documents = new InMemoryDocumentLibrary(name);
        this.schemaDocuments = new SchemaDocumentCache(this, null);
    }

    @Override
    public String getName() {
        return documents.getName();
    }

    @Override
    public Document get( String key ) {
        return documents.get(key);
    }

    @Override
    public Document put( String key,
                         Document document ) {
        Document result = documents.put(key, document);
        schemaDocuments.remove(key);
        return result;
    }

    @Override
    public Document putIfAbsent( String key,
                                 Document document ) {
        Document result = documents.putIfAbsent(key, document);
        schemaDocuments.remove(key);
        return result;
    }

    @Override
    public Document replace( String key,
                             Document document ) {
        Document result = documents.replace(key, document);
        schemaDocuments.remove(key);
        return result;
    }

    @Override
    public Document remove( String key ) {
        Document result = documents.remove(key);
        schemaDocuments.remove(key);
        return result;
    }

    @Override
    public Results validate( Document document,
                             String schemaUri ) {
        ValidationResult result = new ValidationResult();
        SchemaDocument schema = schemaDocuments.get(schemaUri, result);
        if (schema != null) {
            schema.getValidator().validate(null, null, document, Paths.rootPath(), result, schemaDocuments);
        }
        return result;
    }

    @Override
    public Document convertValues( Document document,
                                   Results results ) {
        return DocumentTransformer.convertValuesWithMismatchedTypes(document, results);
    }

    @Override
    public Document convertValues( Document document,
                                   String schemaUri ) {
        Results results = validate(document, schemaUri);
        return convertValues(document, results);
    }
}
