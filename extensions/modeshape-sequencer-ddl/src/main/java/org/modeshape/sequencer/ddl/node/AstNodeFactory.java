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

import java.util.ArrayList;
import java.util.List;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.basic.LongValueFactory;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;
import org.modeshape.graph.property.basic.StringValueFactory;

/**
 * Utility class which provides construction, editing and assorted methods to work with AstNodes.
 */
public class AstNodeFactory {
    /**
     * The context responsible for maintaining a consistent unique set of graph and node properties. see {@link ExecutionContext}
     */
    private ExecutionContext context;

    private LongValueFactory longFactory;
    private StringValueFactory stringFactory;

    /**
     * @param context
     */
    public AstNodeFactory( ExecutionContext context ) {
        super();
        this.context = context;
        this.stringFactory = new StringValueFactory(new SimpleNamespaceRegistry(), Path.URL_DECODER, Path.URL_ENCODER);
        this.longFactory = new LongValueFactory(Path.URL_DECODER, stringFactory);
    }

    public AstNodeFactory() {
        this.context = new ExecutionContext();
    }

    /**
     * Constructs a {@link Name} with the given string name
     * 
     * @param name the name to use in constructing a {@link Name} instance; may not be null
     * @return name the {@link Name} graph property object
     */
    public Name name( String name ) {
        assert name != null;
        return context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * Constructs an {@link AstNode} with the given string name
     * 
     * @param name the name property of the node; may not be null
     * @return the tree node
     */
    public AstNode node( String name ) {
        assert name != null;

        AstNode node = new AstNode(name(name));
        node.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);

        return node;
    }

    /**
     * Constructs an {@link AstNode} with the given name, types and parent node.
     * 
     * @param name the name property of the node; may not be null
     * @param parent the parent of the node; may not be null
     * @param types the list of mixin types {@link Name} for the requested node; may not be null or empty
     * @return the tree node
     */
    public AstNode node( String name,
                         AstNode parent,
                         Object... types ) {
        assert name != null;
        assert parent != null;
        assert types != null;
        assert types.length > 0;

        AstNode node = new AstNode(name(name));
        node.setProperty(JcrLexicon.MIXIN_TYPES, types);
        node.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
        node.setParent(parent);
        return node;
    }

    /**
     * Constructs an {@link AstNode} with the given name, type and parent node.
     * 
     * @param name the name property of the node; may not be null
     * @param parent the parent of the node; may not be null
     * @param type the mixin type {@link Name} for the requested node; may not be null
     * @return the tree node
     */
    public AstNode node( String name,
                         AstNode parent,
                         Name type ) {
        assert name != null;
        assert parent != null;
        assert type != null;

        AstNode node = new AstNode(name(name));
        node.setProperty(JcrLexicon.MIXIN_TYPES, type);
        node.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
        node.setParent(parent);
        return node;
    }

    /**
     * Sets the mixin type property for an {@link AstNode}
     * 
     * @param node the node to set the property on; may not be null
     * @param type the mixin type {@link Name}; may not be null
     */
    public void setType( AstNode node,
                         Name type ) {
        assert node != null;
        assert type != null;
        node.setProperty(JcrLexicon.MIXIN_TYPES, type);
    }

    /**
     * Utility method to obtain the children of a given node that match the given type
     * 
     * @param astNode the parent node; may not be null
     * @param nodeType the type property of the target child node; may not be null
     * @return the list of typed nodes (may be empty)
     */
    public List<AstNode> getChildrenForType( AstNode astNode,
                                             Name nodeType ) {
        assert astNode != null;
        assert nodeType != null;

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
                                           Name nodeType ) {
        assert astNode != null;
        assert name != null;
        assert nodeType != null;

        for (AstNode child : astNode.getChildren()) {
            if (hasMixinType(child.getProperty(JcrLexicon.MIXIN_TYPES), nodeType)) {
                if (name.equalsIgnoreCase(child.getName().getString())) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Utility method to determine if a list of input mixin {@link Property}s contains a specific mixin type.
     * 
     * @param mixins a list of mixin {@link Property} objects; may not be null
     * @param mixinType the target mixin type; may not be null
     * @return true if the mixinType exists in the property list.
     */
    private boolean hasMixinType( Property mixins,
                                  Name mixinType ) {
        assert mixins != null;
        assert mixinType != null;

        for (Object prop : mixins.getValuesAsArray()) {
            if (prop instanceof Name) {
                if (prop.equals(mixinType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Utility method to determine if an {@link AstNode} contains a specific mixin type.
     * 
     * @param node the AstNode
     * @param mixinType the target mixin type {@link Name}; may not be null;
     * @return true if the mixinType exists for this node
     */
    public boolean hasMixinType( AstNode node,
                                 Name mixinType ) {
        assert node != null;
        assert mixinType != null;
        return hasMixinType(node.getProperty(JcrLexicon.MIXIN_TYPES), mixinType);
    }

    public int createInt( String stringValue ) {
        return longFactory.create(stringValue).intValue();
    }

}
