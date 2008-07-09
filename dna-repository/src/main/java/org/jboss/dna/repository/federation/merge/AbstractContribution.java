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
package org.jboss.dna.repository.federation.merge;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;

/**
 * @author Randall Hauch
 */
public abstract class AbstractContribution implements Contribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    protected static final List<Segment> EMPTY_CHILDREN = Collections.emptyList();
    protected static final Map<Name, Property> EMPTY_PROPERTIES = Collections.emptyMap();

    private final String sourceName;
    private DateTime expirationTimeInUtc;

    /**
     * Create a contribution for the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     */
    protected AbstractContribution( String sourceName ) {
        assert sourceName != null && sourceName.trim().length() != 0;
        this.sourceName = sourceName;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * {@inheritDoc}
     */
    public List<Segment> getChildren() {
        return EMPTY_CHILDREN;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Name, Property> getProperties() {
        return EMPTY_PROPERTIES;
    }

    /**
     * {@inheritDoc}
     */
    public DateTime getExpirationTimeInUtc() {
        return this.expirationTimeInUtc;
    }

    /**
     * @param expirationTimeInUtc Sets expirationTimeInUtc to the specified value.
     */
    public void setExpirationTimeInUtc( DateTime expirationTimeInUtc ) {
        this.expirationTimeInUtc = expirationTimeInUtc;
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
        return this.sourceName.hashCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation only compares the {@link #getSourceName() source name}.
     * </p>
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof AbstractContribution) {
            AbstractContribution that = (AbstractContribution)obj;
            return this.getSourceName().equals(that.getSourceName());
        }
        return false;
    }

}
