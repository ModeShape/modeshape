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

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;

/**
 * The contribution of a source to the information for a single federated node.
 * <p>
 * This class does return a mutable list of {@link #getChildren() children} and mutable map of {@link #getProperties() properties}.
 * </p>
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class NodeContribution extends PropertyContribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private List<Segment> children;

    /**
     * Create a contribution of node properties and children from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param properties the properties from the source; may not be null
     * @param children the children from the source; may not be null or empty
     */
    public NodeContribution( String sourceName,
                             Iterable<Property> properties,
                             Iterable<Segment> children ) {
        super(sourceName, properties);
        this.children = new ArrayList<Segment>(1);
        for (Segment child : children) {
            this.children.add(child);
        }
    }

    /**
     * Get the children that make up this contribution. This list is immutable.
     * 
     * @return the list of children; never null
     */
    @Override
    public List<Segment> getChildren() {
        return this.children;
    }
}
