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

package org.modeshape.jcr.spi.federation;

import java.util.LinkedHashMap;
import java.util.List;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.value.Name;

/**
 * A type of document writer that can add paging information to documents. Typically, the operations exposed by this will be used
 * by {@link Pageable} connectors.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface PageWriter {

    /**
     * A constant that should be used by {@link #addPage(String, int, long, long)} and
     * {@link #addPage(String, String, long, long)} to signal that the number of children is either unknown or too large to
     * compute.
     */
    static final long UNKNOWN_TOTAL_SIZE = ChildReferences.UNKNOWN_SIZE;

    /**
     * Add a child with the given id and name to the underlying document.
     * 
     * @param id the new child's id; may not be null
     * @param name the new child's name; may not be null
     * @return this writer; never null
     */
    PageWriter addChild( String id,
                         String name );

    /**
     * Add a child with the given id and name to the underlying document.
     * 
     * @param id the new child's id; may not be null
     * @param name the new child's name; may not be null
     * @return this writer; never null
     */
    PageWriter addChild( String id,
                         Name name );

    /**
     * Remove a child from this document.
     * 
     * @param id the identifier of the child
     * @return this writer; never null
     */
    PageWriter removeChild( String id );

    /**
     * Set the list of children for the underlying document. If children previously existed, they will be replaced.
     * 
     * @param children a list of {@link EditableDocument} instances each describing a single child; may not be null
     * @return this writer; never null
     */
    PageWriter setChildren( List<? extends Document> children );

    /**
     * Set an ordered of (childId, childName) for the underlying document. If children previously existed, they will be replaced.
     * <p>
     * The passed map does not contain any explicit information about same name siblings, so a connector would need to handle that
     * logic.
     * </p>
     * 
     * @param children a map of (childId, childName) pairs; may not be null
     * @return this writer; never null
     */
    PageWriter setChildren( LinkedHashMap<String, Name> children );

    /**
     * Create a reference to a separate page of children in its underlying document. The underlying document can be either the
     * document of an external node, or the document of another page.
     * 
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextPageOffset a {@code non-null} String representing the offset of the next page. The meaning of the offset isn't
     *        defined and it's up to each connector to define it.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children; should be {@link #UNKNOWN_TOTAL_SIZE} if
     *        the number of children is unknown, too large to compute efficiently, or to signal that the repository should use the
     *        {@link Connector#getChildReference(String, String)} method to find the child reference for a parent and a (supposed)
     *        child node.
     * @return the current writer instance
     * @see Connector#getChildReference(String, String)
     */
    PageWriter addPage( String parentId,
                        String nextPageOffset,
                        long blockSize,
                        long totalChildCount );

    /**
     * Create a reference to a separate page of children in its underlying document. The underlying document can be either the
     * document of an external node, or the document of another page.
     * 
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextPageOffset a {@code non-null} int representing a numeric offset of the next page.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children; should be {@link #UNKNOWN_TOTAL_SIZE} if
     *        the number of children is unknown, too large to compute efficiently, or to signal that the repository should use the
     *        {@link Connector#getChildReference(String, String)} method to find the child reference for a parent and a (supposed)
     *        child node.
     * @return the current writer instance
     * @see Connector#getChildReference(String, String)
     */
    PageWriter addPage( String parentId,
                        int nextPageOffset,
                        long blockSize,
                        long totalChildCount );

    /**
     * Returns the underlying document.
     * 
     * @return an {@link EditableDocument} instance; never null
     */
    EditableDocument document();
}
