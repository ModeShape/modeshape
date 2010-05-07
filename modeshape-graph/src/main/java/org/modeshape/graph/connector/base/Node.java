/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.connector.base;

import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * A snapshot of a single node within a map-based repository.
 */
@Immutable
public interface Node {

    /**
     * Returns the UUID for this node
     * 
     * @return the UUID for this node
     */
    UUID getUuid();

    /**
     * Returns the name of this node along with its SNS index within its parent's children
     * 
     * @return the name of this node along with its SNS index within its parent's children, or null for the root node
     */
    Path.Segment getName();

    /**
     * Returns properties of this node.
     * 
     * @return an immutable map of properties keyed by their name.
     */
    Map<Name, Property> getProperties();

    /**
     * Returns the property with the supplied name.
     * 
     * @param name the name of the property
     * @return the property, or null if this node has no such property
     */
    Property getProperty( Name name );

}
