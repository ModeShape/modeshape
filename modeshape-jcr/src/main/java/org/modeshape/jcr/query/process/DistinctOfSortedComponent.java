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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * An efficient {@link ProcessingComponent} that removes duplicates from an already-sorted set of results.
 * 
 * @see DistinctComponent
 */
public class DistinctOfSortedComponent extends DelegatingComponent {

    private final Comparator<Object[]> comparator;

    public DistinctOfSortedComponent( SortValuesComponent delegate ) {
        super(delegate);
        this.comparator = delegate.getSortingComparator();
    }

    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();
        Iterator<Object[]> iter = tuples.iterator();
        Object[] previous = null;
        while (iter.hasNext()) {
            Object[] current = iter.next();
            if (previous != null && this.comparator.compare(previous, current) == 0) {
                iter.remove();
            } else {
                previous = current;
            }
        }
        return tuples;
    }
}
