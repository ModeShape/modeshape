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
package org.modeshape.jcr;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

public final class JcrNodeTypeDelegate implements NodeType {

    private final JcrNodeType delegate;
    private final JcrSession session;

    JcrNodeTypeDelegate( JcrNodeType delegate,
                         JcrSession session ) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean canAddChildNode( String childNodeName ) {
        return delegate.canAddChildNode(childNodeName);
    }

    @Override
    public boolean canAddChildNode( String childNodeName,
                                    String primaryNodeTypeName ) {
        return delegate.canAddChildNode(childNodeName, primaryNodeTypeName);
    }

    @Override
    public boolean canRemoveNode( String itemName ) {
        return delegate.canRemoveNode(itemName);
    }

    @Override
    public boolean canRemoveItem( String itemName ) {
        return delegate.canRemoveItem(itemName);
    }

    @Override
    public boolean canSetProperty( String propertyName,
                                   Value value ) {
        return delegate.canSetProperty(session, propertyName, value);
    }

    @Override
    public boolean canSetProperty( String propertyName,
                                   Value[] values ) {
        return delegate.canSetProperty(session, propertyName, values);
    }

    @Override
    public boolean canRemoveProperty( String propertyName ) {
        return delegate.canRemoveProperty(propertyName);
    }

    @Override
    public JcrNodeDefinition[] getDeclaredChildNodeDefinitions() {
        return delegate.getDeclaredChildNodeDefinitions();
    }

    @Override
    public JcrNodeDefinition[] getChildNodeDefinitions() {
        return delegate.getChildNodeDefinitions();
    }

    @Override
    public JcrPropertyDefinition[] getPropertyDefinitions() {
        return delegate.getPropertyDefinitions();
    }

    @Override
    public JcrNodeType[] getDeclaredSupertypes() {
        return delegate.getDeclaredSupertypes();
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        return delegate.getDeclaredSupertypeNames();
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        return delegate.getSubtypes();
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        return delegate.getDeclaredSubtypes();
    }

    @Override
    public String getPrimaryItemName() {
        return delegate.getPrimaryItemName();
    }

    @Override
    public JcrPropertyDefinition[] getDeclaredPropertyDefinitions() {
        return delegate.getDeclaredPropertyDefinitions();
    }

    @Override
    public NodeType[] getSupertypes() {
        return delegate.getSupertypes();
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return delegate.hasOrderableChildNodes();
    }

    @Override
    public boolean isMixin() {
        return delegate.isMixin();
    }

    @Override
    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    @Override
    public boolean isQueryable() {
        return delegate.isQueryable();
    }

    @Override
    public boolean isNodeType( String nodeTypeName ) {
        return delegate.isNodeType(nodeTypeName);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
