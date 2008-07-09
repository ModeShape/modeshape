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
package org.jboss.dna.repository.federation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.ThreadSafeProblems;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * The configuration of a {@link FederatedRepository}. The configuration defines, among other things, the set of
 * {@link #getRegions() regions} in the federated repository that each specify how and where content from a
 * {@link RepositorySource source} is federated into the unified repository.
 * 
 * @author Randall Hauch
 */
@Immutable
public class FederatedRepositoryConfig {

    private final FederatedRegion cacheRegion;
    private final List<FederatedRegion> regions;
    private final Problems problems;
    private final String name;
    private final CachePolicy defaultCachePolicy;

    /**
     * Create a federated repository instance, as managed by the supplied {@link FederationService}.
     * 
     * @param repositoryName the name of the repository
     * @param cacheRegion the region used for the cache; may not be null
     * @param regions the federated regions; may not be null
     * @param defaultCachePolicy the default cache policy for this repository; may be null
     * @throws IllegalArgumentException if the name is null or is blank
     */
    public FederatedRepositoryConfig( String repositoryName,
                                      FederatedRegion cacheRegion,
                                      Iterable<FederatedRegion> regions,
                                      CachePolicy defaultCachePolicy ) {
        ArgCheck.isNotEmpty(repositoryName, "repositoryName");
        ArgCheck.isNotNull(cacheRegion, "cacheRegion");
        this.name = repositoryName;
        this.problems = new ThreadSafeProblems();
        this.defaultCachePolicy = defaultCachePolicy;
        this.cacheRegion = cacheRegion;
        List<FederatedRegion> regionList = new ArrayList<FederatedRegion>();
        for (FederatedRegion region : regions) {
            if (region == null) continue;
            if (!regionList.contains(region)) {
                regionList.add(region);
            }
        }
        this.regions = Collections.unmodifiableList(regionList);
        ArgCheck.isNotEmpty(this.regions, "regions");
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
     * Get the region that defines the cache for this repository. This region does not exist in the {@link #getRegions() list of
     * source regions}.
     * 
     * @return the region used for caching; never null
     */
    public FederatedRegion getCacheRegion() {
        return cacheRegion;
    }

    /**
     * Return the unmodifiable list of bindings.
     * 
     * @return the bindings
     */
    public List<FederatedRegion> getRegions() {
        return regions;
    }

    /**
     * Get the default cache policy for the repository with the supplied name
     * 
     * @return the default cache policy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }
}
