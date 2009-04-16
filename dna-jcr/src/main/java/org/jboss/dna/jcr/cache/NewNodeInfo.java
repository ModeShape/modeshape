/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr.cache;

import java.util.Map;
import java.util.UUID;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.jcr.NodeDefinitionId;

/**
 * 
 */
public class NewNodeInfo extends ChangedNodeInfo {
    /**
     * Create an immutable NodeInfo instance.
     * 
     * @param originalLocation the original location
     * @param primaryTypeName the name of the node's primary type
     * @param definition the definition used when creating the node
     * @param parent the parent
     * @param properties the unmodifiable map of properties; may be null if there are no properties
     */
    public NewNodeInfo( Location originalLocation,
                        Name primaryTypeName,
                        NodeDefinitionId definition,
                        UUID parent,
                        Map<Name, PropertyInfo> properties ) {
        super(new ImmutableNodeInfo(originalLocation, primaryTypeName, null, definition, parent, null, properties));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns true.
     * </p>
     * 
     * @see org.jboss.dna.jcr.cache.ChangedNodeInfo#isNew()
     */
    @Override
    public boolean isNew() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns false, since a new node is not persisted yet.
     * </p>
     * 
     * @see org.jboss.dna.jcr.cache.ChangedNodeInfo#isModified()
     */
    @Override
    public boolean isModified() {
        return false;
    }

}
