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
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.plan.PlanNode.Type;

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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.process.ProcessingComponent#execute()
     */
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
        if (results == null || results.isEmpty()) return;
        if (variableName == null) return;

        // Grab the first value in each of the tuples, and set on the query context ...
        List<Object> singleColumnResults = new ArrayList<Object>(results.size());
        // Make sure there is at least one column (in the first record; remaining tuples should be the same) ...
        Object[] firstTuple = results.get(0);
        if (firstTuple.length != 0) {
            for (Object[] tuple : results) {
                singleColumnResults.add(tuple[0]);
            }
        }
        // Place the single column results into the variable ...
        getContext().getVariables().put(variableName, singleColumnResults);
    }
}
