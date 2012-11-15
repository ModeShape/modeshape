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

import org.infinispan.schematic.document.Document;

/**
 * Marker interface that should be implemented by {@link org.modeshape.jcr.federation.spi.Connector}(s) that want to expose children
 * of nodes in a "page by page" fashion. For effectively creating blocks of children for each page, connector implementations
 * should use the {@link PagingWriter} extension.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Pageable {

    /**
     * Returns a document which represents a block of children. In order to add a next block, {@link PagingWriter#newBlock(java.util.List, String, String, long, long)}
     * should be used to add a next block to the current block. If there aren't any more blocks {@link PagingWriter#endBlock(java.util.List)}
     * can be used.
     *
     * @param blockKey a {@code non-null} {@link BlockKey} instance, which offers information about a block
     * @return either a {@code non-null} block document or {@code null} indicating that such a block doesn't exist
     */
    public Document getChildrenBlock( BlockKey blockKey );
}
