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
package org.modeshape.graph.search;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.ChangeRequest;

/**
 * A component that acts as a search engine for the content within a single {@link RepositorySource}. This engine manages a set of
 * indexes and provides search functionality for each of the workspaces within the source, and provides various methods to
 * (re)index the content contained with source's workspaces and keep the indexes up-to-date via changes.
 */
@ThreadSafe
public interface SearchEngine {

    /**
     * Get the name of the source that can be searched with an engine that uses this provider.
     * 
     * @return the name of the source that is to be searchable; never null
     */
    String getSourceName();

    /**
     * Create the {@link SearchEngineProcessor} implementation that can be used to operate against the
     * {@link SearchEngineWorkspace} instances.
     * <p>
     * Note that the resulting processor must be {@link SearchEngineProcessor#close() closed} by the caller when completed.
     * </p>
     * 
     * @param context the context in which the processor is to be used; never null
     * @param observer the observer of any events created by the processor; may be null
     * @param readOnly true if the processor will only be reading or searching, or false if the processor will be used to update
     *        the workspaces
     * @return the processor; may not be null
     */
    SearchEngineProcessor createProcessor( ExecutionContext context,
                                           Observer observer,
                                           boolean readOnly );

    /**
     * Update the indexes with the supplied set of changes to the content.
     * 
     * @param context the execution context for which this session is to be established; may not be null
     * @param changes the set of changes to the content
     * @throws IllegalArgumentException if the path is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    void index( ExecutionContext context,
                final Iterable<ChangeRequest> changes ) throws SearchEngineException;
}
