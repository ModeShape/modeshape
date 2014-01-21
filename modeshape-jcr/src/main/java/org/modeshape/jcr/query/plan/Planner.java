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
package org.modeshape.jcr.query.plan;

import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.QueryCommand;

/**
 * Interface for a query planner.
 */
public interface Planner {

    /**
     * Create a canonical query plan for the given command.
     * 
     * @param context the context in which the query is being planned
     * @param query the query command to be planned
     * @return the root node of the plan tree representing the canonical plan
     */
    public PlanNode createPlan( QueryContext context,
                                QueryCommand query );

}
