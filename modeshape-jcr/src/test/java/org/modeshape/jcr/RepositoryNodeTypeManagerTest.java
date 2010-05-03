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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RepositoryNodeTypeManagerTest extends AbstractSessionTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @Override
    protected void initializeContent() {
        super.initializeContent();
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.set("jcr:mixinTypes").on("/a").to(JcrMixLexicon.REFERENCEABLE);
    }

    @Override
    protected void initializeOptions() {
        // Stub out the repository options ...
        options = new EnumMap<JcrRepository.Option, String>(JcrRepository.Option.class);
        options.put(JcrRepository.Option.PROJECT_NODE_TYPES, Boolean.TRUE.toString());
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test
    public void shouldOnlyHaveOneDnaNamespacesNode() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        Node rootNode = session.getRootNode();
        assertThat(rootNode, is(notNullValue()));

        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));
        assertThat(systemNode, is(notNullValue()));

        NodeIterator namespacesNodes = systemNode.getNodes(ModeShapeLexicon.NAMESPACES.getString(registry));
        assertEquals(namespacesNodes.getSize(), 1);
    }

    @Test
    public void shouldOnlyHaveOneNodeTypesNode() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        Node rootNode = session.getRootNode();
        assertThat(rootNode, is(notNullValue()));

        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));
        assertThat(systemNode, is(notNullValue()));

        NodeIterator nodeTypesNodes = systemNode.getNodes(JcrLexicon.NODE_TYPES.getString(registry));
        assertEquals(1, nodeTypesNodes.getSize());
    }

    @Test
    public void shouldAllowMultipleSiblingsDefinitionIfOneSibling() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        // There's no definition for this node or for a * child node that does not allow SNS
        JcrNodeDefinition def = repoTypeManager.findChildNodeDefinition(JcrNtLexicon.NODE_TYPE,
                                                                        Collections.<Name>emptyList(),
                                                                        JcrLexicon.PROPERTY_DEFINITION,
                                                                        JcrNtLexicon.PROPERTY_DEFINITION,
                                                                        1,
                                                                        false);

        assertThat(def, is(notNullValue()));
        assertEquals(def.getName(), JcrLexicon.PROPERTY_DEFINITION.getString(registry));
    }

    public void shouldProjectOntoWorkspaceGraph() throws Exception {
        // projectOnto is called in the JcrWorkspace constructor... just test that the nodes show up
        NamespaceRegistry registry = context.getNamespaceRegistry();

        Node rootNode = session.getRootNode();
        assertThat(rootNode, is(notNullValue()));

        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));
        assertThat(systemNode, is(notNullValue()));

        Node typesNode = systemNode.getNode(ModeShapeLexicon.NODE_TYPES.getString(registry));
        assertThat(typesNode, is(notNullValue()));

        Collection<JcrNodeType> allNodeTypes = repoTypeManager.getAllNodeTypes();
        assertThat(allNodeTypes.size(), greaterThan(0));
        for (JcrNodeType nodeType : allNodeTypes) {
            Node typeNode = typesNode.getNode(nodeType.getName());

            compareNodeTypes(nodeType, typeNode, registry);

        }
    }

    private void compareNodeTypes( JcrNodeType nodeType,
                                   Node typeNode,
                                   NamespaceRegistry registry ) throws Exception {
        assertThat(nodeType.isMixin(), is(typeNode.getProperty(JcrLexicon.IS_MIXIN.getString(registry)).getBoolean()));
        assertThat(nodeType.hasOrderableChildNodes(),
                   is(typeNode.getProperty(JcrLexicon.HAS_ORDERABLE_CHILD_NODES.getString(registry)).getBoolean()));
        try {
            assertThat(nodeType.getPrimaryItemName(), is(typeNode.getProperty(JcrLexicon.PRIMARY_ITEM_NAME.getString(registry))
                                                                 .getString()));
        } catch (PathNotFoundException pnfe) {
            assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        }
        assertThat(nodeType.getName(), is(typeNode.getProperty(JcrLexicon.NODE_TYPE_NAME.getString(registry)).getString()));

        NodeType[] supertypesFromManager = nodeType.getDeclaredSupertypes();
        Value[] supertypesFromGraph = new Value[0];
        try {
            supertypesFromGraph = typeNode.getProperty(JcrLexicon.SUPERTYPES.getString(registry)).getValues();
        } catch (PathNotFoundException pnfe) {
            // Not a mandatory property
        }

        assertThat(supertypesFromGraph.length, is(supertypesFromManager.length));
        for (int i = 0; i < supertypesFromManager.length; i++) {
            assertEquals(supertypesFromManager[i].getName(), supertypesFromGraph[i].getString());
        }

        Map<PropertyDefinitionKey, Node> propertyDefsFromGraph = new HashMap<PropertyDefinitionKey, Node>();
        NodeIterator properties = typeNode.getNodes(JcrLexicon.PROPERTY_DEFINITION.getString(registry));
        while (properties.hasNext()) {
            Node def = properties.nextNode();
            propertyDefsFromGraph.put(new PropertyDefinitionKey(def, registry), def);
        }

        PropertyDefinition[] propertyDefs = nodeType.getDeclaredPropertyDefinitions();
        for (int i = 0; i < propertyDefs.length; i++) {
            JcrPropertyDefinition propertyDef = (JcrPropertyDefinition)propertyDefs[i];
            Node propNode = propertyDefsFromGraph.get(new PropertyDefinitionKey(propertyDef));
            compareProperty(propertyDef, propNode, registry);
        }

        Map<NodeDefinitionKey, Node> nodeDefsFromGraph = new HashMap<NodeDefinitionKey, Node>();
        NodeIterator nodes = typeNode.getNodes(JcrLexicon.CHILD_NODE_DEFINITION.getString(registry));
        while (nodes.hasNext()) {
            Node def = nodes.nextNode();
            nodeDefsFromGraph.put(new NodeDefinitionKey(def, registry), def);
        }

        NodeDefinition[] nodeDefs = nodeType.getDeclaredChildNodeDefinitions();
        for (int i = 0; i < nodeDefs.length; i++) {
            JcrNodeDefinition childNodeDef = (JcrNodeDefinition)nodeDefs[i];
            Node childNodeNode = nodeDefsFromGraph.get(new NodeDefinitionKey(childNodeDef));

            compareChildNode(childNodeDef, childNodeNode, registry);
        }
    }

    private void compareChildNode( JcrNodeDefinition nodeDef,
                                   Node childNodeNode,
                                   NamespaceRegistry registry ) throws Exception {
        assertThat(childNodeNode, is(notNullValue()));

        try {
            Property nameProp = childNodeNode.getProperty(JcrLexicon.NAME.getString(registry));
            assertEquals(nameProp.getString(), nodeDef.getName());
        } catch (PathNotFoundException pnfe) {
            assertThat(nodeDef.getName(), is(JcrNodeType.RESIDUAL_ITEM_NAME));
        }

        Set<Name> requiredPrimaryTypeNames = nodeDef.requiredPrimaryTypeNameSet();
        try {
            Value[] requiredPrimaryTypes = childNodeNode.getProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES.getString(registry))
                                                        .getValues();
            assertEquals(requiredPrimaryTypes.length, requiredPrimaryTypeNames.size());
            for (int i = 0; i < requiredPrimaryTypes.length; i++) {
                Name rptName = context.getValueFactories().getNameFactory().create(requiredPrimaryTypes[i].getString());
                assertEquals(requiredPrimaryTypeNames.contains(rptName), true);
            }
        } catch (PathNotFoundException pnfe) {
            assertEquals(requiredPrimaryTypeNames.size(), 0);
        }

        try {
            Property nameProp = childNodeNode.getProperty(JcrLexicon.DEFAULT_PRIMARY_TYPE.getString(registry));
            assertEquals(nameProp.getString(), nodeDef.getDefaultPrimaryType().getName());
        } catch (PathNotFoundException pnfe) {
            assertThat(nodeDef.getDefaultPrimaryType(), is(nullValue()));
        }

        assertEquals(nodeDef.isAutoCreated(), childNodeNode.getProperty(JcrLexicon.AUTO_CREATED.getString(registry)).getBoolean());
        assertEquals(nodeDef.isMandatory(), childNodeNode.getProperty(JcrLexicon.MANDATORY.getString(registry)).getBoolean());
        assertEquals(nodeDef.isProtected(), childNodeNode.getProperty(JcrLexicon.PROTECTED.getString(registry)).getBoolean());
        assertEquals(nodeDef.allowsSameNameSiblings(),
                     childNodeNode.getProperty(JcrLexicon.SAME_NAME_SIBLINGS.getString(registry)).getBoolean());
        assertEquals(nodeDef.getOnParentVersion(),
                     OnParentVersionAction.valueFromName(childNodeNode.getProperty(JcrLexicon.ON_PARENT_VERSION.getString(registry))
                                                                      .getString()));

    }

    private void compareProperty( JcrPropertyDefinition propertyDef,
                                  Node propNode,
                                  NamespaceRegistry registry ) throws Exception {
        assertThat(propNode, is(notNullValue()));

        try {
            Property nameProp = propNode.getProperty(JcrLexicon.NAME.getString(registry));
            assertEquals(nameProp.getString(), propertyDef.getName());
        } catch (PathNotFoundException pnfe) {
            assertThat(propertyDef.getName(), is(JcrNodeType.RESIDUAL_ITEM_NAME));
        }

        assertEquals(propertyDef.isAutoCreated(), propNode.getProperty(JcrLexicon.AUTO_CREATED.getString(registry)).getBoolean());
        assertEquals(propertyDef.isMandatory(), propNode.getProperty(JcrLexicon.MANDATORY.getString(registry)).getBoolean());
        assertEquals(propertyDef.isMultiple(), propNode.getProperty(JcrLexicon.MULTIPLE.getString(registry)).getBoolean());
        assertEquals(propertyDef.isProtected(), propNode.getProperty(JcrLexicon.PROTECTED.getString(registry)).getBoolean());
        assertEquals(propertyDef.getOnParentVersion(),
                     OnParentVersionAction.valueFromName(propNode.getProperty(JcrLexicon.ON_PARENT_VERSION.getString(registry))
                                                                 .getString()));
        assertEquals(propertyDef.getRequiredType(),
                     PropertyType.valueFromName(propNode.getProperty(JcrLexicon.REQUIRED_TYPE.getString(registry)).getString()));

        Value[] defaultValues = propertyDef.getDefaultValues();
        try {
            Property defaultsProp = propNode.getProperty(JcrLexicon.DEFAULT_VALUES.getString(registry));
            Value[] defaultsFromGraph = defaultsProp.getValues();

            assertEquals(defaultValues.length, defaultsFromGraph.length);
            for (int i = 0; i < defaultValues.length; i++) {
                assertEquals(defaultValues[i].getString(), defaultsFromGraph[i].getString());
            }
        } catch (PathNotFoundException pnfe) {
            assertEquals(defaultValues.length, 0);

        }

        String[] constraintValues = propertyDef.getValueConstraints();
        try {
            Property constraintsProp = propNode.getProperty(JcrLexicon.VALUE_CONSTRAINTS.getString(registry));
            Value[] constraintsFromGraph = constraintsProp.getValues();

            assertEquals(constraintValues.length, constraintsFromGraph.length);
            for (int i = 0; i < constraintValues.length; i++) {
                assertEquals(constraintValues[i], constraintsFromGraph[i].getString());
            }
        } catch (PathNotFoundException pnfe) {
            assertEquals(constraintValues.length, 0);
        }
    }

    // @Test
    // public void shouldInferEmptyFromBadPath() throws Exception {
    // PathFactory pathFactory = context.getValueFactories().getPathFactory();
    // assertThat(repoTypeManager.inferFrom(graph, pathFactory.create("/a")).isEmpty(), is(true));
    // }
    //
    // @Test
    // public void shouldInferEmptyFromInvalidPath() throws Exception {
    // PathFactory pathFactory = context.getValueFactories().getPathFactory();
    // assertThat(repoTypeManager.inferFrom(graph, pathFactory.create("/abc")).isEmpty(), is(true));
    // }
    //    
    // @Test
    // public void shouldInferOwnTypesFromOwnProjection() throws Exception {
    // PathFactory pathFactory = context.getValueFactories().getPathFactory();
    // Path nodeTypePath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, JcrLexicon.NODE_TYPES);
    //        
    // Collection<JcrNodeType> inferredTypes = repoTypeManager.inferFrom(graph, nodeTypePath);
    //        
    // Collection<JcrNodeType> allNodeTypes = repoTypeManager.getAllNodeTypes();
    // assertThat(inferredTypes.size(), is(allNodeTypes.size()));
    //        
    // Map<Name, JcrNodeType> allNodeTypesByName = new HashMap<Name, JcrNodeType>();
    // for (JcrNodeType jnt : allNodeTypes) {
    // allNodeTypesByName.put(jnt.getInternalName(), jnt);
    // }
    //        
    // for (JcrNodeType nodeType : inferredTypes) {
    // JcrNodeType other = allNodeTypesByName.get(nodeType.getInternalName());
    //            
    // System.out.println("Checking: " + other.getName());
    //            
    // assertThat(other, is(notNullValue()));
    // assertThat(areSame(nodeType, other), is(true));
    // }
    // }

    /**
     * Internal class that encapsulates composite unique identifier for property definitions
     */
    class PropertyDefinitionKey {
        String keyString;

        PropertyDefinitionKey( Node node,
                               NamespaceRegistry registry ) throws Exception {
            String propertyName = JcrNodeType.RESIDUAL_ITEM_NAME;

            try {
                propertyName = node.getProperty(JcrLexicon.NAME.getString(registry)).getString();
            } catch (PathNotFoundException pnfe) {
                // Ignorable, means name is not set.
            }
            String requiredType = node.getProperty(JcrLexicon.REQUIRED_TYPE.getString(registry)).getString();
            boolean allowsMultiple = node.getProperty(JcrLexicon.MULTIPLE.getString(registry)).getBoolean();

            this.keyString = propertyName + "-" + requiredType + "-" + allowsMultiple;
        }

        PropertyDefinitionKey( PropertyDefinition def ) {
            String requiredType = PropertyType.nameFromValue(def.getRequiredType());

            this.keyString = def.getName() + "-" + requiredType + "-" + def.isMultiple();
        }

        @Override
        public boolean equals( Object ob ) {
            if (!(ob instanceof PropertyDefinitionKey)) {
                return false;
            }

            return keyString.equals(((PropertyDefinitionKey)ob).keyString);
        }

        @Override
        public int hashCode() {
            return keyString.hashCode();
        }
    }

    /**
     * Internal class that encapsulates composite unique identifier for child node definitions
     */
    class NodeDefinitionKey {
        String keyString;

        NodeDefinitionKey( Node node,
                           NamespaceRegistry registry ) throws Exception {
            StringBuffer buff = new StringBuffer();

            try {
                String nodeName = node.getProperty(JcrLexicon.NAME.getString(registry)).getString();
                buff.append(nodeName);
            } catch (PathNotFoundException pnfe) {
                // Ignorable, means name is not set.
                buff.append(JcrNodeType.RESIDUAL_ITEM_NAME);
            }

            try {
                Value[] requiredTypes = node.getProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES.getString(registry)).getValues();

                for (int i = 0; i < requiredTypes.length; i++) {
                    buff.append('_').append(requiredTypes[i].getString());
                }
            } catch (PathNotFoundException pnfe) {
                // No required types. Weird, but not debilitating.
            }

            buff.append('_').append(node.getProperty(JcrLexicon.SAME_NAME_SIBLINGS.getString(registry)).getBoolean());

            this.keyString = buff.toString();
        }

        NodeDefinitionKey( NodeDefinition def ) {
            StringBuffer buff = new StringBuffer();
            buff.append(def.getName());

            NodeType[] requiredTypes = def.getRequiredPrimaryTypes();
            for (int i = 0; i < requiredTypes.length; i++) {
                buff.append('_').append(requiredTypes[i].getName());
            }

            buff.append('_').append(def.allowsSameNameSiblings());

            this.keyString = buff.toString();
        }

        @Override
        public boolean equals( Object ob ) {
            if (!(ob instanceof NodeDefinitionKey)) {
                return false;
            }

            return keyString.equals(((NodeDefinitionKey)ob).keyString);
        }

        @Override
        public int hashCode() {
            return keyString.hashCode();
        }
    }

}
