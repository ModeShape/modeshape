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
 * This package contains the Optimizer interface, a rule-based optimizer implementation, and library of optimization rules.
 * The Optimizer is responsible for evaluating, validating, and manipulating a canonical query plan to produce a more
 * a single optimized query processing plan.  The query plan is often changed in situ, although this is not required
 * of the {@link org.modeshape.jcr.query.optimize.Optimizer} implementations. A library of existing 
 * {@link org.modeshape.jcr.query.optimize.OptimizerRule} classes is provided, though it's very easy to 
 * add more optimizer rules.
 * </p>
 * <p>
 * The {@link org.modeshape.jcr.query.optimize.RuleBasedOptimizer} is an implementation that optimizes a query 
 * using a stack of rules.  A new stack is created for each rule, though the rules are required to be immutable and thus
 * often shared and reused.  And, the RuleBasedOptimizer is easily subclassed to define a custom stack of rules.
 * </p>
 */

package org.modeshape.jcr.query.optimize;

