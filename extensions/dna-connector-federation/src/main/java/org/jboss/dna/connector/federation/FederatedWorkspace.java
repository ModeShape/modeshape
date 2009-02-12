/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.federation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.ThreadSafeProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.connector.federation.merge.strategy.MergeStrategy;
import org.jboss.dna.connector.federation.merge.strategy.OneContributionMergeStrategy;
import org.jboss.dna.connector.federation.merge.strategy.SimpleMergeStrategy;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositorySource;

/**
 * The configuration of a federated repository. workspace The configuration defines, among other things, the set of
 * {@link #getSourceProjections() source projections} in the federated workspace that each specify how and where content from a
 * {@link RepositorySource source} is federated into the unified workspace.
 * 
 * @author Randall Hauch
 */
@Immutable
public class FederatedWorkspace implements Comparable<FederatedWorkspace> {

    private final Projection cacheProjection;
    private final CachePolicy cachePolicy;
    private final List<Projection> sourceProjections;
    private final Map<String, List<Projection>> projectionsBySourceName;
    private final Problems problems;
    private final String name;
    private final MergeStrategy mergingStrategy;

    /**
     * Create a configuration for a federated workspace.
     * 
     * @param workspaceName the name of the federated workspace; may not be null
     * @param cacheProjection the projection used for the cache; may not be null
     * @param sourceProjections the source projections; may not be null
     * @param cachePolicy the cache policy for this workspace; may be null if there is no policy
     * @throws IllegalArgumentException if the name is null or is blank
     */
    public FederatedWorkspace( String workspaceName,
                               Projection cacheProjection,
                               Iterable<Projection> sourceProjections,
                               CachePolicy cachePolicy ) {
        this(workspaceName, cacheProjection, sourceProjections, cachePolicy, null);
    }

    /**
     * Create a configuration for a federated workspace.
     * 
     * @param workspaceName the name of the federated workspace; may not be null
     * @param cacheProjection the projection used for the cache; may not be null
     * @param sourceProjections the source projections; may not be null
     * @param cachePolicy the cache policy for this workspace; may be null if there is no policy
     * @param mergeStrategy the strategy that should be used to merge nodes, or null if the strategy should be chosen based
     *        automatically based upon the number of sources used by the projections
     * @throws IllegalArgumentException if the name is null or is blank
     */
    public FederatedWorkspace( String workspaceName,
                               Projection cacheProjection,
                               Iterable<Projection> sourceProjections,
                               CachePolicy cachePolicy,
                               MergeStrategy mergeStrategy ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(cacheProjection, "cacheProjection");
        this.name = workspaceName;
        this.cachePolicy = cachePolicy;
        this.problems = new ThreadSafeProblems();
        this.cacheProjection = cacheProjection;
        List<Projection> projectionList = new ArrayList<Projection>();
        for (Projection projection : sourceProjections) {
            if (projection == null) continue;
            if (!projectionList.contains(projection)) {
                projectionList.add(projection);
            }
        }
        this.sourceProjections = Collections.unmodifiableList(projectionList);
        CheckArg.isNotEmpty(this.sourceProjections, "sourceProjections");
        this.projectionsBySourceName = new HashMap<String, List<Projection>>();
        for (Projection projection : this.sourceProjections) {
            String sourceName = projection.getSourceName();
            List<Projection> projectionsForSource = projectionsBySourceName.get(sourceName);
            if (projectionsForSource == null) {
                projectionsForSource = new LinkedList<Projection>();
                projectionsBySourceName.put(sourceName, projectionsForSource);
            }
            projectionsForSource.add(projection);
        }
        if (mergeStrategy != null) {
            this.mergingStrategy = mergeStrategy;
        } else {
            if (this.sourceProjections.size() == 1 && this.sourceProjections.get(0).isSimple()) {
                this.mergingStrategy = new OneContributionMergeStrategy();
            } else {
                this.mergingStrategy = new SimpleMergeStrategy();
            }
        }
        assert this.mergingStrategy != null;
    }

    /**
     * Get the name of this repository
     * 
     * @return name
     */
    public String getName() {
        return this.name;
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
     * Get the merging strategy used for this workspace
     * 
     * @return the workspace's merging strategy; never null
     */
    public MergeStrategy getMergingStrategy() {
        return mergingStrategy;
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
     * Get the projection that defines the cache for this repository. This projection does not exist in the
     * {@link #getSourceProjections() list of source projections}.
     * 
     * @return the region used for caching; never null
     */
    public Projection getCacheProjection() {
        return cacheProjection;
    }

    /**
     * Return the unmodifiable list of source projections.
     * 
     * @return the source projections; never null and never empty
     */
    public List<Projection> getSourceProjections() {
        return sourceProjections;
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
            for (Projection projection : sourceProjections) {
                if (projection.getWorkspaceName().equals(workspaceName)) return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
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
            if (!this.getName().equals(that.getName())) return false;
            if (!this.getCacheProjection().equals(that.getCacheProjection())) return false;
            if (!this.getSourceProjections().equals(that.getSourceProjections())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( FederatedWorkspace that ) {
        if (that == this) return 0;
        int diff = this.getName().compareTo(that.getName());
        if (diff != 0) return diff;
        diff = this.getCacheProjection().compareTo(that.getCacheProjection());
        if (diff != 0) return diff;
        Iterator<Projection> thisIter = this.getSourceProjections().iterator();
        Iterator<Projection> thatIter = that.getSourceProjections().iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            diff = thisIter.next().compareTo(thatIter.next());
            if (diff != 0) return diff;
        }
        if (thisIter.hasNext()) return 1;
        if (thatIter.hasNext()) return -1;
        return 0;
    }
}
