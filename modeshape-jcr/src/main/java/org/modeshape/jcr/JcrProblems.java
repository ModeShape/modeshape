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

import java.util.Iterator;

/**
 * A wrapper around an {@link org.modeshape.common.collection.Problems internal Problems} object that implements the ModeShape JCR
 * API {@link org.modeshape.jcr.api.Problems} interface.
 */
public class JcrProblems implements org.modeshape.jcr.api.Problems {

    private final org.modeshape.common.collection.Problems delegate;

    public JcrProblems( org.modeshape.common.collection.Problems problems ) {
        this.delegate = problems;
    }

    @Override
    public Iterator<org.modeshape.jcr.api.Problem> iterator() {
        final Iterator<org.modeshape.common.collection.Problem> iter = delegate.iterator();
        return new Iterator<org.modeshape.jcr.api.Problem>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public org.modeshape.jcr.api.Problem next() {
                final org.modeshape.common.collection.Problem next = iter.next();
                return new org.modeshape.jcr.api.Problem() {
                    @Override
                    public String getMessage() {
                        return next.getMessageString();
                    }

                    @Override
                    public Throwable getThrowable() {
                        return next.getThrowable();
                    }
                };
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean hasProblems() {
        return delegate.hasProblems();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
