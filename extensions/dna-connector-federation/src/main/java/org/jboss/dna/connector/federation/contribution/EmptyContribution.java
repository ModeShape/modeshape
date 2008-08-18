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
package org.jboss.dna.connector.federation.contribution;

import net.jcip.annotations.Immutable;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Path;

/**
 * A source contribution that is empty. In other words, the source has no contribution to make.
 * <p>
 * Note that this is different than an unknown contribution, which may occur when a source is added to a federated repository
 * after the contributions have already been determined for nodes. In this case, the new source's contribution for a node is not
 * known and must be determined.
 * </p>
 * 
 * @author Randall Hauch
 */
@Immutable
public class EmptyContribution extends Contribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a contribution for the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     */
    public EmptyContribution( String sourceName,
                              DateTime expirationTime ) {
        super(sourceName, expirationTime);
        if (ContributionStatistics.RECORD) ContributionStatistics.record(0, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getPathInSource()
     */
    @Override
    public Path getPathInSource() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof EmptyContribution) {
            EmptyContribution that = (EmptyContribution)obj;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            return true;
        }
        return false;
    }
}
