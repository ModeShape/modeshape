/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
