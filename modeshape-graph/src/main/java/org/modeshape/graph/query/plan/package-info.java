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
 * This package defines the {@link Planner} interface, the {@link CanonicalPlanner} implementation, and the
 * {@link PlanNode} class that is used to represent a canonical query plan.
 * <p>
 * The query plan is a tree of {@link org.modeshape.graph.query.plan.PlanNode} objects that each represent a 
 * different aspect of the query, and is a form that is easily manipulated by subsequent stages.
 * Any implementation of {@link org.modeshape.graph.query.plan.Planner} can be used, though a {@link org.modeshape.graph.query.plan.CanonicalPlanner}
 * implementation is provided and will be sufficient for most cases.  In fact, the subsequent execution steps often
 * require the plan to be in its canonical form, so for most situations it may be best to simply reuse the CanonicalPlanner
 * and in other simply extend it.
 * </p>
 * <p>
 * Note that query plans are mutable and not thread-safe, meaning that such plans are not intended to be shared
 * or reused.
 * </p>

 */

package org.modeshape.graph.query.plan;

