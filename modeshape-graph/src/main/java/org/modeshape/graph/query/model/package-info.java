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
 * <p>The Abstract Query Model is a vocabulary that can be used to construct a language-neutral representation of a query.
 * It consists of a family of Java classes each represent the important semantic elements
 * needed to fully define a query.  A {@link QueryCommand} is the interface that represents a top-level query,
 * with the {@link Query} and {@link SetQuery} classes being the two implementations.
 * </p>
 * <p>
 * There are two ways to construct abstract query models.  The first is to programmatically construct a query
 * model using the {@link org.modeshape.graph.query.QueryBuilder}, which provides a fluent API that makes it easy to create a query
 * with Java code.  The second (and more common) approach is to use a {@link org.modeshape.graph.query.parse.QueryParser}
 * that parses a query represented in a specific language (like SQL or XPath) and then creates the query's equivalent abstract query model.
 * </p>
 * <p>
 * The abstract query model classes are immutable, making them very easily shared or reused if that is advantageous.
 * </p>
 */

package org.modeshape.graph.query.model;

