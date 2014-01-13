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
package org.infinispan.schematic.internal;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.schematic.DocumentLibrary;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.schema.SchemaDocument;
import org.infinispan.schematic.internal.schema.SchemaDocumentCache;
import org.infinispan.schematic.internal.schema.ValidationResult;

/**
 * A {@link Mapper} implementation that validates {@link Document JSON Documents} within Infinispan's Map-Reduce
 * framework.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class DocumentValidationMapper implements Mapper<String, SchematicEntry, String, Results> {

   private static final long serialVersionUID = 1L;

   private final SchemaDocumentCache schemaLibrary;
   private final String defaultSchemaUri;

   /**
    * Create a new instance of the document validation {@link Mapper}.
    * 
    * @param schemaLibrary
    *           the serializable document library containing all required/available JSON Schema documents; may not be
    *           null
    * @param defaultSchemaUri
    *           the URI of the JSON Schema that should be used for JSON documents that have not been associated with a
    *           schema (via the "{@link FieldName#SCHEMA_URI schemaUri}" field in the document's metadata); may be null
    *           if JSON documents without a schema reference should be skipped and not validated
    */
   public DocumentValidationMapper(DocumentLibrary schemaLibrary, String defaultSchemaUri) {
      this.schemaLibrary = new SchemaDocumentCache(schemaLibrary, null);
      this.defaultSchemaUri = defaultSchemaUri;
   }

   @Override
   public void map(String key, SchematicEntry value, Collector<String, Results> collector) {
      Results results = validate(key, value);

      // Emit the problems IFF there are any ...
      if (results != null && results.hasProblems()) {
         collector.emit(key, results);
      }
   }

   public Results validate(String key, SchematicEntry value) {
      // The entry must have a Document for the content ...
      Document doc = value.getContentAsDocument();
      if (doc == null) {
         return null;
      }
      // The entry must have metadata ...
      Document metadata = value.getMetadata();
      if (metadata == null) {
         return null;
      }
      // Get the schema URI from the metadata (or use the default) ...
      String schemaUri = metadata.getString(FieldName.SCHEMA_URI, defaultSchemaUri);
      if (schemaUri == null) {
         return null;
      }
      // Find the schema document ...
      ValidationResult problems = new ValidationResult();
      SchemaDocument schemaDoc = schemaLibrary.get(schemaUri, problems);
      if (schemaDoc != null) {
         // Validate the document and collect the results ...
         schemaDoc.getValidator().validate(null, null, doc, Paths.rootPath(), problems, schemaLibrary);
      }
      return problems;
   }

}
