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
package org.jboss.dna.graph;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.properties.Path;

/**
 * A subgraph returned by the {@link Graph}, containing the nodes in the subgraph as well as the properties and children for each
 * of those nodes. The {@link #iterator()} method may be used to walk the nodes in the subgraph in a pre-order traversal.
 * <p>
 * Since this subgraph has a single {@link #getLocation() node that is the top of the subgraph}, the methods that take a String
 * path or {@link Path path object} will accept absolute or relative paths.
 * </p>
 * 
 * @author Randall Hauch
 */
@Immutable
public interface Subgraph extends Results {

    /**
     * Get the location of the node.
     * 
     * @return the node's location
     */
    Location getLocation();

    /**
     * Get the maximum depth requested for this subgraph. The actual subgraph may not be as deep, but will never be deeper than
     * this value.
     * 
     * @return the maximum depth requested; always positive
     */
    int getMaximumDepth();
}
