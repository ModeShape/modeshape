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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Location;

/**
 * A {@link ProcessingComponent} implementation that removes duplicates. The results from the delegate component do not need to be
 * sorted; in fact, if the delegate component is a {@link SortValuesComponent}, then use {@link DistinctOfSortedComponent} instead
 * of this class.
 */
public class DistinctComponent extends DelegatingComponent {

    public DistinctComponent( ProcessingComponent delegate ) {
        super(delegate);
    }

    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();

        if (!tuples.isEmpty()) {
            QueryResults.Columns columns = getColumns();
            int locationCount = columns.getLocationCount();
            int[] locationIndexes = super.getLocationIndexes(columns);

            // Go through this list and remove any tuples that appear more than once ...
            Iterator<Object[]> iter = tuples.iterator();

            // Duplicate tuples are removed using a Set<Location[]> ...
            Set<List<Location>> found = new HashSet<List<Location>>();
            while (iter.hasNext()) {
                Object[] tuple = iter.next();
                List<Location> locations = new ArrayList<Location>(locationCount);
                for (int locationIndex : locationIndexes) {
                    locations.add((Location)tuple[locationIndex]);
                }
                if (!found.add(locations)) {
                    // Was already found, so remove this tuple from the results ...
                    iter.remove();
                }
            }
        }
        return tuples;
    }
}
