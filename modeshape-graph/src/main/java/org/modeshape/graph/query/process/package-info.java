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
 * This package defines the {@link QueryProcessor} interface, which is responsible for constructing for each query
 * a tree of {@link ProcessingComponent} objects that each are responsible for processing a specific aspect of
 * the query and returning the tuples to the parent component.
 * <p>
 * At the bottom are the "access" components that perform the low-level access of the tuples from the graph container.
 * Above these are the other components that implement various operations, such as limits, joins (using merge and 
 * nested loop algorithms), unions, intersects, distinct, sorts, and even column projections.  At the top is
 * a single component that produces tuples that represent the results of the query.
 * </p>
 * <p>
 * Once the {@link org.modeshape.graph.query.process.QueryProcessor} creates the ProcessingComponent assembly,
 * the top-level component is executed.  Execution involves requesting from the child processing component(s) the next batch of results,
 * processing each of the tuples according to the specific ProcessingComponent algorithm, and finally returning
 * the processed tuples.  
 * </p>
 */

package org.modeshape.graph.query.process;

