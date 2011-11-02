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
package org.modeshape.jcr;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import org.modeshape.common.annotation.Immutable;

/**
 * Type-safe {@link Iterator} implementation for NodeTypes, as per the JCR specification.
 */
@Immutable
final class JcrNodeTypeIterator implements NodeTypeIterator {

    private int size;
    private int position;
    private Iterator<NodeType> iterator;

    JcrNodeTypeIterator( Collection<? extends NodeType> values ) {
        this.iterator = Collections.unmodifiableCollection(values).iterator();
        this.size = values.size();
        this.position = 0;

    }

    @Override
    public NodeType nextNodeType() {
        position++;
        return iterator.next();
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
    public void skip( long count ) {
        position += count;
        while (count-- > 0)
            iterator.next();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        position++;
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Node types cannot be removed through their iterator");
    }

}
