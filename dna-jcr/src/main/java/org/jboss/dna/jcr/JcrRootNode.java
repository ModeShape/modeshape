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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.session.GraphSession.NodeId;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrRootNode extends AbstractJcrNode {

    JcrRootNode( SessionCache cache,
                 NodeId nodeId,
                 Location location ) {
        super(cache, nodeId, location);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrNode#isRoot()
     */
    @Override
    boolean isRoot() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return 0;
     * @see javax.jcr.Item#getDepth()
     */
    @Override
    public int getDepth() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @return 1;
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @return "";
     * @see javax.jcr.Item#getName()
     */
    public String getName() {
        return "";
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ItemNotFoundException always
     * @see javax.jcr.Item#getParent()
     */
    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException {
        throw new ItemNotFoundException(JcrI18n.rootNodeHasNoParent.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @return "/";
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() {
        return "/";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrItem#getAncestor(int)
     */
    @Override
    public final Item getAncestor( int depth ) throws RepositoryException {
        if (depth == 0) return this;
        if (depth < 0) {
            throw new ItemNotFoundException(JcrI18n.noNegativeDepth.text(depth));
        }
        throw new ItemNotFoundException(JcrI18n.tooDeep.text(depth));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#remove()
     */
    public void remove() throws ConstraintViolationException {
        String msg = JcrI18n.unableToRemoveRootNode.text(cache.workspaceName());
        throw new ConstraintViolationException(msg);
    }

}
