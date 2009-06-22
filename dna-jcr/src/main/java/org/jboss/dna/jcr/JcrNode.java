/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import net.jcip.annotations.NotThreadSafe;

/**
 *
 */
@NotThreadSafe
final class JcrNode extends AbstractJcrNode {

    JcrNode( SessionCache cache,
             UUID nodeUuid ) {
        super(cache, nodeUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrNode#isRoot()
     */
    @Override
    boolean isRoot() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        return cache.getSnsIndexOf(nodeUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public String getName() throws RepositoryException {
        return cache.getNameOf(nodeUuid).getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        return cache.findJcrNode(nodeInfo().getParent());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() throws RepositoryException {
        return cache.getPathFor(nodeUuid).getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#remove()
     */
    public void remove() throws RepositoryException {
        editorForParent().destroyChild(nodeUuid);
    }
}
