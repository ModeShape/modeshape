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
import java.util.List;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;

/**
 * 
 */
public class NestedLoopJoinComponent extends JoinComponent {

    public NestedLoopJoinComponent( QueryContext context,
                                    ProcessingComponent left,
                                    ProcessingComponent right,
                                    JoinCondition condition,
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
        final ValueSelector leftSelector = valueSelectorFor(left(), getJoinCondition());
        final ValueSelector rightSelector = valueSelectorFor(right(), getJoinCondition());
        final Joinable joinable = joinableFor(left(), right(), getJoinCondition());
        final TupleMerger merger = createMerger(getColumns(), left().getColumns(), right().getColumns());

        // Walk through the left and right results ...
        List<Object[]> leftTuples = left().execute();
        List<Object[]> rightTuples = right().execute();
        List<Object[]> tuples = new ArrayList<Object[]>(leftTuples.size() * rightTuples.size());
        for (Object[] leftTuple : leftTuples) {
            for (Object[] rightTuple : rightTuples) {
                // Get the value from the left and right side ...
                Object leftValue = leftSelector.evaluate(leftTuple);
                Object rightValue = rightSelector.evaluate(rightTuple);
                // Determine if the tuples should be joined ...
                if (joinable.evaluate(leftValue, rightValue)) {
                    Object[] result = merger.merge(leftTuple, rightTuple);
                    tuples.add(result);
                }
            }
        }
        return tuples;
    }
}
