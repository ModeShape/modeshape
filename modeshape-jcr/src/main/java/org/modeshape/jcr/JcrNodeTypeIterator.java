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
