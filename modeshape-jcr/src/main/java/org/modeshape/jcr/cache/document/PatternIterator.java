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
package org.modeshape.jcr.cache.document;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * A base class for an {@link Iterator} implementation that exposes values from another iterator that match at least one of the
 * supplied patterns.
 * 
 * @param <Type> the type of values
 */
abstract class PatternIterator<Type> extends DelegatingIterator<Type> {

    private final Collection<?> patterns;
    private Type last;

    protected PatternIterator( Iterator<Type> delegate,
                               Collection<?> patterns ) {
        super(delegate);
        this.patterns = patterns;
    }

    @Override
    public boolean hasNext() {
        if (last != null) {
            return true;
        }
        while (super.hasNext()) {
            last = super.next();
            String childName = matchable(last);
            for (Object pattern : patterns) {
                if (pattern instanceof String) {
                    // Check for an exact match ...
                    if (childName.equals(pattern)) return true;
                } else {
                    Pattern p = (Pattern)pattern;
                    if (p.matcher(childName).matches()) return true;
                }
            }
        }
        last = null;
        return false;
    }

    @Override
    public Type next() {
        try {
            if (last == null) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            if (last != null) {
                return last;
            }
        } finally {
            last = null;
        }
        throw new NoSuchElementException();
    }

    protected abstract String matchable( Type value );
}
