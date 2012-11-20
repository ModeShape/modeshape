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
     * Returns a document which represents a page of children. In order to add a next page, {@link PagingWriter#addPage(String, String, long, long)}
     * should be used to add a new page of children.
     *
     * @param pageKey a {@code non-null} {@link PageKey} instance, which offers information
     * about the page that should be retrieved.
     * @return either a {@code non-null} page document or {@code null} indicating that such a page doesn't exist
     */
    public Document getChildren( PageKey pageKey );
}
