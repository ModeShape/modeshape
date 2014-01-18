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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.TupleReformatter;

/**
 */
public abstract class SetOperationComponent extends ProcessingComponent {

    private final Iterable<ProcessingComponent> sources;
    protected final List<TupleReformatter> sourceReformatters;
    protected final Comparator<Object[]> removeDuplicatesComparator;
    private final Columns columns;

    protected SetOperationComponent( QueryContext context,
                                     Columns columns,
                                     Iterable<ProcessingComponent> sources,
                                     boolean alreadySorted,
                                     boolean all ) {
        super(context, columns);
        assert unionCompatible(columns, sources);
        this.sources = wrapWithLocationOrdering(sources, alreadySorted);
        // Use for sorting the columns that may have been wrapped with location ordering ...
        List<TupleReformatter> reformatters = new ArrayList<TupleReformatter>();
        Columns reformattedColumns = null;
        for (ProcessingComponent component : sources) {
            TupleReformatter reformatter = component.getColumns().getTupleReformatter();
            reformatters.add(reformatter);
            if (reformattedColumns == null && reformatter != null) {
                reformattedColumns = reformatter.getColumns();
            }
        }
        Columns newColumns = this.sources.iterator().next().getColumns();
        if (Collections.frequency(reformatters, reformatters.get(0)) == reformatters.size()) {
            // They are all the same, so don't reformat anything ...
            this.sourceReformatters = Collections.nCopies(reformatters.size(), null);
        } else {
            // We have to reformat the tuples, and use the corresponding columns with proper indexes
            this.sourceReformatters = reformatters;
            if (reformattedColumns != null) newColumns = reformattedColumns;
        }
        this.columns = newColumns;
        this.removeDuplicatesComparator = all ? null : createSortComparator(context, this.columns);
    }

    protected static boolean unionCompatible( Columns columns,
                                              Iterable<ProcessingComponent> sources ) {
        for (ProcessingComponent source : sources) {
            if (!columns.isUnionCompatible(source.getColumns())) return false;
        }
        return true;
    }

    @Override
    public Columns getColumns() {
        return columns;
    }

    /**
     * @return sources
     */
    protected Iterable<ProcessingComponent> sources() {
        return sources;
    }

    /**
     * The sources' results must be sorted before the intersection can be computed. Ensure that the sources' results are indeed
     * sorted, and if not wrap them in a sorting component.
     * 
     * @param sources the sources being intersected; may not be null
     * @param alreadySorted true if the sources' results are already sorted, or false if they must be sorted by this component
     * @return the sources (or their wrappers); never null
     */
    protected static Iterable<ProcessingComponent> wrapWithLocationOrdering( Iterable<ProcessingComponent> sources,
                                                                             boolean alreadySorted ) {
        assert sources != null;
        if (alreadySorted) return sources;
        List<ProcessingComponent> wrapped = new LinkedList<ProcessingComponent>();
        for (ProcessingComponent source : sources) {
            wrapped.add(new SortLocationsComponent(source));
        }
        return wrapped;
    }

    protected void removeDuplicatesIfRequested( List<Object[]> tuples ) {
        if (removeDuplicatesComparator != null) {
            Iterator<Object[]> iter = tuples.iterator();
            Object[] previous = null;
            while (iter.hasNext()) {
                Object[] current = iter.next();
                if (previous != null && removeDuplicatesComparator.compare(previous, current) == 0) {
                    iter.remove();
                } else {
                    previous = current;
                }
            }
        }
    }
}
