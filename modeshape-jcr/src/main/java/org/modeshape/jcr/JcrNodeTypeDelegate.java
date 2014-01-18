/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
