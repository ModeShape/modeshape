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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;

/**
 * The contribution of a source to the information for a single federated node. Users of this interface should treat contributions
 * as generally being immutable, since some implementation will be immutable and will return immutable {@link #getProperties()
 * properties} and {@link #getChildren() children} containers. Thus, rather than make changes to an existing contribution, a new
 * contribution is created to replace the previous contribution.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public interface Contribution extends Serializable {

    /**
     * Get the name of the source that made this contribution.
     * 
     * @return the name of the contributing source
     */
    public String getSourceName();

    /**
     * Get the source-specific path of this information.
     * 
     * @return the path as known to the source, or null for {@link EmptyContribution}
     */
    public Path getPathInSource();

    /**
     * Determine whether this contribution has expired given the supplied current time.
     * 
     * @param utcTime the current time expressed in UTC; may not be null
     * @return true if at least one contribution has expired, or false otherwise
     */
    public boolean isExpired( DateTime utcTime );

    /**
     * Get the expiration time, already in UTC.
     * 
     * @return the expiration time in UTC
     */
    public DateTime getExpirationTimeInUtc();

    /**
     * Get the properties that make up this contribution. This map is immutable.
     * 
     * @return the map of properties; never null
     */
    public Map<Name, Property> getProperties();

    /**
     * Get the children that make up this contribution. This list is immutable.
     * 
     * @return the list of children; never null
     */
    public List<Segment> getChildren();
}
