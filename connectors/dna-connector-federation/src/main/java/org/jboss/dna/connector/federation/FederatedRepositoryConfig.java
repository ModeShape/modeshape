/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.ThreadSafeProblems;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * The configuration of a federated repository. The configuration defines, among other things, the set of
 * {@link #getSourceProjections() source projections} in the federated repository that each specify how and where content from a
 * {@link RepositorySource source} is federated into the unified repository.
 * 
 * @author Randall Hauch
 */
@Immutable
public class FederatedRepositoryConfig implements Comparable<FederatedRepositoryConfig> {

    private final Projection cacheProjection;
    private final List<Projection> sourceProjections;
    private final Problems problems;
    private final String name;
    private final CachePolicy defaultCachePolicy;

    /**
     * Create a federated repository instance.
     * 
     * @param repositoryName the name of the repository
     * @param cacheProjection the projection used for the cache; may not be null
     * @param sourceProjections the source projections; may not be null
     * @param defaultCachePolicy the default cache policy for this repository; may be null
     * @throws IllegalArgumentException if the name is null or is blank
     */
    public FederatedRepositoryConfig( String repositoryName,
                                      Projection cacheProjection,
                                      Iterable<Projection> sourceProjections,
                                      CachePolicy defaultCachePolicy ) {
        ArgCheck.isNotEmpty(repositoryName, "repositoryName");
        ArgCheck.isNotNull(cacheProjection, "cacheProjection");
        this.name = repositoryName;
        this.problems = new ThreadSafeProblems();
        this.defaultCachePolicy = defaultCachePolicy;
        this.cacheProjection = cacheProjection;
        List<Projection> projectionList = new ArrayList<Projection>();
        for (Projection projection : sourceProjections) {
            if (projection == null) continue;
            if (!projectionList.contains(projection)) {
                projectionList.add(projection);
            }
        }
        this.sourceProjections = Collections.unmodifiableList(projectionList);
        ArgCheck.isNotEmpty(this.sourceProjections, "sourceProjections");
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
     * Get the default cache policy for the repository with the supplied name
     * 
     * @return the default cache policy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
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
        if (obj instanceof FederatedRepositoryConfig) {
            FederatedRepositoryConfig that = (FederatedRepositoryConfig)obj;
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
    public int compareTo( FederatedRepositoryConfig that ) {
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
