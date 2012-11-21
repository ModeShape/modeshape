/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.jcr.federation.spi;

import java.util.List;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.value.Name;

/**
 * A type of document writer that can add paging information to documents. Typically, the operations exposed by this will be used
 * by {@link Pageable} connectors.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface PageWriter {

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
     * Set the list of children for the underlying document. If children previously existed, they will be replaced.
     * 
     * @param children a list of {@link EditableDocument} instances each describing a single child; may not be null
     * @return this writer; never null
     */
    PageWriter setChildren( List<? extends Document> children );

    /**
     * Create a reference to a separate page of children in its underlying document. The underlying document can be either the
     * document of an external node, or the document of another page.
     * 
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextPageOffset a {@code non-null} String representing the offset of the next page. The meaning of the offset isn't
     *        defined and it's up to each connector to define it.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children
     * @return the current writer instance
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
     * @param totalChildCount an integer which indicates the total number of children
     * @return the current writer instance
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
