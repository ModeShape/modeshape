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
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * A {@link ProcessingComponent} that executes a {@link Type#DEPENDENT_QUERY dependent query} node by first executing the left
 * component and then executing the right component. If a variable name is specified, this component will save the query results
 * from the the corresponding component into the {@link QueryContext#getVariables() variables} in the {@link QueryContext}.
 */
public class DependentQueryComponent extends ProcessingComponent {

    private final ProcessingComponent left;
    private final ProcessingComponent right;
    private final String leftVariableName;
    private final String rightVariableName;

    public DependentQueryComponent( QueryContext context,
                                    ProcessingComponent left,
                                    ProcessingComponent right,
                                    String leftVariableName,
                                    String rightVariableName ) {
        super(context, right.getColumns());
        this.left = left;
        this.right = right;
        this.leftVariableName = leftVariableName;
        this.rightVariableName = rightVariableName;
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
     * Get the columns definition for the results from the left, independent query that is processed first.
     * 
     * @return the left-side columns; never null
     */
    protected final Columns colunnsOfIndependentQuery() {
        return left.getColumns();
    }

    /**
     * Get the columns definition for the results from the right component that is dependent upon the left.
     * 
     * @return the right-side columns; never null
     */
    protected final Columns colunnsOfDependentQuery() {
        return right.getColumns();
    }

    @Override
    public List<Object[]> execute() {
        // First execute the left side ...
        List<Object[]> leftResults = left.execute();
        if (left.getColumns().getColumnCount() > 0) {
            saveResultsToVariable(leftResults, leftVariableName);
        }

        // Then execute the right side ...
        List<Object[]> rightResults = right.execute();
        if (right.getColumns().getColumnCount() > 0) {
            saveResultsToVariable(rightResults, rightVariableName);
        }
        return rightResults;
    }

    protected void saveResultsToVariable( List<Object[]> results,
                                          String variableName ) {
        if (results == null) return;
        if (variableName == null) return;

        // Grab the first value in each of the tuples, and set on the query context ...
        List<Object> singleColumnResults = new ArrayList<Object>(results.size());
        if (!results.isEmpty()) {
            // Make sure there is at least one column (in the first record; remaining tuples should be the same) ...
            Object[] firstTuple = results.get(0);
            if (firstTuple.length != 0) {
                for (Object[] tuple : results) {
                    singleColumnResults.add(tuple[0]);
                }
            }
        }
        // Place the single column results into the variable ...
        getContext().getVariables().put(variableName, singleColumnResults);
    }
}
