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
package org.modeshape.sequencer.ddl.node;

import static org.modeshape.jcr.api.JcrConstants.JCR_MIXIN_TYPES;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.util.CheckArg;

/**
 * Utility class which provides construction, editing and assorted methods to work with AstNodes.
 */
public final class AstNodeFactory {

    /**
     * Constructs an {@link AstNode} with the given string name
     * 
     * @param name the name property of the node; may not be null
     * @return the tree node
     */
    public AstNode node( String name ) {
        CheckArg.isNotNull(name, "name");
        AstNode node = new AstNode(name);
        node.setProperty(JCR_PRIMARY_TYPE, NT_UNSTRUCTURED);
        return node;
    }

    /**
     * Constructs an {@link AstNode} with the given name, types and parent node.
     * 
     * @param name the name property of the node; may not be null
     * @param parent the parent of the node; may not be null
     * @param types the mixin types; may not be null, but may be empty
     * @return the tree node
     */
    public AstNode node( String name,
                         AstNode parent,
                         Object... types ) {
        CheckArg.isNotNull(name, "name");
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotEmpty(types, "types");

        AstNode node = new AstNode(name);
        node.setProperty(JCR_MIXIN_TYPES, types);
        node.setProperty(JCR_PRIMARY_TYPE, NT_UNSTRUCTURED);
        node.setParent(parent);
        return node;
    }

    /**
     * Constructs an {@link AstNode} with the given name, type and parent node.
     * 
     * @param name the name property of the node; may not be null
     * @param parent the parent of the node; may not be null
     * @param type the mixin type {@link String} for the requested node; may not be null
     * @return the tree node
     */
    public AstNode node( String name,
                         AstNode parent,
                         String type ) {
        CheckArg.isNotNull(name, "name");
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotNull(type, "type");

        AstNode node = new AstNode(name);
        node.setProperty(JCR_MIXIN_TYPES, type);
        node.setProperty(JCR_PRIMARY_TYPE, NT_UNSTRUCTURED);
        node.setParent(parent);
        return node;
    }

    /**
     * Sets the mixin type property for an {@link AstNode}
     * 
     * @param node the node to set the property on; may not be null
     * @param type the mixin type {@link String}; may not be null
     */
    public void setType( AstNode node,
                         String type ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(type, "parent");
        node.setProperty(JCR_MIXIN_TYPES, type);
    }

    /**
     * Utility method to obtain the children of a given node that match the given type
     * 
     * @param astNode the parent node; may not be null
     * @param nodeType the type property of the target child node; may not be null
     * @return the list of typed nodes (may be empty)
     */
    public List<AstNode> getChildrenForType( AstNode astNode,
                                             String nodeType ) {
        CheckArg.isNotNull(astNode, "astNode");
        CheckArg.isNotNull(nodeType, "nodeType");

        List<AstNode> childrenOfType = new ArrayList<AstNode>();
        for (AstNode child : astNode.getChildren()) {
            if (hasMixinType(child, nodeType)) {
                childrenOfType.add(child);
            }
        }

        return childrenOfType;
    }

    /**
     * Utility method to obtain a {@link AstNode} child of a parent {@link AstNode} with the given string name and node type.
     * 
     * @param astNode the parent node; may not be null
     * @param name the name property of the node; may not be null
     * @param nodeType the type property of the target child node; may not be null
     * @return the matched child (may be null)
     */
    public AstNode getChildforNameAndType( AstNode astNode,
                                           String name,
                                           String nodeType ) {
        CheckArg.isNotNull(astNode, "astNode");
        CheckArg.isNotNull(name, "name");
        CheckArg.isNotNull(nodeType, "nodeType");

        for (AstNode child : astNode.getChildren()) {
            if (hasMixinType(child, nodeType)) {
                if (name.equalsIgnoreCase(child.getName())) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Utility method to determine if an {@link AstNode} contains a specific mixin type.
     * 
     * @param node the AstNode
     * @param mixinType the target mixin type {@link String}; may not be null;
     * @return true if the mixinType exists for this node
     */
    public boolean hasMixinType( AstNode node,
                                 String mixinType ) {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotNull(mixinType, "mixinType");
        return node.getMixins().contains(mixinType);
    }
}
