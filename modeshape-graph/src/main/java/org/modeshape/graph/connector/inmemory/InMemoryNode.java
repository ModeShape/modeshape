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
package org.modeshape.graph.connector.inmemory;

import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.inmemory.InMemoryRepository.InMemoryNodeState;
import org.modeshape.graph.connector.map.DefaultMapNode;
import org.modeshape.graph.connector.map.MapNode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * An {@link MapNode} implementation used by the {@link InMemoryRepository}.
 */
public class InMemoryNode extends DefaultMapNode {

    private final ChangeListener listener;

    /**
     * @param listener the listener that is to be notified of any changes
     * @param uuid the UUID of the node
     */
    public InMemoryNode( ChangeListener listener,
                         UUID uuid ) {
        super(uuid);
        this.listener = listener;
        assert this.listener != null;
    }

    /**
     * The interface that {@link InMemoryNode} objects use to signal when they are about to change.
     */
    public static interface ChangeListener {
        /**
         * Signal that the supplied node is about to change.
         * 
         * @param node the node that is about to change
         */
        void prepareForChange( InMemoryNode node );
    }

    /**
     * Restore the state of this node from the supplied snapshot.
     * 
     * @param state the snapshot of the state; may not be null
     */
    protected void restoreFrom( InMemoryNodeState state ) {
        super.setParent(state.getParent());
        super.setName(state.getName());
        // Restore the properties ...
        super.setProperties(state.getProperties());
        // Restore the children ...
        super.clearChildren();
        for (MapNode originalChild : state.getChildren()) {
            originalChild.setParent(this);
            super.addChild(originalChild);
        }
        Set<Name> uniqueChildNames = super.getUniqueChildNames();
        uniqueChildNames.clear();
        uniqueChildNames.addAll(state.getUniqueChildNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#addChild(int, org.modeshape.graph.connector.map.MapNode)
     */
    @Override
    public void addChild( int index,
                          MapNode child ) {
        listener.prepareForChange(this);
        super.addChild(index, child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#addChild(org.modeshape.graph.connector.map.MapNode)
     */
    @Override
    public void addChild( MapNode child ) {
        listener.prepareForChange(this);
        super.addChild(child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#clearChildren()
     */
    @Override
    public void clearChildren() {
        listener.prepareForChange(this);
        super.clearChildren();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#removeChild(org.modeshape.graph.connector.map.MapNode)
     */
    @Override
    public boolean removeChild( MapNode child ) {
        listener.prepareForChange(this);
        return super.removeChild(child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#removeProperty(org.modeshape.graph.property.Name)
     */
    @Override
    public MapNode removeProperty( Name propertyName ) {
        listener.prepareForChange(this);
        return super.removeProperty(propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#setName(org.modeshape.graph.property.Path.Segment)
     */
    @Override
    public void setName( Segment name ) {
        // This method only sets this name and does not change the name of any other node ...
        listener.prepareForChange(this);
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#setParent(org.modeshape.graph.connector.map.MapNode)
     */
    @Override
    public void setParent( MapNode parent ) {
        listener.prepareForChange(this);
        super.setParent(parent);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#setProperties(java.lang.Iterable)
     */
    @Override
    public MapNode setProperties( Iterable<Property> properties ) {
        listener.prepareForChange(this);
        return super.setProperties(properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#setProperty(org.modeshape.graph.ExecutionContext, java.lang.String,
     *      java.lang.Object[])
     */
    @Override
    public MapNode setProperty( ExecutionContext context,
                                String name,
                                Object... values ) {
        listener.prepareForChange(this);
        return super.setProperty(context, name, values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.DefaultMapNode#setProperty(org.modeshape.graph.property.Property)
     */
    @Override
    public MapNode setProperty( Property property ) {
        listener.prepareForChange(this);
        return super.setProperty(property);
    }

}
