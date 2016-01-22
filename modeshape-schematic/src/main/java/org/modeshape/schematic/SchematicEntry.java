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

import java.util.Objects;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * A wrapper over a conventional {@link Document} which exposes a predefined structure of documents usually stored inside
 * a {@link SchematicDb}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @author Horia Chiorean <hchiorea@redhat.com>
 * 
 * @since 5.0
 */
@FunctionalInterface
public interface SchematicEntry {

    interface FieldName {
        /**
         * The name of the field used internally to store an entry's metadata.
         */
        String METADATA = "metadata";
        /**
         * The name of the field used internally to store an entry's content, which is either a {@link Document} or a
         * {@link Binary} value.
         */
        String CONTENT = "content";

        /**
         * The name of the metadata field used to store the document key. Note that {@value} is also the field name used by <a
         * href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.27">JSON Schema</a>.
         */
        String ID = "id";
    }

    /**
     * Returns the original document which is wrapped by this entry.
     *
     * @return a {@link Document} instance, never {@code null}
     */
    Document source();

    /**
     * Get the metadata associated with this document.
     *
     * @return the metadata document or null if there is no metadata document
     */
    default Document getMetadata() {
        return source().getDocument(FieldName.METADATA);
    }

    /**
     * Return this document's content. The result will either be a {@link Document} or null.
     *
     * @return the content, or null if there is no content
     */
    default Document getContent() {
        return source().getDocument(FieldName.CONTENT);
    }

    /**
     * Returns this document's id.
     *
     * @return the ID, or null if no ID field is present
     * @throws NullPointerException if this document does not have a metadata section
     */
    default String getId() {
        return Objects.requireNonNull(getMetadata(), "Metadata document is null").getString(FieldName.ID);
    }

    /**
     * Creates a new empty entry with the given id.
     * 
     * @param id the id of the document, may not be null. 
     * @return a new {@link SchematicEntry}, never {@code null}
     */
    static SchematicEntry create(String id) {
        return create(id, new BasicDocument());    
    }

    /**
     * Creates a new entry with the given content.
     *
     * @param id the id of the document, may not be null.
     * @param content the id of the document, may not be null.
     * @return a new {@link SchematicEntry}, never {@code null}
     */
    static SchematicEntry create(String id, Document content) {
        id = Objects.requireNonNull(id, "id cannot be null");
        content = Objects.requireNonNull(content, "content cannot be null");
        final Document source = new BasicDocument(FieldName.METADATA, new BasicDocument(FieldName.ID, id), 
                                                  FieldName.CONTENT, content);
        return () -> source;
    }
}
