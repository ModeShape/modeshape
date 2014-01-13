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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Source;

/**
 * Represents a typical select-style query.
 */
public interface SelectQuery extends QueryCommand {
    /**
     * Gets the node-tuple source for this query.
     * 
     * @return the node-tuple source; non-null
     */
    public Source getSource();

    /**
     * Gets the constraint for this query.
     * 
     * @return the constraint, or null if none
     */
    public Constraint getConstraint();
}
