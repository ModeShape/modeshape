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

    abstract class FieldName {
        private FieldName() {
        }

        /**
         * The name of the field used internally to store an entry's metadata.
         */
        protected static final String  METADATA = "metadata";
        
        /**
         * The name of the field used internally to store an entry's content, which is either a {@link Document} or a
         * {@link Binary} value.
         */
        protected static final String CONTENT = "content";

        /**
         * The name of the metadata field used to store the document key. Note that {@value} is also the field name used by <a
         * href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.27">JSON Schema</a>.
         */
        protected static final String ID = "id";
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
    default Document content() {
        return SchematicEntry.content(source());
    }

    /**
     * Returns this document's id.
     *
     * @return the ID, or null if no ID field is present
     * @throws NullPointerException if this document does not have a metadata section
     */
    default String id() {
        return SchematicEntry.id(source());
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

    /**
     * Returns the value of the ID from a given entry document.
     * 
     * @param entryDocument a {@link Document} instance representing a schematic entry.
     * @return a {@link String} or {@code null} if there is no {@link org.modeshape.schematic.SchematicEntry.FieldName#METADATA}
     * document or if that document doesn't have an id.
     */
    static String id(Document entryDocument) {
        Document metadata = entryDocument.getDocument(FieldName.METADATA);
        if (metadata == null) {
            return null;
        }
        return metadata.getString(FieldName.ID);
    }

    /**
     * Returns the value of the CONTENT document from a given entry document.
     * 
     * @param entryDocument a {@link Document} instance representing a schematic entry.
     * @return a {@link Document} or {@code null} if there is no {@link org.modeshape.schematic.SchematicEntry.FieldName#CONTENT}
     * document.
     */
    static Document content(Document entryDocument) {
        return entryDocument.getDocument(FieldName.CONTENT);
    }
    
    /**
     * Creates a new schematic entry instance based on the given document.
     * 
     * @param entryDocument a {@link Document} instance; may not be {@code null}
     * @return a {@link SchematicEntry} instance which wraps the underlying document; never {@code null}
     */
    static SchematicEntry fromDocument(Document entryDocument) {
        Objects.requireNonNull(entryDocument, "document cannot be null");
        return () -> entryDocument;
    }
}
