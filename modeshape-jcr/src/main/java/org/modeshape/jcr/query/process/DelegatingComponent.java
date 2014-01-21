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
package org.modeshape.jcr.query.process;

import org.modeshape.jcr.query.QueryResults.Columns;

/**
 */
public abstract class DelegatingComponent extends ProcessingComponent {

    private final ProcessingComponent delegate;

    protected DelegatingComponent( ProcessingComponent delegate ) {
        this(delegate, delegate.getColumns());
    }

    protected DelegatingComponent( ProcessingComponent delegate,
                                   Columns overridingColumns ) {
        super(delegate.getContext(), overridingColumns);
        this.delegate = delegate;
    }

    /**
     * Get the delegate processor.
     * 
     * @return the delegate processor
     */
    protected final ProcessingComponent delegate() {
        return delegate;
    }

    @Override
    public void close() {
        super.close();
        this.delegate.close();
    }
}
