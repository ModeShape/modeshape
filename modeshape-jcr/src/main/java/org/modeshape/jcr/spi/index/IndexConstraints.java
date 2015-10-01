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

package org.modeshape.jcr.spi.index;

import java.util.Collection;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.spi.index.provider.Filter;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.value.ValueFactories;

/**
 * The a set of constraints that ModeShape passes to {@link Filter} instances via the {@link Filter#filter(IndexConstraints)}
 * method.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public interface IndexConstraints {
    /**
     * Return whether this filter contains constraints. This is identical to calling "<code>!getConstraints().isEmpty()</code> ".
     * 
     * @return true if there is at least one constraint, or false if there are none
     */
    boolean hasConstraints();

    /**
     * Get the constraints that apply to the index to which this filter is submitted.
     * 
     * @return the constraints; never null but maybe empty
     */
    Collection<Constraint> getConstraints();

    /**
     * Get the join conditions that apply to the index to which this filter is submitted.
     * 
     * @return the conditions; never null but maybe empty
     */
    Collection<JoinCondition> getJoinConditions();

    /**
     * Get the variables that are to be substituted into the {@link BindVariableName} used in the query.
     * 
     * @return immutable map of variable values keyed by their name; never null but possibly empty
     */
    Map<String, Object> getVariables();

    /**
     * Get the factories for values of various types.
     * 
     * @return the type-based factories; never null
     */
    ValueFactories getValueFactories();

    /**
     * Get the parameters for this filter operation that were set during this provider's {@link IndexPlanner#applyIndexes}
     * 
     * @return the parameters; never null but may be empty
     */
    Map<String, Object> getParameters();
}
