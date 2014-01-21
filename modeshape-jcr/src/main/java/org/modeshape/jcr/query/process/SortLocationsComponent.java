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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 */
public class SortLocationsComponent extends DelegatingComponent {

    private Comparator<Object[]> sortingComparator;

    public SortLocationsComponent( ProcessingComponent delegate ) {
        super(delegate);
        this.sortingComparator = createSortComparator(delegate.getContext(), delegate.getColumns());
    }

    public SortLocationsComponent( ProcessingComponent delegate,
                                   Comparator<Object[]> sortingComparator ) {
        super(delegate);
        this.sortingComparator = sortingComparator;
    }

    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();
        if (tuples.size() > 1) {
            // Sort the tuples ...
            Collections.sort(tuples, sortingComparator);
        }
        return tuples;
    }
}
