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
/**
 * This package defines the {@link org.modeshape.jcr.query.process.QueryProcessor} interface, which is responsible for constructing for each query
 * a tree of {@link org.modeshape.jcr.query.process.ProcessingComponent} objects that each are responsible for processing a specific aspect of
 * the query and returning the tuples to the parent component.
 * <p>
 * At the bottom are the "access" components that perform the low-level access of the tuples from the graph container.
 * Above these are the other components that implement various operations, such as limits, joins (using merge and 
 * nested loop algorithms), unions, intersects, distinct, sorts, and even column projections.  At the top is
 * a single component that produces tuples that represent the results of the query.
 * </p>
 * <p>
 * Once the {@link org.modeshape.jcr.query.process.QueryProcessor} creates the ProcessingComponent assembly,
 * the top-level component is executed.  Execution involves requesting from the child processing component(s) the next batch of results,
 * processing each of the tuples according to the specific ProcessingComponent algorithm, and finally returning
 * the processed tuples.  
 * </p>
 */

package org.modeshape.jcr.query.process;

