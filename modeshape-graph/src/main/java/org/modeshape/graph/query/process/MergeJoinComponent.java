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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.Location;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.SameNodeJoinCondition;

/**
 * Create a processing component that performs a merge-join algorithm. This algorithm only makes sense for
 * {@link EquiJoinCondition equi-joins}, {@link ChildNodeJoinCondition child-node joins}, and {@link SameNodeJoinCondition
 * same-node joins}.
 * <p>
 * <b> Note that it is required that the left and right processing components (where this component gets its results) must already
 * be properly sorted and have had duplicates removed. </b>
 * </p>
 */
@Immutable
public class MergeJoinComponent extends JoinComponent {

    public MergeJoinComponent( QueryContext context,
                               ProcessingComponent left,
                               ProcessingComponent right,
                               EquiJoinCondition condition,
                               JoinType joinType ) {
        super(context, left, right, condition, joinType);
    }

    public MergeJoinComponent( QueryContext context,
                               ProcessingComponent left,
                               ProcessingComponent right,
                               ChildNodeJoinCondition condition,
                               JoinType joinType ) {
        super(context, left, right, condition, joinType);
    }

    public MergeJoinComponent( QueryContext context,
                               ProcessingComponent left,
                               ProcessingComponent right,
                               SameNodeJoinCondition condition,
                               JoinType joinType ) {
        super(context, left, right, condition, joinType);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.process.ProcessingComponent#execute()
     */
    @Override
    public List<Object[]> execute() {
        // Construct the necessary components ...
        final Columns leftColumns = left().getColumns();
        final Columns rightColumns = right().getColumns();
        final ValueSelector leftSelector = valueSelectorFor(left(), getJoinCondition());
        final ValueSelector rightSelector = valueSelectorFor(right(), getJoinCondition());
        final Comparator<Object> comparator = comparatorFor(getContext(), left(), right(), getJoinCondition());
        final TupleMerger merger = createMerger(getColumns(), leftColumns, rightColumns);

        // Walk through the left and right results ...
        List<Object[]> leftTuples = left().execute();
        List<Object[]> rightTuples = right().execute();
        List<Object[]> tuples = new ArrayList<Object[]>(leftTuples.size() * rightTuples.size());
        Iterator<Object[]> leftIter = leftTuples.iterator();
        Iterator<Object[]> rightIter = rightTuples.iterator();
        Object[] leftTuple = leftIter.next();
        Object[] rightTuple = rightIter.next();
        Object[] nextLeftTuple = null;
        Object[] nextRightTuple = null;
        while (true) {
            // Get the value from the left and right side ...
            Object leftValue = leftSelector.evaluate(leftTuple);
            Object rightValue = rightSelector.evaluate(rightTuple);
            // Determine if the tuples should be joined ...
            int compare = comparator.compare(leftValue, rightValue);
            while (compare == 0) {
                Object[] result = merger.merge(leftTuple, rightTuple);
                tuples.add(result);

                // Peek at the next right tuple, but skip any duplicates ...
                if (nextRightTuple == null) {
                    nextRightTuple = rightIter.next();
                    while (isSameTuple(rightColumns, nextRightTuple, rightTuple)) {
                        nextRightTuple = rightIter.next();
                    }
                }

                // Compare the leftTuple with the nextRightTuple ...
                leftValue = leftSelector.evaluate(leftTuple);
                rightValue = rightSelector.evaluate(nextRightTuple);
                compare = comparator.compare(leftValue, rightValue);
                if (compare == 0) {
                    // This is a good match, update the variables and repeat ...
                    rightTuple = nextRightTuple;
                    nextRightTuple = null;
                    continue;
                }
                if (compare > 0) {
                    // The rightTuple is smaller than the left, so we need to increment the right again ...
                    rightTuple = nextRightTuple;
                    nextRightTuple = null;
                    compare = 0; // to prevent iteration below ...
                    break;
                }

                // Otherwise, the leftTuple didn't work with the nextRightTuple,
                // so try the nextLeftTuple with the rightTuple ...
                nextLeftTuple = leftIter.next();
                while (isSameTuple(leftColumns, nextLeftTuple, leftTuple)) {
                    nextLeftTuple = leftIter.next();
                }
                leftValue = leftSelector.evaluate(nextLeftTuple);
                rightValue = rightSelector.evaluate(rightTuple);
                compare = comparator.compare(leftValue, rightValue);
                if (compare == 0) {
                    // This is a good match, so update the variables and repeat ...
                    leftTuple = nextLeftTuple;
                    nextLeftTuple = null;
                    continue;
                }

                // Otherwise, neither was a good match, so advance both ...
                leftTuple = nextLeftTuple;
                rightTuple = nextRightTuple;
                nextLeftTuple = null;
                nextRightTuple = null;
                compare = 0; // to prevent iteration below ...
                break;
            }
            // There wasn't a match ...
            if (compare < 0) {
                // The leftValue is smaller than the right, so we need to increment the left ...
                if (!leftIter.hasNext()) break;
                leftTuple = leftIter.next();
            }
            if (compare > 0) {
                // The rightValue is smaller than the left, so we need to increment the right ...
                if (!rightIter.hasNext()) break;
                rightTuple = rightIter.next();
            }
        }
        return tuples;
    }

    protected final boolean isSameTuple( Columns columns,
                                         Object[] tuple1,
                                         Object[] tuple2 ) {
        for (int i = columns.getColumnCount(); i != columns.getLocationCount(); ++i) {
            Location location = (Location)tuple1[i];
            Location location2 = (Location)tuple2[i];
            if (!location.isSame(location2)) return false;
        }
        return true;
    }
}
