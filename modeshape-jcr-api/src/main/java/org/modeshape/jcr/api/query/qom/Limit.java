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
 * Representation of a limit on the number of tuple results.
 */
public interface Limit {

    /**
     * Get the number of rows skipped before the results begin.
     * 
     * @return the offset; always 0 or a positive number
     */
    public int getOffset();

    /**
     * Get the maximum number of rows that are to be returned.
     * 
     * @return the maximum number of rows; always positive, or equal to {@link Integer#MAX_VALUE} if there is no limit
     */
    public int getRowLimit();

    /**
     * Determine whether this limit clause is necessary.
     * 
     * @return true if the number of rows is not limited and there is no offset, or false otherwise
     */
    public boolean isUnlimited();

    /**
     * Determine whether this limit clause defines an offset.
     * 
     * @return true if there is an offset, or false if there is no offset
     */
    public boolean isOffset();
}
