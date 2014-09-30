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

import org.infinispan.schematic.Schematic.ContentTypes;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;

/**
 * A value used to store user's content (often a JSON document or a binary value) and metadata as an entry in a SchematicDb. These
 * values also offer fine-grained locking and serialization of the value.
 *
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface SchematicEntry extends Cloneable {

    public static interface FieldName {
        /**
         * The name of the field used internally to store an entry's metadata.
         */
        public static final String METADATA = "metadata";
        /**
         * The name of the field used internally to store an entry's content, which is either a {@link Document} or a
         * {@link Binary} value.
         */
        public static final String CONTENT = "content";

        /**
         * The name of the metadata field used to store the document key. Note that {@value} is also the field name used by <a
         * href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.27">JSON Schema</a>.
         */
        public static final String ID = "id";

        /**
         * The name of the metadata field used to store the reference to the JSON Schema to which the document should conform.
         * Note that {@value} is the field name used by <a
         * href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.29">JSON Schema</a>.
         */
        public static final String SCHEMA_URI = "$schema";
    }

    /**
     * Get the metadata associated with this value.
     *
     * @return the metadata document; never null
     */
    Document getMetadata();

    /**
     * Return this value's content. The result will either be a {@link Document}.
     *
     * @return the content, or null if there is no content
     */
    Document getContent();

    /**
     * Set the content for this value to be the supplied Document and set the content type to be " {@link ContentTypes#JSON
     * application/json}".
     *
     * @param content the Document representing the JSON content; may not be null
     */
    void setContent( Document content );

    /**
     * Get the representation of this entry as a document, which will include the {@link #getMetadata() metadata} and
     * {@link #getContent() content} as nested documents.
     *
     * @return the entry's representation as a document
     */
    Document asDocument();
}
