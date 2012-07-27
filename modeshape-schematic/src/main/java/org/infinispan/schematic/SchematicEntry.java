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
package org.infinispan.schematic;

import org.infinispan.schematic.Schematic.ContentTypes;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.SchematicEntryLookup;

/**
 * A value used to store user's content (often a JSON document or a binary value) and metadata as an entry in a SchematicDb. These
 * values also offer fine-grained locking and serialization of the value.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @see SchematicEntryLookup
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

        /**
         * The name of the metadata field used to store the MIME type for the entry's content.
         */
        public static final String CONTENT_TYPE = "contentType";
    }

    /**
     * Get the metadata associated with this value.
     * 
     * @return the metadata document; never null
     */
    Document getMetadata();

    /**
     * Get the media type describing the content.
     * 
     * @return the media type; never null
     */
    String getContentType();

    /**
     * Return this value's content. The result will either be a {@link Document} or {@link Binary}.
     * Note that this method will return a non-null value only if the {@link #hasDocumentContent()} method
     * returns <code>true</code>.
     * 
     * @return the content, represented as a {@link Document} object, or null if there is no content
     */
    Object getContent();

    /**
     * Get this value's content, if it is a Document object. This method will always return a non-null Document if
     * {@link #hasDocumentContent()} returns <code>true</code>, and is therefore equivalent to the following:
     * 
     * <pre>
     * return hasDocumentContent() ? (Document)getContent() : null;
     * </pre>
     * 
     * @return the {@link Document} content, or null if there is no content or the content is a {@link Binary} value
     */
    Document getContentAsDocument();

    /**
     * Get this value's content, if it is a Binary object. This method will always return a non-null Binary if
     * {@link #hasDocumentContent()} returns <code>false</code>, and is therefore equivalent to the following:
     * 
     * <pre>
     * return hasDocumentContent() ? (Binary)getContent() : null;
     * </pre>
     * 
     * @return the {@link Binary} content, or null if there is no content or if the content is a {@link Document}
     */
    Binary getContentAsBinary();

    /**
     * Return <code>true</code> if the {@link #getContent()} method would return a Document (or {@link #getContentAsDocument()}
     * would return a non-null value).
     * <p>
     * This is equivalent to the following:
     * 
     * <pre>
     * return getContent() instanceof Document;
     * </pre>
     * 
     * </p>
     * 
     * @return <code>true</code> if the content is a Document, or <code>false</code> otherwise
     */
    boolean hasDocumentContent();

    /**
     * Return <code>true</code> if the {@link #getContent()} method would return a Binary (or {@link #getContentAsBinary()} would
     * return a non-null value).
     * <p>
     * This is equivalent to the following:
     * 
     * <pre>
     * return getContent() instanceof Binary;
     * </pre>
     * 
     * </p>
     * 
     * @return <code>true</code> if the content is a Binary value, or <code>false</code> otherwise
     */
    boolean hasBinaryContent();

    /**
     * Set the content for this value to be the supplied Document and set the content type to be " {@link ContentTypes#JSON
     * application/json}".
     * 
     * @param content the Document representing the JSON content; may not be null
     * @param metadata the Document representing the metadata; may be null
     * @param defaultContentType the value for the MIME type describing the content that should be used if the metadata does not
     *        already contain a "contentType" field, and typically {@link ContentTypes#JSON}, {@link ContentTypes#JSON_SCHEMA} ,
     *        or {@link ContentTypes#BSON}; may not be null
     */
    void setContent( Document content,
                     Document metadata,
                     String defaultContentType );

    /**
     * Set the content for this value to be the supplied {@link Binary} data described by the supplied content type.
     * 
     * @param content the Binary representation of the content; may not be null
     * @param metadata the Document representing the metadata; may be null
     * @param defaultContentType the value for the MIME type describing the content that should be used if the metadata does not
     *        already contain a "contentType" field, and typically {@link ContentTypes#JSON}, {@link ContentTypes#JSON_SCHEMA} ,
     *        or {@link ContentTypes#BSON}; may not be null
     */
    void setContent( Binary content,
                     Document metadata,
                     String defaultContentType );

    /**
     * Get an {@link EditableDocument editable metadata document}. The client is expected to make these edits within the context
     * of a transaction, and the edits will be saved when the transaction is committed.
     * 
     * @return the editable representation of the document
     * @see #editDocumentContent()
     */
    EditableDocument editMetadata();

    /**
     * Get an {@link EditableDocument editable document}, when the content is a {@link #hasDocumentContent() document}. The client
     * is expected to make these edits within the context of a transaction, and the edits will be saved when the transaction is
     * committed.
     * 
     * @return the editable representation of the content document
     * @see #editMetadata()
     */
    EditableDocument editDocumentContent();
}
