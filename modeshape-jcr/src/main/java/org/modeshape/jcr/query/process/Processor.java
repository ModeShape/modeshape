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

import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Statistics;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.plan.PlanNode;

/**
 * Interface for a query processor.
 */
public interface Processor {

    /**
     * Process the supplied query plan for the given command and return the results.
     * 
     * @param context the context in which the command is being processed
     * @param command the command being executed
     * @param statistics the time metrics up until this execution
     * @param plan the plan to be processed
     * @return the results of the query
     */
    QueryResults execute( QueryContext context,
                          QueryCommand command,
                          Statistics statistics,
                          PlanNode plan );
}
