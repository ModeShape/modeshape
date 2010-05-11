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
package org.modeshape.connector.jbosscache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.connector.base.MapNode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * A specialization of the {@link MapNode}.
 */
public class JBossCacheNode extends MapNode {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new node for storage inside Infinispan.
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known and there is no parent
     * @param parent the UUID of the parent node; may be null if this is the root node and there is no name
     * @param properties the properties; may be null if there are no properties
     * @param children the list of child nodes; may be null
     */
    public JBossCacheNode( UUID uuid,
                           Segment name,
                           UUID parent,
                           Map<Name, Property> properties,
                           List<UUID> children ) {
        super(uuid, name, parent, properties, children);
    }

    /**
     * Create a new node for storage inside Infinispan.
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known and there is no parent
     * @param parent the UUID of the parent node; may be null if this is the root node and there is no name
     * @param properties the properties; may be null if there are no properties
     * @param children the list of child nodes; may be null
     */
    public JBossCacheNode( UUID uuid,
                           Segment name,
                           UUID parent,
                           Iterable<Property> properties,
                           List<UUID> children ) {
        super(uuid, name, parent, properties, children);
    }

    /**
     * Create a new node for storage inside Infinispan.
     * 
     * @param uuid the desired UUID; never null
     */
    public JBossCacheNode( UUID uuid ) {
        super(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapNode#freeze()
     */
    @Override
    public JBossCacheNode freeze() {
        if (!hasChanges()) return this;
        return new JBossCacheNode(getUuid(), getName(), getParent(), changes.getUnmodifiableProperties(),
                                  changes.getUnmodifiableChildren());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method never clones the {@link #hasChanges() changes}.
     * </p>
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public JBossCacheNode clone() {
        return new JBossCacheNode(getUuid(), getName(), getParent(), getProperties(), getChildren());
    }
}
