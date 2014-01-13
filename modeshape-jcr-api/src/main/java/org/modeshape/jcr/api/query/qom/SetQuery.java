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

/**
 * Represents a set query.
 */
public interface SetQuery extends QueryCommand {
    /**
     * Get the left-hand query.
     * 
     * @return the left-hand query; never null
     */
    public QueryCommand getLeft();

    /**
     * Get the right-hand query.
     * 
     * @return the right-hand query; never null
     */
    public QueryCommand getRight();

    /**
     * Get the set operation for this query.
     * 
     * @return the operation; never null
     */
    public String getOperation();

    /**
     * Return whether this set query is a 'UNION ALL' or 'INTERSECT ALL' or 'EXCEPT ALL' query.
     * 
     * @return true if this is an 'ALL' query, or false otherwise
     */
    public boolean isAll();
}
