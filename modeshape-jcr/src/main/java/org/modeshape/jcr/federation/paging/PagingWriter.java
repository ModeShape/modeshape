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

package org.modeshape.jcr.federation.paging;

import java.util.List;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.FederatedDocumentWriter;

/**
 * Extension of a {@code FederatedDocumentWriter} that should be used by {@link Pageable} connectors to create the proper block
 * structure in their documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class PagingWriter extends FederatedDocumentWriter {

    /**
     * @see FederatedDocumentWriter#FederatedDocumentWriter(org.modeshape.jcr.cache.document.DocumentTranslator)
     */
    public PagingWriter( DocumentTranslator translator ) {
        super(translator);
    }

    /**
     * @see FederatedDocumentWriter#FederatedDocumentWriter(org.modeshape.jcr.cache.document.DocumentTranslator, org.infinispan.schematic.document.Document)
     */
    public PagingWriter( DocumentTranslator translator,
                         Document document ) {
        super(translator, document);
    }

    /**
     * Creates a block of children in its underlying document. The underlying document can be either the document of an external node,
     * or the document of a block, depending on how the {@link PagingWriter} was created.
     *
     * @param childrenFromCurrentBlock a {@code non-null} {@link List} of children representing the children that should go in this block
     * @param parentId a {@code non-null} String representing the identifier of the parent (owning) document
     * @param nextBlockOffset a {@code non-null} String representing the offset of the next block. The meaning of the offset isn't
     * defined and it's up to each connector to define it.
     * @param blockSize an integer which indicates the size of the next block of children
     * @param totalChildCount an integer which indicates the total number of children
     * @return the current writer instance
     */
    public PagingWriter newBlock( List<? extends Document> childrenFromCurrentBlock,
                                  String parentId,
                                  String nextBlockOffset,
                                  long blockSize,
                                  long totalChildCount ) {
        //sets only the children from the current page
        setChildren(childrenFromCurrentBlock);
        //adds a new block at the end
        addChildrenBlock(parentId, nextBlockOffset, blockSize, totalChildCount);
        return this;
    }

    /**
     * Creates the "final block" document with a given list of children.
     *
     * @param childrenFromCurrentBlock a non-null {@link List} of children representing the children that should go in the final block
     * @return the current writer instance
     */
    public PagingWriter endBlock( List<? extends Document> childrenFromCurrentBlock ) {
        setChildren(childrenFromCurrentBlock);
        return this;
    }

    private void addChildrenBlock( String parentId,
                                   String offset,
                                   long blockSize,
                                   long totalChildCount ) {
        EditableDocument childrenInfo = document().getDocument(DocumentTranslator.CHILDREN_INFO);
        if (childrenInfo == null) {
            childrenInfo = DocumentFactory.newDocument();
            document().setDocument(DocumentTranslator.CHILDREN_INFO, childrenInfo);
        }
        BlockKey blockKey = new BlockKey(parentId, offset, blockSize);
        childrenInfo.setNumber(DocumentTranslator.COUNT, totalChildCount);
        childrenInfo.setNumber(DocumentTranslator.BLOCK_SIZE, blockSize);
        childrenInfo.setString(DocumentTranslator.NEXT_BLOCK, blockKey.toString());
    }
}
