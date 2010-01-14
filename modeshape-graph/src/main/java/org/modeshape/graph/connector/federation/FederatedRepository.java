/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.graph.connector.federation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.request.InvalidWorkspaceException;

/**
 * The configuration of a {@link FederatedRepositorySource}.
 */
@Immutable
class FederatedRepository {

    private final String sourceName;
    private final CachePolicy defaultCachePolicy;
    private final RepositoryConnectionFactory connectionFactory;
    private final Map<String, FederatedWorkspace> workspacesByName;
    private final FederatedWorkspace defaultWorkspace;
    private final ExecutorService executor;

    /**
     * Construct a new instance of a configuration defining how the workspaces in a federated repository should project the
     * content from one or more sources.
     * 
     * @param sourceName the name of the federated repository source; may not be null
     * @param connectionFactory the factory for connections to the sources being federated; may not be null
     * @param workspaces the workspaces that make up this federated repository; may not be null or empty
     * @param defaultCachePolicy the default cache policy for the source, or null if there is no default caching policy for the
     *        whole federated repository (each workspace may have its own)
     * @param executor the {@link ExecutorService} that can be used to parallelize actions within this repository;may not be null
     */
    public FederatedRepository( String sourceName,
                                RepositoryConnectionFactory connectionFactory,
                                Iterable<FederatedWorkspace> workspaces,
                                CachePolicy defaultCachePolicy,
                                ExecutorService executor ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(workspaces, "workspaces");
        CheckArg.isNotNull(executor, "executor");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.executor = executor;
        this.defaultCachePolicy = defaultCachePolicy;
        this.workspacesByName = new HashMap<String, FederatedWorkspace>();
        FederatedWorkspace defaultWorkspace = null;
        for (FederatedWorkspace workspace : workspaces) {
            if (defaultWorkspace == null) defaultWorkspace = workspace;
            this.workspacesByName.put(workspace.getName(), workspace);
        }
        this.defaultWorkspace = defaultWorkspace;
        assert this.defaultWorkspace != null;
        assert this.workspacesByName.size() > 0;
    }

    /**
     * Get the name of the {@link RepositorySource} that owns this configuration.
     * 
     * @return the source's name; never null
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the {@link RepositoryConnectionFactory factory for connections} to the sources that are being federated by this
     * repository.
     * 
     * @return the connection factory for the sources; never null
     */
    public RepositoryConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Get the {@link FederatedWorkspace workspace} information, given its name.
     * 
     * @param name the name of the workspace, or null if the default workspace should be returned
     * @return the workspace
     * @throws InvalidWorkspaceException if the specified workspace does not exist
     */
    public FederatedWorkspace getWorkspace( String name ) {
        if (name == null) {
            assert defaultWorkspace != null;
            return defaultWorkspace;
        }
        FederatedWorkspace workspace = workspacesByName.get(name);
        if (workspace == null) {
            String msg = GraphI18n.workspaceDoesNotExistInFederatedRepository.text(name, getSourceName());
            throw new InvalidWorkspaceException(msg);
        }
        return workspace;
    }

    /**
     * Get the names of the available workspaces.
     * 
     * @return the unmodifiable copy of the workspace names; never null
     */
    Set<String> getWorkspaceNames() {
        return Collections.unmodifiableSet(new HashSet<String>(workspacesByName.keySet()));
    }

    /**
     * Get the default cache policy that used for the whole repository. Note that the repository may or may not have a default
     * caching policy, and each {@link FederatedWorkspace workspace} may have its own {@link FederatedWorkspace#getCachePolicy()
     * caching policy}.
     * 
     * @return the default cache policy for the repository, used if/when the workspace(s) don't have their own caching policy or
     *         when the source content does not specify the caching policy; may be null
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * Get the executor for this repository. This executor can be used to process tasks.
     * 
     * @return the executor; may not be null
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Federated repository \"" + getSourceName() + "\" with workspaces " + workspacesByName.keySet();
    }
}
