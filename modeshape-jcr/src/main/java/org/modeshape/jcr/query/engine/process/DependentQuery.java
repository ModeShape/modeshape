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
package org.modeshape.jcr.query.engine.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * A node sequence implementation that performs an independent query to find results that are required for a dependent query. At
 * this time, all of the results of the first query are kept in-memory, and stored in a variable where it is then accessed by the
 * dependent query.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class DependentQuery extends NodeSequence {
    protected static final Logger logger = Logger.getLogger(DependentQuery.class);

    private final NodeSequence independentQuery;
    private final ExtractFromRow independentQueryValueExtractor;
    private final TypeFactory<?> expectedType;
    private final NodeSequence dependentQuery;
    private final String variableName;
    private final Map<String, Object> variables;
    private boolean ranIndependentQuery;

    public DependentQuery( NodeSequence independentQuery,
                           ExtractFromRow independentQueryValueExtractor,
                           TypeFactory<?> expectedType,
                           NodeSequence dependentQuery,
                           String variableName,
                           Map<String, Object> variables ) {
        this.independentQuery = independentQuery;
        this.independentQueryValueExtractor = independentQueryValueExtractor;
        this.expectedType = expectedType;
        this.dependentQuery = dependentQuery;
        this.variableName = variableName;
        this.variables = variables;
    }

    @Override
    public long getRowCount() {
        initialize();
        return dependentQuery.getRowCount();
    }

    @Override
    public int width() {
        initialize();
        return dependentQuery.width();
    }

    @Override
    public boolean isEmpty() {
        initialize();
        return dependentQuery.isEmpty();
    }

    @Override
    public Batch nextBatch() {
        initialize();
        return dependentQuery.nextBatch();
    }

    protected void initialize() {
        if (ranIndependentQuery) return;
        // Perform the first query
        long size = independentQuery.getRowCount();
        Batch batch = independentQuery.nextBatch();
        List<Object> singleColumnResults = size < 0 ? new ArrayList<>() : new ArrayList<>((int)size);
        while (batch != null) {
            if (!batch.isEmpty()) {
                // Load all the rows from this batch ...
                while (batch.hasNext()) {
                    batch.nextRow();
                    Object value = independentQueryValueExtractor.getValueInRow(batch);
                    if (value instanceof Object[]) {
                        // Grab just the first non-null value ...
                        Object[] values = (Object[])value;
                        for (Object oneValue : values) {
                            if (oneValue == null) continue;
                            oneValue = expectedType.create(oneValue);
                            singleColumnResults.add(oneValue);
                            break;
                        }
                    } else {
                        value = expectedType.create(value);
                        singleColumnResults.add(value);
                    }
                }
            }
            batch = independentQuery.nextBatch();
        }

        if (logger.isTraceEnabled()) {
            LOGGER.trace("Saving variable '{0}' with values: {1}", variableName, singleColumnResults);
        }

        // Save the results to the variable ...
        variables.put(variableName, singleColumnResults);
        ranIndependentQuery = true;
    }

    @Override
    public void close() {
        RuntimeException error = null;
        try {
            independentQuery.close();
        } catch (RuntimeException e) {
            error = e;
        } finally {
            try {
                dependentQuery.close();
            } catch (RuntimeException e2) {
                if (error == null) error = e2;
            } finally {
                if (error != null) throw error;
            }
        }
    }

}
