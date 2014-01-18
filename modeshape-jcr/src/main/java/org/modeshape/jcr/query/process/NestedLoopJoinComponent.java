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
import java.util.List;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.JoinType;

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

    @Override
    public List<Object[]> execute() {
        // Construct the necessary components ...
        final ValueSelector leftSelector = valueSelectorFor(left(), getJoinCondition());
        final ValueSelector rightSelector = valueSelectorFor(right(), getJoinCondition());
        final Joinable joinable = joinableFor(left(), right(), getJoinCondition());
        final JoinType joinType = getJoinType();
        final TupleMerger merger = createMerger(getColumns(), left().getColumns(), right().getColumns());

        // Walk through the left and right results ...
        List<Object[]> leftTuples = left().execute();
        List<Object[]> rightTuples = right().execute();
        List<Object[]> tuples = null;
        switch (joinType) {
            case INNER:
                // Note that in SQL joins, a NULL value on one side of the join criteria is not considered equal to
                // a NULL value on the other side. Therefore, in the following algorithms, we're shortcutting the
                // loops as soon as we get any NULL value for the join criteria.
                // see http://en.wikipedia.org/wiki/Join_(SQL)#Inner_join

                // Must match tuples on left with those on right, so we'll have no more result tuples than the
                // minumum number of tuples on the left or right...
                int maxSize = Math.min(leftTuples.size(), rightTuples.size());
                tuples = new ArrayList<Object[]>(maxSize);
                // Iterate through all the tuples on the left ...
                for (Object[] leftTuple : leftTuples) {
                    Object leftValue = leftSelector.evaluate(leftTuple);
                    if (leftValue == null) {
                        continue;
                    }

                    // And then find the matching ones on the right ...
                    for (Object[] rightTuple : rightTuples) {
                        // Get the value from the left and right side ...
                        Object rightValue = rightSelector.evaluate(rightTuple);
                        if (rightValue == null) {
                            continue;
                        }

                        // Determine if the tuples should be joined ...
                        if (joinable.evaluate(leftValue, rightValue)) {
                            Object[] result = merger.merge(leftTuple, rightTuple);
                            tuples.add(result);
                        }
                    }
                }
                break;
            case LEFT_OUTER:
                // We'll have all the tuples on the left, with any of those on the right that match ...
                maxSize = leftTuples.size();
                tuples = new ArrayList<Object[]>(maxSize);
                // Iterate through all the tuples on the left ...
                for (Object[] leftTuple : leftTuples) {
                    Object leftValue = leftSelector.evaluate(leftTuple);

                    // And then find the matching ones on the right ...
                    boolean foundMatch = false;
                    if (leftValue != null) {
                        for (Object[] rightTuple : rightTuples) {
                            // Get the value from the left and right side ...
                            Object rightValue = rightSelector.evaluate(rightTuple);
                            if (rightValue == null) {
                                continue;
                            }

                            // Determine if the tuples should be joined ...
                            if (joinable.evaluate(leftValue, rightValue)) {
                                Object[] result = merger.merge(leftTuple, rightTuple);
                                tuples.add(result);
                                foundMatch = true;
                            }
                        }
                    }
                    // We've processed all the tuples on the right, and if we've not yet found a match
                    // we still need to include the left tuple (but we only want to include it once) ...
                    if (!foundMatch) {
                        tuples.add(merger.merge(leftTuple, null));
                    }
                }
                break;
            case RIGHT_OUTER:
                // We'll have all the tuples on the right, with any of those on the left that match ...
                maxSize = rightTuples.size();
                tuples = new ArrayList<Object[]>(maxSize);
                // Iterate through all the tuples on the right ...
                for (Object[] rightTuple : rightTuples) {
                    Object rightValue = rightSelector.evaluate(rightTuple);

                    // And then find the matching ones on the right ...
                    boolean foundMatch = false;
                    if (rightValue != null) {
                        for (Object[] leftTuple : leftTuples) {
                            // Get the value from the left and right side ...
                            Object leftValue = leftSelector.evaluate(leftTuple);
                            if (leftValue == null) {
                                continue;
                            }

                            // Determine if the tuples should be joined ...
                            if (joinable.evaluate(leftValue, rightValue)) {
                                Object[] result = merger.merge(leftTuple, rightTuple);
                                tuples.add(result);
                                foundMatch = true;
                            }
                        }
                    }
                    // We've processed all the tuples on the left, and if we've not yet found a match
                    // we still need to include the right tuple (but we only want to include it once) ...
                    if (!foundMatch) {
                        tuples.add(merger.merge(null, rightTuple));
                    }
                }
                break;
            case FULL_OUTER:
                // We'll have all the tuples on the right and all the tuples on the left, and in the worst case
                // none will match, so we'll start with the sum of the number of tuples on the left plus the number
                // on the right ...
                maxSize = leftTuples.size() + rightTuples.size();
                tuples = new ArrayList<Object[]>(maxSize);
                // Iterate through all the tuples on the left ...
                for (Object[] leftTuple : leftTuples) {
                    Object leftValue = leftSelector.evaluate(leftTuple);

                    // And then find the matching ones on the right ...
                    boolean foundMatch = false;
                    for (Object[] rightTuple : rightTuples) {
                        // Get the value from the left and right side ...
                        Object rightValue = rightSelector.evaluate(rightTuple);
                        if (rightValue == null) continue;

                        // Determine if the tuples should be joined ...
                        if (joinable.evaluate(leftValue, rightValue)) {
                            Object[] result = merger.merge(leftTuple, rightTuple);
                            tuples.add(result);
                            foundMatch = true;
                        } else {
                            // Otherwise, we still return the right tuple ...
                            tuples.add(merger.merge(null, rightTuple));
                        }
                    }
                    // We've processed all the tuples on the right, and if we've not yet found a match
                    // we still need to include the left tuple (but we only want to include it once) ...
                    if (!foundMatch) {
                        tuples.add(merger.merge(leftTuple, null));
                    }
                }
                break;
            case CROSS:
                // A cross join results in the Cartesian product, so each tuple on the left will be combined with
                // each tuple on the right ...
                maxSize = leftTuples.size() * rightTuples.size();
                tuples = new ArrayList<Object[]>(maxSize);
                // Iterate through all the tuples on the left ...
                for (Object[] leftTuple : leftTuples) {
                    // And for each iterate through all the tuples on the left ...
                    for (Object[] rightTuple : rightTuples) {
                        // We always use both tuples ...
                        Object[] result = merger.merge(leftTuple, rightTuple);
                        tuples.add(result);
                    }
                }
                break;

        }
        assert tuples != null;
        return tuples;
    }
}
