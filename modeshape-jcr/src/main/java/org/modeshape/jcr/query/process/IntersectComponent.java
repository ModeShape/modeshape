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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.TupleReformatter;

/**
 */
public class IntersectComponent extends SetOperationComponent {

    private final Comparator<Object[]> comparator;

    public IntersectComponent( QueryContext context,
                               Columns columns,
                               Iterable<ProcessingComponent> sources,
                               boolean alreadySorted,
                               boolean all ) {
        super(context, columns, sources, alreadySorted, all);
        this.comparator = createSortComparator(context, columns);
    }

    @Override
    public List<Object[]> execute() {
        Iterator<ProcessingComponent> sources = sources().iterator();
        if (!sources.hasNext()) return emptyTuples();

        // Execute all of the source components (so we can sort the results from the smallest to the largest) ...
        // TODO: Parallelize this ???
        List<List<Object[]>> allTuples = new LinkedList<List<Object[]>>();
        Iterator<TupleReformatter> reformatters = sourceReformatters.iterator();
        while (sources.hasNext()) {
            List<Object[]> tuples = sources.next().execute();
            if (tuples == null) continue;
            if (tuples.isEmpty()) return emptyTuples();
            TupleReformatter reformatter = reformatters.next();
            if (reformatter != null) {
                ListIterator<Object[]> iter = tuples.listIterator();
                while (iter.hasNext()) {
                    iter.set(reformatter.reformat(iter.next()));
                }
            }
            allTuples.add(tuples);

        }
        if (allTuples.isEmpty()) return emptyTuples();
        if (allTuples.size() == 1) return allTuples.get(0); // just one source
        // Sort the tuples by size, starting with the smallest ...
        Collections.sort(allTuples, new Comparator<List<Object[]>>() {
            @Override
            public int compare( List<Object[]> tuples1,
                                List<Object[]> tuples2 ) {
                return tuples1.size() - tuples2.size();
            }
        });

        // Do the sources 2 at a time ...
        Iterator<List<Object[]>> iter = allTuples.iterator();
        List<Object[]> tuples = iter.next(); // already sorted ...
        assert iter.hasNext();
        // Process the next source with the previous ...
        while (iter.hasNext()) {
            List<Object[]> next = iter.next(); // already sorted ...
            // Walk through the first and second results ...
            Iterator<Object[]> tupleIter = tuples.iterator();
            Iterator<Object[]> nextIter = next.iterator();
            // There is at least one tuple in each ...
            Object[] tuple1 = tupleIter.next();
            Object[] tuple2 = nextIter.next();
            while (true) {
                int comparison = comparator.compare(tuple1, tuple2);
                if (comparison == 0) {
                    if (!tupleIter.hasNext() || !nextIter.hasNext()) {
                        break;
                    }
                    // Both match, so leave the tuple in 'tuples' and advance to next comparison ...
                    tuple1 = tupleIter.next();
                    tuple2 = nextIter.next();
                } else {
                    // No match, so remove tuple1 from 'tuples'
                    tupleIter.remove();
                    if (comparison < 0) {
                        // tuple1 is less than tuple2, so advance tupleIter ...
                        if (!tupleIter.hasNext()) {
                            // The intersection results ('tuples') has no more tuples, so go to the next source ...
                            break;
                        }
                        tuple1 = tupleIter.next();
                    } else {
                        // tuple1 is greater than tuple2, so advance nextIter ...
                        if (!nextIter.hasNext()) {
                            // The next source has no more tuples, so remove all remaining tuples ...
                            while (tupleIter.hasNext()) {
                                tupleIter.next();
                                tupleIter.remove();
                            }
                            // then go to the next source ...
                            break;
                        }
                        tuple2 = nextIter.next();
                    }
                }
            }
        }
        // Remove duplicates if requested to ...
        removeDuplicatesIfRequested(tuples);
        // Return all of the common tuples that were in all sources ...
        return tuples;
    }
}
