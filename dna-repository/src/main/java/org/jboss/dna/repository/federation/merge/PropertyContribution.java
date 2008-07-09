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

import java.util.HashMap;
import java.util.Map;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * The record of a source contributing only properties to a node.
 * <p>
 * This class does return a mutable mutable map of {@link #getProperties() properties}.
 * </p>
 * 
 * @author Randall Hauch
 */
public class PropertyContribution extends AbstractContribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private final Map<Name, Property> properties;

    /**
     * Create a contribution of node properties from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param properties the properties from the source; may not be null
     */
    public PropertyContribution( String sourceName,
                                 Iterable<Property> properties ) {
        super(sourceName);
        this.properties = new HashMap<Name, Property>();
        for (Property property : properties) {
            this.properties.put(property.getName(), property);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Name, Property> getProperties() {
        return this.properties;
    }

}
