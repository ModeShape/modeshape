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
 * This package defines the {@link org.modeshape.jcr.query.plan.Planner} interface, the {@link org.modeshape.jcr.query.plan.CanonicalPlanner} implementation, and the
 * {@link org.modeshape.jcr.query.plan.PlanNode} class that is used to represent a canonical query plan.
 * <p>
 * The query plan is a tree of {@link org.modeshape.jcr.query.plan.PlanNode} objects that each represent a 
 * different aspect of the query, and is a form that is easily manipulated by subsequent stages.
 * Any implementation of {@link org.modeshape.jcr.query.plan.Planner} can be used, though a {@link org.modeshape.jcr.query.plan.CanonicalPlanner}
 * implementation is provided and will be sufficient for most cases.  In fact, the subsequent execution steps often
 * require the plan to be in its canonical form, so for most situations it may be best to simply reuse the CanonicalPlanner
 * and in other simply extend it.
 * </p>
 * <p>
 * Note that query plans are mutable and not thread-safe, meaning that such plans are not intended to be shared
 * or reused.
 * </p>

 */

package org.modeshape.jcr.query.plan;

