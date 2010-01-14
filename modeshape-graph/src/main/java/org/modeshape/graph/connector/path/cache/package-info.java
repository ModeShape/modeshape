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
 * 
 * The {@link PathRepositoryCache} class and its supporting classes provide a standard caching mechanism for path-based repositories.  Users of any path repository
 * based on {@link org.modeshape.graph.connector.path.AbstractPathRepositorySource} can call {@link org.modeshape.graph.connector.path.AbstractPathRepositorySource#setCachePolicy(PathCachePolicy)} to
 * configure the caching for that repository.  This method can be invoked at run-time to modify the cache properties of an in-use repository.
 * <p>
 * Each {@code AbstractPathRepositorySource} has a local instance of {@link PathRepositoryCache} that reads the {@code PathCachePolicy} for the source and uses it to create instances of
 * {@link WorkspaceCache} for each workspace in the repository.  When a workspace cache instance is requested for the first time from the {@link PathRepositoryCache#getCache(String)} method,
 * an instance of the class is created using the no-argument constructor for the class and {@link WorkspaceCache#initialize(PathCachePolicy, String)} is called with the current cache policy. 
 * </p>
 * <p>
 * When {@link org.modeshape.graph.connector.path.AbstractPathRepositorySource#setCachePolicy(PathCachePolicy) the cache policy is changed} on the source, the existing {@code PathRepositoryCache}
 * is {@link PathRepositoryCache#close() closed} and a new path repository cache is created.
 * </p> 
 * 
 */

package org.modeshape.graph.connector.path.cache;

