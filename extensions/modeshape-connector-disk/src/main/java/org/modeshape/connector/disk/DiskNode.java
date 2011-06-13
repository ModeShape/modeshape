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
package org.modeshape.connector.disk;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.util.Base64;
import org.modeshape.graph.connector.base.MapNode;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.property.basic.FileSystemBinary;

/**
 * A specialization of the {@link MapNode}.
 */
public class DiskNode extends MapNode {

    private static final long serialVersionUID = 1L;
    private transient Set<String> largeValueKeys;

    /**
     * Create a new node for storage on a disk.
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known and there is no parent
     * @param parent the UUID of the parent node; may be null if this is the root node and there is no name
     * @param properties the properties; may be null if there are no properties
     * @param children the list of child nodes; may be null
     */
    public DiskNode( UUID uuid,
                           Segment name,
                           UUID parent,
                           Map<Name, Property> properties,
                           List<UUID> children ) {
        super(uuid, name, parent, properties, children);
        calculateLargeValueKeys();
    }

    /**
     * Create a new node for storage on a disk..
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known and there is no parent
     * @param parent the UUID of the parent node; may be null if this is the root node and there is no name
     * @param properties the properties; may be null if there are no properties
     * @param children the list of child nodes; may be null
     */
    public DiskNode( UUID uuid,
                           Segment name,
                           UUID parent,
                           Iterable<Property> properties,
                           List<UUID> children ) {
        super(uuid, name, parent, properties, children);
        calculateLargeValueKeys();
    }

    /**
     * Create a new node for storage on a disk.
     * 
     * @param uuid the desired UUID; never null
     */
    public DiskNode( UUID uuid ) {
        super(uuid);
        calculateLargeValueKeys();
    }

    /**
     * Create a new node for storage on a disk.
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known and there is no parent
     * @param parent the UUID of the parent node; may be null if this is the root node and there is no name
     * @param properties the properties; may be null if there are no properties
     * @param children the list of child nodes; may be null
     * @param largeValueKeys the list of large
     */
    private DiskNode( UUID uuid,
                     Segment name,
                     UUID parent,
                     Map<Name, Property> properties,
                     List<UUID> children,
                      Set<String> largeValueKeys ) {
        super(uuid, name, parent, properties, children);
        this.largeValueKeys = largeValueKeys;
    }

    /**
     * Walks through all properties for the node and stores the keys of any large values in the {@code largeValueKeys} collection.
     * This must be called from each constructor <i>before</i> any properties can be modified.
     */
    private void calculateLargeValueKeys() {
        largeValueKeys = new HashSet<String>();
        
        try {
            for (Property property : getProperties().values()) {
                for (Object value : property) {
                    if (value instanceof FileSystemBinary) {
                        largeValueKeys.add(Base64.encodeBytes(((Binary)value).getHash(), Base64.URL_SAFE));
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        largeValueKeys = Collections.unmodifiableSet(largeValueKeys);
    }

    /**
     * Returns a list of all large value keys that were in use at the time this node was loaded. This may differ from the current
     * list of keys in use as properties are added, modified, and removed.
     * 
     * @return the list of all large value keys that were in use at the time this node was loaded; never null
     */
    Set<String> largeValueHashesInUse() {
        return largeValueKeys;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapNode#freeze()
     */
    @Override
    public DiskNode freeze() {
        if (!hasChanges()) return this;
        return new DiskNode(getUuid(), getName(), getParent(), changes.getUnmodifiableProperties(),
                            changes.getUnmodifiableChildren(), largeValueKeys);
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
    public DiskNode clone() {
        return new DiskNode(getUuid(), getName(), getParent(), getProperties(), getChildren());
    }
}
