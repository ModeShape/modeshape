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

import java.util.NoSuchElementException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A concrete {@link PropertyIterator}.
 */
@Immutable
class JcrEmptyPropertyIterator implements PropertyIterator {

    static final PropertyIterator INSTANCE = new JcrEmptyPropertyIterator();

    private JcrEmptyPropertyIterator() {
        // Prevent instantiation
    }

    @Override
    public Property nextProperty() {
        throw new NoSuchElementException();
    }

    @Override
    public long getPosition() {
        return 0L;
    }

    @Override
    public long getSize() {
        return 0L;
    }

    @Override
    public void skip( long skipNum ) {
        CheckArg.isNonNegative(skipNum, "skipNum");
        if (skipNum == 0L) return;
        throw new NoSuchElementException();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Object next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
