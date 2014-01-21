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
import java.util.Iterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;

/**
 * ModeShape implementation of a {@link PropertyIterator}.
 */
@NotThreadSafe
final class JcrPropertyIterator implements PropertyIterator {

    private final Iterator<? extends Property> iterator;
    private int ndx;
    private int size;

    JcrPropertyIterator( Collection<? extends Property> properties ) {
        assert properties != null;
        iterator = properties.iterator();
        size = properties.size();
    }

    @Override
    public long getPosition() {
        return ndx;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        return nextProperty();
    }

    @Override
    public Property nextProperty() {
        Property next = iterator.next();
        ndx++;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void skip( long count ) {
        CheckArg.isNonNegative(count, "count");
        while (--count >= 0) {
            nextProperty();
        }
    }
}
