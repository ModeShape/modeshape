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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.ThreadSafeProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryContext;

/**
 * The configuration of a single workspace within a {@link FederatedRepositorySource federated repository}.
 */
@Immutable
class FederatedWorkspace {

    private final RepositoryContext repositoryContext;
    private final String sourceName;
    private final String workspaceName;
    private final List<Projection> projections;
    private final Map<String, List<Projection>> projectionsBySourceName;
    private final CachePolicy cachePolicy;
    private final Problems problems;
    private final Projector projector;

    /**
     * Create a configuration for a federated workspace.
     * 
     * @param repositoryContext the repository context; may not be null
     * @param sourceName the name of the federated repository; may not be null
     * @param workspaceName the name of the federated workspace; may not be null
     * @param projections the source projections; may not be null
     * @param cachePolicy the cache policy for this workspace; may be null if there is no policy
     * @throws IllegalArgumentException if the name is null or is blank
     */
    public FederatedWorkspace( RepositoryContext repositoryContext,
                               String sourceName,
                               String workspaceName,
                               Iterable<Projection> projections,
                               CachePolicy cachePolicy ) {
        CheckArg.isNotNull(repositoryContext, "repositoryContext");
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(projections, "projections");
        this.repositoryContext = repositoryContext;
        this.workspaceName = workspaceName;
        this.sourceName = sourceName;
        this.cachePolicy = cachePolicy;
        this.problems = new ThreadSafeProblems();
        List<Projection> projectionList = new ArrayList<Projection>();
        for (Projection projection : projections) {
            if (projection == null) continue;
            if (!projectionList.contains(projection)) {
                projectionList.add(projection);
            }
        }
        this.projections = Collections.unmodifiableList(projectionList);
        CheckArg.isNotEmpty(this.projections, "sourceProjections");
        this.projectionsBySourceName = new HashMap<String, List<Projection>>();
        for (Projection projection : this.projections) {
            String nameOfSource = projection.getSourceName();
            List<Projection> projectionsForSource = projectionsBySourceName.get(nameOfSource);
            if (projectionsForSource == null) {
                projectionsForSource = new LinkedList<Projection>();
                projectionsBySourceName.put(nameOfSource, projectionsForSource);
            }
            projectionsForSource.add(projection);
        }

        // Create the (most) appropriate projector ...
        ExecutionContext context = this.repositoryContext.getExecutionContext();
        Projector projector = MirrorProjector.with(context, projectionList);
        if (projector == null) projector = BranchedMirrorProjector.with(context, projectionList);
        if (projector == null) projector = OffsetMirrorProjector.with(context, projectionList);
        if (projector == null) projector = GeneralProjector.with(context, projectionList);
        assert projector != null;
        this.projector = projector;
    }

    /**
     * Get the repository context in which this workspace exists and has been initialized.
     * 
     * @return the repository context
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * Get the name of the federated repository.
     * 
     * @return sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the name of this repository
     * 
     * @return name
     */
    public String getName() {
        return this.workspaceName;
    }

    /**
     * Get the cache policy for this workspace
     * 
     * @return the workspace's cache policy; may be null
     */
    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    /**
     * Return the problem associated with this configuration. These problems may change at any time, although the returned
     * {@link Problems} object is thread-safe.
     * 
     * @return the thread-safe problems for this configuration
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * Return the unmodifiable list of source projections.
     * 
     * @return the source projections; never null and never empty
     */
    public List<Projection> getProjections() {
        return projections;
    }

    /**
     * Determine whether this workspace has a projection supplied contribution
     * 
     * @param sourceName the name of the source
     * @param workspaceName the name of the workspace
     * @return true if this workspace contains a projection that uses the supplied source and workspace
     */
    public boolean contains( String sourceName,
                             String workspaceName ) {
        List<Projection> projections = this.projectionsBySourceName.get(sourceName);
        if (projections != null) {
            for (Projection projection : projections) {
                if (projection.getWorkspaceName().equals(workspaceName)) return true;
            }
        }
        return false;
    }

    /**
     * Get the map of projections by their source name. This method provides direct access to the map used by this instance, and
     * is mutable. This is meant to be called only by subclasses and tests.
     * 
     * @return the list of projections for each source
     */
    Map<String, List<Projection>> getProjectionsBySourceName() {
        return projectionsBySourceName;
    }

    /**
     * Project the supplied location in the federated repository into the equivalent projected node(s).
     * 
     * @param context the execution context in which the content is being accessed; may not be null
     * @param location the location in the federated repository; may not be null
     * @param requiresUpdate true if the operation for which this projection is needed will update the content in some way, or
     *        false if read-only operations will be performed
     * @return the projected node, or null if the node does not exist in any projection
     */
    public ProjectedNode project( ExecutionContext context,
                                  Location location,
                                  boolean requiresUpdate ) {
        return projector.project(context, location, requiresUpdate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.workspaceName.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedWorkspace) {
            FederatedWorkspace that = (FederatedWorkspace)obj;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            if (!this.getName().equals(that.getName())) return false;
            if (!this.getProjections().equals(that.getProjections())) return false;
            return true;
        }
        return false;
    }

}
