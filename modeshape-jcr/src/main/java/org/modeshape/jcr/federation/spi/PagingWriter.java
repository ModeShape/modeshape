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

/**
 * A type of document writer that can add paging information to documents. Typically, the operations exposed by this will be used
 * by {@link Pageable} connectors.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface PagingWriter {

    /**
     * Creates a new page of children in its underlying document. The underlying document can be either the document of an external node,
     * or the document of another page.
     *
     * @param childrenFromCurrentPage a {@code non-null} {@link List} of children representing the children that should go in this page
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextPageOffset a {@code non-null} String representing the offset of the next page. The meaning of the offset isn't
     * defined and it's up to each connector to define it.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children
     * @return the current writer instance
     */
    public PagingWriter addPage( List<? extends Document> childrenFromCurrentPage,
                                 String parentId,
                                 String nextPageOffset,
                                 long blockSize,
                                 long totalChildCount );

     /**
     * Creates a new page of children in its underlying document. The underlying document can be either the document of an external node,
     * or the document of another page.
     *
     * @param childrenFromCurrentPage a {@code non-null} {@link List} of children representing the children that should go in this page
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextPageOffset a {@code non-null} int representing a numeric offset of the next page.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children
     * @return the current writer instance
     */
    public PagingWriter addPage( List<? extends Document> childrenFromCurrentPage,
                                 String parentId,
                                 int nextPageOffset,
                                 long blockSize,
                                 long totalChildCount );

    /**
     * Creates a document with a given list of children representing a final page of children.
     *
     * @param children a non-null {@link List} of children representing the children that should go in the last page
     * @return the current writer instance
     */
    public PagingWriter lastPage( List<? extends Document> children );
}