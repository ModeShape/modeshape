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

import java.util.Comparator;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.DescendantNodeJoinCondition;
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.SameNodeJoinCondition;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueComparators;

/**
 * 
 */
public abstract class JoinComponent extends ProcessingComponent {

    protected static final Comparator<Location> LOCATION_COMPARATOR = Location.getComparator();

    private final ProcessingComponent left;
    private final ProcessingComponent right;
    private final JoinCondition condition;
    private final JoinType joinType;

    protected JoinComponent( QueryContext context,
                             ProcessingComponent left,
                             ProcessingComponent right,
                             JoinCondition condition,
                             JoinType joinType ) {
        super(context, left.getColumns().joinWith(right.getColumns()));
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.condition = condition;
        assert this.left != null;
        assert this.right != null;
        assert this.joinType != null;
    }

    /**
     * Get the type of join this processor represents.
     * 
     * @return the join type; never null
     */
    public final JoinType getJoinType() {
        return joinType;
    }

    /**
     * Get the join condition.
     * 
     * @return the join condition; never null
     */
    public final JoinCondition getJoinCondition() {
        return condition;
    }

    /**
     * Get the processing component that serves as the left side of the join.
     * 
     * @return the left-side processing component; never null
     */
    protected final ProcessingComponent left() {
        return left;
    }

    /**
     * Get the processing component that serves as the right side of the join.
     * 
     * @return the right-side processing component; never null
     */
    protected final ProcessingComponent right() {
        return right;
    }

    /**
     * Get the columns definition for the results from the left side of the join.
     * 
     * @return the left-side columns that feed this join; never null
     */
    protected final Columns leftColunns() {
        return left.getColumns();
    }

    /**
     * Get the columns definition for the results from the right side of the join.
     * 
     * @return the right-side columns that feed this join; never null
     */
    protected final Columns rightColumns() {
        return right.getColumns();
    }

