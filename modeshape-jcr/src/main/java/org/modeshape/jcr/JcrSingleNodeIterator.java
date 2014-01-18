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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;

/**
 * A concrete {@link NodeIterator} that delegates to the supplied iterator.
 */
@NotThreadSafe
final class JcrSingleNodeIterator implements NodeIterator {

    private final long size;
    private final Iterator<AbstractJcrNode> iterator;

    protected JcrSingleNodeIterator( AbstractJcrNode singleNode ) {
        assert singleNode != null;
        this.iterator = Collections.singleton(singleNode).iterator();
        this.size = 1;
    }

    @Override
    public Node nextNode() {
        return this.iterator.next();
    }

    @Override
    public long getPosition() {
        return 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void skip( long skipNum ) {
        CheckArg.isNonNegative(skipNum, "skipNum");
        if (skipNum == 0L) return;
        throw new NoSuchElementException();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
