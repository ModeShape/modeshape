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

import java.util.List;
import org.modeshape.jcr.query.model.Limit;

/**
 */
public class LimitComponent extends DelegatingComponent {

    private final Limit limit;

    public LimitComponent( ProcessingComponent delegate,
                           Limit limit ) {
        super(delegate);
        this.limit = limit;
        assert this.limit != null;
    }

    @Override
    public List<Object[]> execute() {
        if (limit.getRowLimit() == 0) {
            return emptyTuples();
        }
        List<Object[]> tuples = delegate().execute();
        if (limit.isOffset()) {
            if (limit.getOffset() >= tuples.size()) {
                // There aren't enough results, so return an empty list ...
                return emptyTuples();
            }
            if (limit.hasRowLimited()) {
                // Both an offset AND a row limit (which may be more than the number of rows available)...
                int toIndex = Math.min(tuples.size(), Math.max(0, limit.getOffset() + limit.getRowLimit()));
                tuples = tuples.subList(limit.getOffset(), toIndex);
            } else {
                // An offset, but no row limit ...
                tuples = tuples.subList(limit.getOffset(), tuples.size());
            }
        } else {
            // No offset, but perhaps there's a row limit ...
            if (limit.hasRowLimited()) {
                int toIndex = Math.min(limit.getRowLimit(), tuples.size());
                tuples = tuples.subList(0, toIndex);
            }
        }
        return tuples;
    }
}