    /**
     * Create a {@link TupleMerger} implementation that will combine a tuple fitting the left columns with a tuple fitting the
     * right columns. This merger will properly place all of the values, locations, and scores such that the tuples always have
     * this arrangement:
     * 
     * <pre>
     *    [ <i>v1</i>, <i>v2</i>, ..., <i>vM</i>, <i>loc1</i>, <i>loc2</i>, ..., <i>locN</i>, <i>s1</i>, <i>s2</i>, ..., <i>sN</i> ]
     * </pre>
     * 
     * where <i>M</i> is the number of values in the tuple, and <i>N</i> is the number of sources in the tuple.
     * <p>
     * Note that this merger does not actually reduce or combine values. That is done with a particular {@link JoinComponent}
     * subclass.
     * </p>
     * 
     * @param joinColumns the Columns specification for the joined/merged tuples; may not be null
     * @param leftColumns the Columns specification for the tuple on the left side of the join; may not be null
     * @param rightColumns the Columns specification for the tuple on the right side of the join; may not be null
     * @return the merger implementation that will combine tuples from the left and right to form the merged tuples; never null
     */
    protected static TupleMerger createMerger( Columns joinColumns,
                                               Columns leftColumns,
                                               Columns rightColumns ) {
        final int joinTupleSize = joinColumns.getTupleSize();
        final int joinColumnCount = joinColumns.getColumnCount();
        final int joinLocationCount = joinColumns.getLocationCount();
        final int leftColumnCount = leftColumns.getColumnCount();
        final int leftLocationCount = leftColumns.getLocationCount();
        final int leftTupleSize = leftColumns.getTupleSize();
        final int rightColumnCount = rightColumns.getColumnCount();
        final int rightLocationCount = rightColumns.getLocationCount();
        final int rightTupleSize = rightColumns.getTupleSize();
        final int startLeftLocations = joinColumnCount;
        final int startRightLocations = startLeftLocations + leftLocationCount;

        // The left and right selectors should NOT overlap ...
        assert joinLocationCount == leftLocationCount + rightLocationCount;

        // Create different implementations depending upon the options, since this save us from having to make
        // these decisions while doing the merges...
        if (joinColumns.hasFullTextSearchScores()) {
            final int leftScoreCount = leftTupleSize - leftColumnCount - leftLocationCount;
            final int rightScoreCount = rightTupleSize - rightColumnCount - rightLocationCount;
            final int startLeftScores = startRightLocations + rightLocationCount;
            final int startRightScores = startLeftScores + leftScoreCount;
            final int leftScoreIndex = leftTupleSize - leftScoreCount;
            final int rightScoreIndex = rightTupleSize - rightScoreCount;

            return new TupleMerger() {
                @Override
                public Object[] merge( Object[] leftTuple,
                                       Object[] rightTuple ) {
                    Object[] result = new Object[joinTupleSize]; // initialized to null
                    // If the tuple arrays are null, then we don't need to copy because the arrays are
                    // initialized to null values.
                    if (leftTuple != null) {
                        // Copy the left tuple values ...
                        System.arraycopy(leftTuple, 0, result, 0, leftColumnCount);
                        // Copy the left tuple locations ...
                        System.arraycopy(leftTuple, leftColumnCount, result, startLeftLocations, leftLocationCount);
                        // Copy the left tuple scores ...
                        System.arraycopy(leftTuple, leftScoreIndex, result, startLeftScores, leftScoreCount);
                    }
                    if (rightTuple != null) {
                        // Copy the right tuple values ...
                        System.arraycopy(rightTuple, 0, result, leftColumnCount, rightColumnCount);
                        // Copy the right tuple locations ...
                        System.arraycopy(rightTuple, rightColumnCount, result, startRightLocations, rightLocationCount);
                        // Copy the right tuple scores ...
                        System.arraycopy(rightTuple, rightScoreIndex, result, startRightScores, rightScoreCount);
                    }
                    return result;
                }
            };
        }
        // There are no full-text search scores ...
        return new TupleMerger() {
            @Override
            public Object[] merge( Object[] leftTuple,
                                   Object[] rightTuple ) {
                Object[] result = new Object[joinTupleSize]; // initialized to null
                // If the tuple arrays are null, then we don't need to copy because the arrays are
                // initialized to null values.
                if (leftTuple != null) {
                    // Copy the left tuple values ...
                    System.arraycopy(leftTuple, 0, result, 0, leftColumnCount);
                    System.arraycopy(leftTuple, leftColumnCount, result, startLeftLocations, leftLocationCount);
                }
                if (rightTuple != null) {
                    // Copy the right tuple values ...
                    System.arraycopy(rightTuple, 0, result, leftColumnCount, rightColumnCount);
                    System.arraycopy(rightTuple, rightColumnCount, result, startRightLocations, rightLocationCount);
                }
                return result;
            }
        };
    }

    /**
     * A component that will merge the supplied tuple on the left side of a join with the supplied tuple on the right side of the
     * join, to produce a single tuple with all components from the left and right tuples.
     */
    protected static interface TupleMerger {
        Object[] merge( Object[] leftTuple,
                        Object[] rightTuple );
    }

    /**
     * Interface defining the value of a tuple that is used in the join condition.
     */
    protected static interface ValueSelector {
        /**
         * Obtain the value that is to be used in the join condition.
         * 
         * @param tuple the tuple
         * @return the value that should be used
         */
        Object evaluate( Object[] tuple );
    }

