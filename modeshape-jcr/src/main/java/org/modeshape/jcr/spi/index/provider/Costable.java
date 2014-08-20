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

import javax.jcr.query.qom.Constraint;

/**
 * A simple interface that provides a method to compute the cost of applying indexes.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface Costable {
    /**
     * Compute the cost applying the given constraint.
     *
     * @param constraint the constraint; never null
     * @return the approximate number of records that will be returned
     */
    long estimateCardinality( Constraint constraint );

}
