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
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.DateTime;

/**
 * The record of a non-empty source contribution from a single location within the source.
 * 
 * @author Randall Hauch
 */
@Immutable
public abstract class NonEmptyContribution extends Contribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private final Location locationInSource;

    /**
     * Create a contribution of node properties from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the location in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     */
    protected NonEmptyContribution( String sourceName,
                                    Location locationInSource,
                                    DateTime expirationTime ) {
        super(sourceName, expirationTime);
        assert locationInSource != null;
        this.locationInSource = locationInSource;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getLocationInSource()
     */
    @Override
    public Location getLocationInSource() {
        return locationInSource;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the hash code of the {@link #getSourceName() source name}, and is compatible with the
     * implementation of {@link #equals(Object)}.
     * </p>
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.getSourceName(), this.getLocationInSource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NonEmptyContribution) {
            NonEmptyContribution that = (NonEmptyContribution)obj;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            if (!this.getLocationInSource().equals(that.getLocationInSource())) return false;
            return true;
        }
        return false;
    }
}