    /**
     * Create a {@link ValueSelector} that obtains the value required to use the supplied join condition.
     * 
     * @param source the source component; may not be null
     * @param condition the join condition; may not be null
     * @return the value selector; never null
     */
    protected static ValueSelector valueSelectorFor( ProcessingComponent source,
                                                     JoinCondition condition ) {
        if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.childSelectorName().name();
            if (source.getColumns().hasSelector(childSelectorName)) {
                return selectPath(source, childSelectorName);
            }
            String parentSelectorName = joinCondition.parentSelectorName().name();
            return selectPath(source, parentSelectorName);
        } else if (condition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition joinCondition = (SameNodeJoinCondition)condition;
            String selector1Name = joinCondition.selector1Name().name();
            if (source.getColumns().hasSelector(selector1Name)) {
                return selectPath(source, selector1Name);
            }
            String selector2Name = joinCondition.selector2Name().name();
            return selectPath(source, selector2Name);
        } else if (condition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition joinCondition = (DescendantNodeJoinCondition)condition;
            String ancestorSelectorName = joinCondition.ancestorSelectorName().name();
            if (source.getColumns().hasSelector(ancestorSelectorName)) {
                return selectPath(source, ancestorSelectorName);
            }
            String descendantSelectorName = joinCondition.descendantSelectorName().name();
            return selectPath(source, descendantSelectorName);
        } else if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition joinCondition = (EquiJoinCondition)condition;
            SelectorName selector1Name = joinCondition.selector1Name();
            String propName1 = joinCondition.getProperty1Name();
            if (source.getColumns().hasSelector(selector1Name.name())) {
                return selectValue(source, selector1Name, propName1);
            }
            SelectorName selector2Name = joinCondition.selector2Name();
            String propName2 = joinCondition.getProperty2Name();
            return selectValue(source, selector2Name, propName2);
        }
        throw new IllegalArgumentException();
    }

    private static ValueSelector selectPath( ProcessingComponent component,
                                             String selectorName ) {
        final int index = component.getColumns().getLocationIndex(selectorName);
        return new ValueSelector() {
            @Override
            public Object evaluate( Object[] tuple ) {
                return tuple[index]; // Location
            }
        };
    }

    private static ValueSelector selectValue( ProcessingComponent component,
                                              SelectorName selectorName,
                                              String propertyName ) {
        final int index = component.getColumns().getColumnIndexForProperty(selectorName.name(), propertyName);
        return new ValueSelector() {
            @Override
            public Object evaluate( Object[] tuple ) {
                return tuple[index];
            }
        };
    }

    /**
     * Interface defining the value of a tuple that is used in the join condition.
     */
    protected static interface Joinable {
        /**
         * Obtain the value that is to be used in the join condition.
         * 
         * @param leftValue the value from the left tuple; never null
         * @param rightValue the value from the right tuple; never null
         * @return true if the tuples are to be joined
         */
        boolean evaluate( Object leftValue,
                          Object rightValue );
    }

    /**
     * Create a {@link ValueSelector} that obtains the value required to use the supplied join condition.
     * 
     * @param left the left source component; may not be null
     * @param right the left source component; may not be null
     * @param condition the join condition; may not be null
     * @return the value selector; never null
     */
    protected static Joinable joinableFor( ProcessingComponent left,
                                           ProcessingComponent right,
                                           JoinCondition condition ) {
        if (condition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition joinCondition = (SameNodeJoinCondition)condition;
            if (left.getColumns().hasSelector(joinCondition.getSelector1Name())) {
                final String relPathStr = joinCondition.getSelector2Path();
                final PathFactory pathFactory = right.getContext().getExecutionContext().getValueFactories().getPathFactory();
                final Path relPath = pathFactory.create(relPathStr);
                if (relPath == null || relPath.isAbsolute()
                    || (relPath.size() == 1 && relPath.getLastSegment().isSelfReference())) {
                    return new Joinable() {
                        @Override
                        public boolean evaluate( Object locationA,
                                                 Object locationB ) {
                            Location location1 = (Location)locationA;
                            Location location2 = (Location)locationB;
                            return location1 != null && location1.isSame(location2);
                        }
                    };
                }
                // Get the path factory for the right-side ...
                return new Joinable() {
                    @Override
                    public boolean evaluate( Object locationA,
                                             Object locationB ) {
                        Location location1 = (Location)locationA;
                        Location location2 = (Location)locationB;
                        Path path1 = location1.getPath();
                        if (path1 == null) return false;
                        Path path2a = location2.getPath();
                        if (path2a == null) return false;
                        Path path2 = pathFactory.create(path2a, relPath);
                        return path2.isSameAs(path1);
                    }
                };
            }
            // This must be a right outer join that was reversed in query-plan optimization ...
            final String relPathStr = joinCondition.getSelector2Path();
            final PathFactory pathFactory = left.getContext().getExecutionContext().getValueFactories().getPathFactory();
            final Path relPath = pathFactory.create(relPathStr);
            if (relPath == null || relPath.isAbsolute() || (relPath.size() == 1 && relPath.getLastSegment().isSelfReference())) {
                return new Joinable() {
                    @Override
                    public boolean evaluate( Object locationA,
                                             Object locationB ) {
                        Location location1 = (Location)locationA;
                        Location location2 = (Location)locationB;
                        return location1 != null && location1.isSame(location2);
                    }
                };
            }
            // Get the path factory for the left-side ...
            return new Joinable() {
                @Override
                public boolean evaluate( Object locationA,
                                         Object locationB ) {
                    Location location1 = (Location)locationA;
                    Location location2 = (Location)locationB;
                    Path path1a = location1.getPath();
                    if (path1a == null) return false;
                    Path path2 = location2.getPath();
                    if (path2 == null) return false;
                    Path path1 = pathFactory.create(path1a, relPath);
                    return path2.isSameAs(path1);
                }
            };

        } else if (condition instanceof EquiJoinCondition) {
            return new Joinable() {
                @Override
                public boolean evaluate( Object leftValue,
                                         Object rightValue ) {
                    // Standard equi-joins treat nulls differently than one might expect:
                    //
                    // "NULL will never match any other value (not even NULL itself), unless the join condition
                    // explicitly uses the IS NULL or IS NOT NULL predicates." JCR doesn't have "IS NULL" or "IS NOT NULL"
                    // support for the join conditions.
                    //
                    // See http://en.wikipedia.org/wiki/Join_(SQL)#Inner_join
                    if (leftValue == null) return false;
                    if (leftValue instanceof Object[]) {
                        // Look for a single match in one of the values ...
                        for (Object leftV : (Object[])leftValue) {
                            if (leftV.equals(rightValue)) return true;
                        }
                        return false;
                    }
                    return leftValue.equals(rightValue);
                }
            };
        } else if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.childSelectorName().name();
            if (left.getColumns().hasSelector(childSelectorName)) {
                // The child is on the left ...
                return new Joinable() {
                    @Override
                    public boolean evaluate( Object childLocation,
                                             Object parentLocation ) {
                        if (childLocation == null || parentLocation == null) return false;
                        Path childPath = ((Location)childLocation).getPath();
                        Path parentPath = ((Location)parentLocation).getPath();
                        if (childPath.isRoot()) return false;
                        return parentPath.isSameAs(childPath.getParent());
                    }
                };
            }
            // The child is on the right ...
            return new Joinable() {
                @Override
                public boolean evaluate( Object parentLocation,
                                         Object childLocation ) {
                    if (childLocation == null || parentLocation == null) return false;
                    Path childPath = ((Location)childLocation).getPath();
                    Path parentPath = ((Location)parentLocation).getPath();
                    if (childPath.isRoot()) return false;
                    return parentPath.isSameAs(childPath.getParent());
                }
            };
        } else if (condition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition joinCondition = (DescendantNodeJoinCondition)condition;
            String ancestorSelectorName = joinCondition.ancestorSelectorName().name();
            if (left.getColumns().hasSelector(ancestorSelectorName)) {
                // The ancestor is on the left ...
                return new Joinable() {
                    @Override
                    public boolean evaluate( Object ancestorLocation,
                                             Object descendantLocation ) {
                        if (ancestorLocation == null || descendantLocation == null) return false;
                        Path ancestorPath = ((Location)ancestorLocation).getPath();
                        Path descendantPath = ((Location)descendantLocation).getPath();
                        return ancestorPath.isAncestorOf(descendantPath);
                    }
                };
            }
            // The ancestor is on the right ...
            return new Joinable() {
                @Override
                public boolean evaluate( Object descendantLocation,
                                         Object ancestorLocation ) {
                    if (ancestorLocation == null || descendantLocation == null) return false;
                    Path ancestorPath = ((Location)ancestorLocation).getPath();
                    Path descendantPath = ((Location)descendantLocation).getPath();
                    return ancestorPath.isAncestorOf(descendantPath);
                }
            };
        }
        throw new IllegalArgumentException();
    }

    protected static Comparator<Object> arrayAwareComparator( final Comparator<Object> comparator ) {
        return new Comparator<Object>() {
            @Override
            public int compare( Object o1,
                                Object o2 ) {
                // Standard equi-joins treat nulls differently than one might expect:
                //
                // "NULL will never match any other value (not even NULL itself), unless the join condition
                // explicitly uses the IS NULL or IS NOT NULL predicates." JCR doesn't have "IS NULL" or "IS NOT NULL"
                // support for the join conditions.
                //
                // See http://en.wikipedia.org/wiki/Join_(SQL)#Inner_join
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                if (o1 instanceof Object[]) {
                    // Look for a single match in one of the values ...
                    int firstResult = 0;
                    boolean first = false;
                    for (Object leftValue : (Object[])o1) {
                        int result = comparator.compare(leftValue, o2);
                        if (result == 0) return 0;
                        if (first) firstResult = result;
                    }
                    return firstResult;
                }
                return comparator.compare(o1, o2);
            }
        };
    }

    /**
     * Create a {@link Comparable} that can be used to compare the values required to evaluate the supplied join condition.
     * 
     * @param context the context in which this query is being evaluated; may not be null
     * @param left the left source component; may not be null
     * @param right the left source component; may not be null
     * @param condition the join condition; may not be null
     * @return the comparator; never null
     */
    @SuppressWarnings( "unchecked" )
    protected static Comparator<Object> comparatorFor( QueryContext context,
                                                       ProcessingComponent left,
                                                       ProcessingComponent right,
                                                       JoinCondition condition ) {
        final Comparator<Path> pathComparator = ValueComparators.PATH_COMPARATOR;
        if (condition instanceof SameNodeJoinCondition) {
            return new Comparator<Object>() {
                @Override
                public int compare( Object location1,
                                    Object location2 ) {
                    if (location1 == null) return location2 == null ? 0 : -1;
                    else if (location2 == null) return 1;
                    Path path1 = ((Location)location1).getPath();
                    Path path2 = ((Location)location2).getPath();
                    return pathComparator.compare(path1, path2);
                }
            };
        }
        if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.childSelectorName().name();
            if (left.getColumns().hasSelector(childSelectorName)) {
                // The child is on the left ...
                return new Comparator<Object>() {
                    @Override
                    public int compare( Object childLocation,
                                        Object parentLocation ) {
                        if (childLocation == null) return parentLocation == null ? 0 : -1;
                        else if (parentLocation == null) return 1;
                        Path childPath = ((Location)childLocation).getPath();
                        Path parentPath = ((Location)parentLocation).getPath();
                        if (childPath.isRoot()) return parentPath.isRoot() ? 0 : -1;
                        Path parentOfChild = childPath.getParent();
                        return pathComparator.compare(parentPath, parentOfChild);
                    }
                };
            }
            // The child is on the right ...
            return new Comparator<Object>() {
                @Override
                public int compare( Object parentLocation,
                                    Object childLocation ) {
                    if (parentLocation == null) return 0;
                    else if (childLocation == null) return 1;
                    Path childPath = ((Location)childLocation).getPath();
                    Path parentPath = ((Location)parentLocation).getPath();
                    if (childPath.isRoot()) return parentPath.isRoot() ? 0 : -1;
                    Path parentOfChild = childPath.getParent();
                    return pathComparator.compare(parentPath, parentOfChild);
                }
            };
        }
        if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition joinCondition = (EquiJoinCondition)condition;
            SelectorName leftSelectorName = joinCondition.selector1Name();
            SelectorName rightSelectorName = joinCondition.selector2Name();
            String leftPropertyName = joinCondition.getProperty1Name();
            String rightPropertyName = joinCondition.getProperty2Name();

            Schemata schemata = context.getSchemata();
            TypeSystem typeSystem = context.getTypeSystem();

            Schemata.Table leftTable = schemata.getTable(leftSelectorName);
            Schemata.Column leftColumn = leftTable.getColumn(leftPropertyName);
            if (leftColumn == null) {
                return typeSystem.getDefaultComparator();
            }
            String leftType = leftColumn.getPropertyTypeName();

            Schemata.Table rightTable = schemata.getTable(rightSelectorName);
            Schemata.Column rightColumn = rightTable.getColumn(rightPropertyName);
            if (rightColumn == null) {
                return typeSystem.getDefaultComparator();
            }
            String rightType = rightColumn.getPropertyTypeName();

            if (leftType.equals(rightType)) {
                TypeFactory<?> typeFactory = typeSystem.getTypeFactory(leftType);
                if (typeFactory != null) {
                    return (Comparator<Object>)typeFactory.getComparator();
                }
            }
            return typeSystem.getDefaultComparator();
        }
        throw new IllegalArgumentException();
    }
}
