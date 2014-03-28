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

package org.modeshape.jcr.spi.index.provider;

import java.util.Collection;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;

/**
 * The filter containing a set of constraints.
 * <p>
 * Instances of this type are created by ModeShape and passed into the {@link Index#filter(IndexFilter)} method. Thus,
 * providers do not need to implement this interface (except maybe for testing purposes).
 * </p>
 * 
 * @see Index#filter(IndexFilter)
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public
interface IndexFilter {
    /**
     * Return whether this filter contains constraints. This is identical to calling "<code>!getConstraints().isEmpty()</code>
     * ".
     * 
     * @return true if there is at least one constraint, or false if there are none
     */
    boolean hasConstraints();

    /**
     * Get the constraints for this filter.
     * 
     * @return the constraints; never null but maybe empty
     */
    Collection<Constraint> getConstraints();

    /**
     * Get the parameters for this filter operation, as determined during {@link IndexPlanner#applyIndexes}
     * 
     * @return the parameters; never null but may be empty
     */
    Map<String, Object> getParameters();
}