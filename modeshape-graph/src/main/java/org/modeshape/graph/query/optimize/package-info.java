/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * This package contains the Optimizer interface, a rule-based optimizer implementation, and library of optimization rules.
 * The Optimizer is responsible for evaluating, validating, and manipulating a canonical query plan to produce a more
 * a single optimized query processing plan.  The query plan is often changed in situ, although this is not required
 * of the {@link org.modeshape.graph.query.optimize.Optimizer} implementations. A library of existing 
 * {@link org.modeshape.graph.query.optimize.OptimizerRule} classes is provided, though it's very easy to 
 * add more optimizer rules.
 * </p>
 * <p>
 * The {@link org.modeshape.graph.query.optimize.RuleBasedOptimizer} is an implementation that optimizes a query 
 * using a stack of rules.  A new stack is created for each rule, though the rules are required to be immutable and thus
 * often shared and reused.  And, the RuleBasedOptimizer is easily subclassed to define a custom stack of rules.
 * </p>
 */

package org.modeshape.graph.query.optimize;

