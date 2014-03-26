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
package org.modeshape.jcr.query.engine.process;

import org.modeshape.jcr.query.NodeSequence;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class DelegatingSequence extends NodeSequence {

    protected final NodeSequence delegate;

    protected DelegatingSequence( NodeSequence delegate ) {
        this.delegate = delegate;
    }

    @Override
    public int width() {
        return delegate.width();
    }

    @Override
    public long getRowCount() {
        return delegate.getRowCount();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Batch nextBatch() {
        return delegate.nextBatch();
    }

    @Override
    public void close() {
        delegate.close();
    }

}
