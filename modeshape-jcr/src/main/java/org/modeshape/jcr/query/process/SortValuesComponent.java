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
import java.util.List;
import java.util.Map;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

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
            @Override
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
            @Override
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
                                                                  ordering.getOperand());
        final TypeSystem typeSystem = context.getTypeSystem();
        final TypeFactory<?> typeFactory = typeSystem.getTypeFactory(operation.getExpectedType());
        assert typeFactory != null;
        final Comparator<Object> typeComparator = (Comparator<Object>)typeFactory.getComparator();
        assert typeComparator != null;
        if (ordering.order() == Order.DESCENDING) {
            return new Comparator<Object[]>() {
                @Override
                public int compare( Object[] tuple1,
                                    Object[] tuple2 ) {
                    Object value1 = typeFactory.create(operation.evaluate(tuple1));
                    Object value2 = typeFactory.create(operation.evaluate(tuple2));
                    return 0 - typeComparator.compare(value1, value2);
                }
            };
        }
        return new Comparator<Object[]>() {
            @Override
            public int compare( Object[] tuple1,
                                Object[] tuple2 ) {
                Object value1 = typeFactory.create(operation.evaluate(tuple1));
                Object value2 = typeFactory.create(operation.evaluate(tuple2));
                return typeComparator.compare(value1, value2);
            }
        };
    }
}
