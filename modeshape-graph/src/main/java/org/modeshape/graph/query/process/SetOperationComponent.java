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
package org.modeshape.graph.query.process;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;

/**
 */
public abstract class SetOperationComponent extends ProcessingComponent {

    private final Iterable<ProcessingComponent> sources;
    protected final Comparator<Object[]> removeDuplicatesComparator;

    protected SetOperationComponent( QueryContext context,
                                     Columns columns,
                                     Iterable<ProcessingComponent> sources,
                                     boolean alreadySorted,
                                     boolean all ) {
        super(context, columns);
        assert unionCompatible(columns, sources);
        this.sources = wrapWithLocationOrdering(sources, alreadySorted);
        this.removeDuplicatesComparator = all ? null : createSortComparator(context, columns);
    }

    protected static boolean unionCompatible( Columns columns,
                                              Iterable<ProcessingComponent> sources ) {
        for (ProcessingComponent source : sources) {
            if (!columns.isUnionCompatible(source.getColumns())) return false;
        }
        return true;
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
