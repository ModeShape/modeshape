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
package org.modeshape.graph.connector;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.observe.Observer;

/**
 * The context for a repository. This interface need not be implemented by a {@link RepositorySource}, as it is normally provided
 * to the source when {@link RepositorySource#initialize(RepositoryContext) initialized}.
 */
@ThreadSafe
public interface RepositoryContext {

    /**
     * Get the execution context, which can be used to create other contexts with specific JAAS security contexts.
     * 
     * @return the execution context; never null
     */
    ExecutionContext getExecutionContext();

    /**
     * Get the factory for {@link RepositoryConnection connections} to other sources.
     * 
     * @return the connection factory
     */
    RepositoryConnectionFactory getRepositoryConnectionFactory();

    /**
     * Get the observer that the connector may use to publish changes.
     * 
     * @return the observer, or null if the are no listeners and publishing is not required/requested
     */
    Observer getObserver();

    /**
     * Get a snapshot of the current configuration for the {@link RepositorySource}. The root of the subgraph will be the node in
     * the configuration that represents the RepositorySource.
     * 
     * @param depth the max depth of the configuration subgraph
     * @return the configuration snapshot as a subgraph, or null if there is no configuration
     */
    Subgraph getConfiguration( int depth );
}
