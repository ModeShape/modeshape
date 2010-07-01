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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A {@link ProcessingComponent} implementation that performs a {@link Type#PROJECT PROJECT} operation to reduce the columns that
 * appear in the results.
 */
public class SortValuesComponent extends DelegatingComponent {

    private final Comparator<Object[]> sortingComparator;

    public SortValuesComponent( ProcessingComponent delegate,
                                List<Ordering> orderings,
                                Map<SelectorName, SelectorName> sourceNamesByAlias ) {
        super(delegate);
        this.sortingComparator = createSortComparator(delegate.getContext(), delegate.getColumns(), orderings, sourceNamesByAlias);
    }

    /**
     * @return sortingComparator
     */
    public Comparator<Object[]> getSortingComparator() {
        return sortingComparator;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.process.ProcessingComponent#execute()
     */
    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();
        if (tuples.size() > 1 && sortingComparator != null) {
            // Sort the tuples ...
            Collections.sort(tuples, sortingComparator);
        }
        return tuples;
    }

    protected Comparator<Object[]> createSortComparator( QueryContext context,
                                                         Columns columns,
                                                         List<Ordering> orderings,
                                                         Map<SelectorName, SelectorName> sourceNamesByAlias ) {
        assert context != null;
        assert orderings != null;
        if (orderings.isEmpty()) {
            return null;
        }
        if (orderings.size() == 1) {
            return createSortComparator(context, columns, orderings.get(0), sourceNamesByAlias);
        }
        // Create a comparator that uses an ordered list of comparators ...
        final List<Comparator<Object[]>> comparators = new ArrayList<Comparator<Object[]>>(orderings.size());
        for (Ordering ordering : orderings) {
            comparators.add(createSortComparator(context, columns, ordering, sourceNamesByAlias));
        }
        return new Comparator<Object[]>() {
            public int compare( Object[] tuple1,
                                Object[] tuple2 ) {
                for (Comparator<Object[]> comparator : comparators) {
                    int result = comparator.compare(tuple1, tuple2);
                    if (result != 0) return result;
                }
                return 0;
            }
        };
    }

    @SuppressWarnings( "unchecked" )
    protected Comparator<Object[]> createSortComparator( QueryContext context,
                                                         Columns columns,
                                                         Ordering ordering,
                                                         final Map<SelectorName, SelectorName> sourceNamesByAlias ) {
        assert context != null;
        assert ordering != null;
        final Schemata originalSchemata = context.getSchemata();
        final Schemata schemataWithAliases = sourceNamesByAlias == null ? originalSchemata : new Schemata() {
            public Table getTable( SelectorName name ) {
                // First assume that the name is an alias, so try resolving it first ...
                Table result = null;
                SelectorName unaliasedName = sourceNamesByAlias.get(name);
                if (unaliasedName != null) {
                    result = originalSchemata.getTable(unaliasedName);
                }
                if (result == null) {
                    // The name was not an alias, so use it to look up the table ...
                    result = originalSchemata.getTable(name);
                }
                return result;
            }
        };
        final DynamicOperation operation = createDynamicOperation(context.getTypeSystem(),
                                                                  schemataWithAliases,
                                                                  columns,
                                                                  ordering.operand());
        final TypeSystem typeSystem = context.getTypeSystem();
        final TypeFactory<?> typeFactory = typeSystem.getTypeFactory(operation.getExpectedType());
        assert typeFactory != null;
        final Comparator<Object> typeComparator = (Comparator<Object>)typeFactory.getComparator();
        assert typeComparator != null;
        if (ordering.order() == Order.DESCENDING) {
            return new Comparator<Object[]>() {
                public int compare( Object[] tuple1,
                                    Object[] tuple2 ) {
                    Object value1 = typeFactory.create(operation.evaluate(tuple1));
                    Object value2 = typeFactory.create(operation.evaluate(tuple2));
                    return 0 - typeComparator.compare(value1, value2);
                }
            };
        }
        return new Comparator<Object[]>() {
            public int compare( Object[] tuple1,
                                Object[] tuple2 ) {
                Object value1 = typeFactory.create(operation.evaluate(tuple1));
                Object value2 = typeFactory.create(operation.evaluate(tuple2));
                return typeComparator.compare(value1, value2);
            }
        };
    }
}
