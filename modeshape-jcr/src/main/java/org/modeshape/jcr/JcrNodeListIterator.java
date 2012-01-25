/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.util.CheckArg;

/**
 * A concrete {@link NodeIterator} that returns the supplied nodes.
 */
class JcrNodeListIterator implements NodeIterator {

    private final long size;
    private final Iterator<AbstractJcrNode> nodes;
    private Node nextNode;
    private long position = 0L;

    protected JcrNodeListIterator( Iterator<AbstractJcrNode> nodeIter,
                                   long size ) {
        this.nodes = nodeIter;
        this.size = size;
    }

    @Override
    public Node nextNode() {
        if (hasNext()) {
            assert nextNode != null;
            try {
                ++position;
                return nextNode;
            } finally {
                nextNode = null;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void skip( long skipNum ) {
        CheckArg.isNonNegative(skipNum, "skipNum");
        if (skipNum == 0L) return;
        for (long i = 0; i != skipNum; i++) {
            if (!hasNext()) return;
            next();
        }
    }

    @Override
    public boolean hasNext() {
        while (nextNode == null && nodes.hasNext()) {
            nextNode = nodes.next();
        }
        return nextNode != null;
    }

    @Override
    public Object next() {
        return nextNode();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
