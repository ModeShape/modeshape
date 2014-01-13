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

import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Ordering;

/**
 * Represents the abstract base class for all query commands. Subclasses include {@link SetQuery} and {@link SelectQuery}.
 */
public interface QueryCommand {

    /**
     * Gets the orderings for this query.
     * 
     * @return an array of zero or more orderings; non-null
     */
    public Ordering[] getOrderings();

    /**
     * Gets the columns for this query.
     * 
     * @return an array of zero or more columns; non-null
     */
    public Column[] getColumns();

    /**
     * Get the limits associated with this query.
     * 
     * @return the limits; never null but possibly {@link Limit#isUnlimited() unlimited}
     */
    public Limit getLimits();
}
