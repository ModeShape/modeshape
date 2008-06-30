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

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;

/**
 * A binding of content from a source into a particular location within a repository. The content in the source is defined by the
 * name of the source and the {@link #getPathInSource() path within the source}. The {@link #getPathInRepository() path in the
 * repository} defines the location at which the content appears.
 * <p>
 * Note that it is possible for a federated repository to have a single source may be bound to multiple locations, and multiple
 * sources bound to the same location.
 * </p>
 * 
 * @author Randall Hauch
 */
@Immutable
public class FederatedRegion implements Comparable<FederatedRegion> {

    private final Path pathInRepository;
    private final Path pathInSource;
    private final String sourceName;

    /**
     * Create a binding given a location in the repository below which the content from the named source appears.
     * 
     * @param pathInRepository the absolute path in the repository at which the content appears
     * @param pathInSource the path in the source defining the content
     * @param sourceName the name of the source
     * @throws IllegalArgumentException if any of the parameters is null, or if the source name is empty/blank, or if the
     *         <code>pathInRepository</code> is not {@link Path#isAbsolute() absolute}.
     */
    public FederatedRegion( Path pathInRepository,
                             Path pathInSource,
                             String sourceName ) {
        ArgCheck.isNotNull(pathInRepository, "pathInRepository");
        ArgCheck.isNotNull(pathInSource, "pathInSource");
        ArgCheck.isNotEmpty(sourceName, "sourceName");
        if (!pathInRepository.isAbsolute()) {
            String msg = RepositoryI18n.repositoryPathInFederationBindingIsNotAbsolute.text(pathInRepository.getString());
            throw new IllegalArgumentException(msg);
        }
        this.pathInRepository = pathInRepository;
        this.pathInSource = pathInSource;
        this.sourceName = sourceName;
    }

    /**
     * Get the location in the repository at which the source's content appears.
     * 
     * @return the repository-based path for the top of the source's content
     */
    public Path getPathInRepository() {
        return pathInRepository;
    }

    /**
     * Get the path to the source content that is bound to the {@link #getPathInRepository() repository location}. This path is
     * source-specific, and may be either absolute or relative.
     * 
     * @return pathInSource
     */
    public Path getPathInSource() {
        return pathInSource;
    }

    /**
     * @return sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Convert a path defined in the source system into an equivalent path in the repository system.
     * 
     * @param pathInSource the path in the source system, which may include the {@link #getPathInSource()}
     * @param factory the path factory; may not be null
     * @return the path in the repository system, which will be normalized and absolute (including the
     *         {@link #getPathInRepository()})
     */
    public Path convertPathInSourceToPathInRepository( Path pathInSource,
                                                       PathFactory factory ) {
        Path relativeSourcePath = pathInSource;
        if (this.pathInSource.isAncestorOf(pathInSource)) {
            // Remove the leading source path ...
            relativeSourcePath = pathInSource.relativeTo(this.pathInSource);
        }
        // Prepend the region's root path ...
        Path result = factory.create(this.pathInRepository, relativeSourcePath);
        return result.getNormalizedPath();
    }

    /**
     * Convert a path defined in the repository system into an equivalent path in the source system.
     * 
     * @param pathInRepository the path in the repository system, which may include the {@link #getPathInRepository()}
     * @param factory the path factory; may not be null
     * @return the path in the source system, which will be normalized and absolute (including the {@link #getPathInSource()})
     */
    public Path convertPathInRepositoryToPathInSource( Path pathInRepository,
                                                       PathFactory factory ) {
        Path pathInRegion = pathInRepository;
        if (this.pathInRepository.isAncestorOf(pathInRepository)) {
            // Find the relative path from the root of this region ...
            pathInRegion = pathInRepository.relativeTo(this.pathInRepository);
        }
        // Prepend the path in source ...
        Path result = factory.create(this.pathInSource, pathInRegion);
        return result.getNormalizedPath();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return pathInRepository.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedRegion) {
            FederatedRegion that = (FederatedRegion)obj;
            if (!this.getPathInRepository().equals(that.getPathInRepository())) return false;
            if (!this.getPathInSource().equals(that.getPathInSource())) return false;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( FederatedRegion that ) {
        if (this == that) return 0;
        int diff = this.getPathInRepository().compareTo(that.getPathInRepository());
        if (diff != 0) return diff;
        diff = this.getPathInSource().compareTo(that.getPathInSource());
        if (diff != 0) return diff;
        return this.getSourceName().compareTo(that.getSourceName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getPathInRepository().toString() + " -> \"" + this.getSourceName() + "\":" + this.getPathInSource();
    }
}
