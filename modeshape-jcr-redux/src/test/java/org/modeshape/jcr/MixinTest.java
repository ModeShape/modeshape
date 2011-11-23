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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MixinTest extends SingleUseAbstractTest {

    /*
     * Declares the following node types:
     * mixinTypeA with child node nodeA and property propertyA
     * mixinTypeB with child node nodeB and property propertyB
     * mixinTypeC with child node nodeA and property propertyB
     * mixinTypeWithAutoCreatedProperty with auto-created property propertyB
     * mixinTypeWithAutoCreatedChildNode with auto-created child node nodeB
     * mixinTypeC with child node nodeA and property propertyB
     * primaryTypeA with child node nodeA and property propertyA
     */
    static final String MIXIN_TYPE_A = "mixinTypeA";
    static final String MIXIN_TYPE_B = "mixinTypeB";
    static final String MIXIN_TYPE_C = "mixinTypeC";
    static final String BASE_PRIMARY_TYPE = "baseType";
    static final String MIXIN_TYPE_WITH_AUTO_PROP = "mixinTypeWithAutoCreatedProperty";
    static final String MIXIN_TYPE_WITH_AUTO_CHILD = "mixinTypeWithAutoCreatedChildNode";
    static final String PRIMARY_TYPE_A = "primaryTypeA";

    static final String PROPERTY_A = "propertyA";
    static final String PROPERTY_B = "propertyB";

    static final String CHILD_NODE_A = "nodeA";
    static final String CHILD_NODE_B = "nodeB";

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(getTestTypes(), true);
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullMixinTypeName() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyMixinTypeName() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("");
    }

    @Test( expected = NoSuchNodeTypeException.class )
    public void shouldNotAllowInvalidMixinTypeName() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("foo");
    }

    @Test
    public void shouldNotAllowAddingMixinToProtectedNodes() throws Exception {
        Node rootNode = session.getRootNode();
        Node systemNode = rootNode.getNode("jcr:system");

        assertThat(systemNode.getDefinition().isProtected(), is(true));
        assertThat(systemNode.canAddMixin("mix:versionable"), is(false));
    }

    @Test
    public void shouldAllowAddingMixinIfNoConflict() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A).addMixin("mix:referenceable");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B), is(true));
    }

    @Test
    public void shouldAllowAddingMixinIfPrimaryTypeConflicts() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A).addMixin("mix:referenceable");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C), is(true));
    }

    @Test
    public void shouldAllowAddingMixinIfMixinTypeConflicts() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A).addMixin(MIXIN_TYPE_B);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C), is(true));
    }

    @Test
    public void shouldAutoCreateAutoCreatedPropertiesOnAddition() throws Exception {
        session.getRootNode().addNode("a");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP);
        Property prop = nodeA.getProperty(PROPERTY_B);

        assertThat(prop, is(notNullValue()));
        assertThat(prop.getLong(), is(10L));
    }

    @Test
    public void shouldAutoCreateAutoCreatedChildNodesOnAddition() throws Exception {
        session.getRootNode().addNode("a");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD);
        Node childNode = nodeA.getNode(CHILD_NODE_B);

        assertThat(childNode, is(notNullValue()));
        assertThat(childNode.getPrimaryNodeType().getName(), is("nt:unstructured"));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualPropertyConflicts() throws Exception {
        session.getRootNode().addNode("a", "nt:unstructured");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B, "Not a boolean");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP);
    }

    @Test
    public void shouldAllowAdditionIfResidualPropertyDoesNotConflict() throws Exception {
        session.getRootNode().addNode("a", "nt:unstructured");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B, 10L);

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP);
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualChildNodeConflicts() throws Exception {
        session.getRootNode().addNode("a", "nt:unstructured").addNode(CHILD_NODE_B, "nt:base");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD);
    }

    @Test
    public void shouldAllowAdditionIfResidualChildNodeDoesNotConflict() throws Exception {
        session.getRootNode().addNode("a", "nt:unstructured").addNode(CHILD_NODE_B, "nt:unstructured");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD);
        session.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");

        Node newNodeB = newNodeA.getNode(CHILD_NODE_B);

        assertThat(newNodeA.isNodeType(MIXIN_TYPE_WITH_AUTO_CHILD), is(true));
        assertThat(newNodeB.isNodeType("nt:unstructured"), is(true));
        assertThat(newNodeB, is(notNullValue()));

        // Uncomment this to see the problem with the session cache not refreshing the immutable info for /a after a save
        // assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B));

    }

    @Test
    public void shouldAllowSettingNewPropertyAfterAddingMixin() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A).addMixin("mix:referenceable");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B), is(true));
        nodeA.addMixin(MIXIN_TYPE_B);

        nodeA.setProperty(PROPERTY_B, "some string");
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");
        Property propB = nodeA.getProperty(PROPERTY_B);

        assertThat(propB, is(notNullValue()));
        assertThat(propB.getValue().getString(), is("some string"));
    }

    @Test
    public void shouldAllowAddingNewChildNodeAfterAddingMixin() throws Exception {
        session.getRootNode().addNode("a", PRIMARY_TYPE_A).addMixin("mix:referenceable");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B), is(true));
        nodeA.addMixin(MIXIN_TYPE_B);

        nodeA.addNode(CHILD_NODE_B);
        session.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");
        Node newNodeB = newNodeA.getNode(CHILD_NODE_B);

        assertThat(newNodeB, is(notNullValue()));
        assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B));
    }

    @Test
    public void shouldAllowRemovalIfNoConflict() throws Exception {
        Node a = session.getRootNode().addNode("a");
        a.addMixin(MIXIN_TYPE_B);
        a.addNode("nodeB");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test
    public void shouldAllowRemovalIfExistingPropertyWouldHaveDefinition() throws Exception {
        Node a = session.getRootNode().addNode("a", "nt:unstructured");
        a.addMixin(MIXIN_TYPE_B);
        a.setProperty(PROPERTY_B, true);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingPropertyWouldHaveNoDefinition() throws Exception {
        Node a = session.getRootNode().addNode("a", BASE_PRIMARY_TYPE);
        a.addMixin(MIXIN_TYPE_B);
        a.setProperty(PROPERTY_B, "true");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
        session.save();
    }

    @Test
    public void shouldAllowRemovalIfExistingChildNodeWouldHaveDefinition() throws Exception {
        Node a = session.getRootNode().addNode("a", "nt:unstructured");
        a.addMixin(MIXIN_TYPE_B);
        a.addNode("nodeB");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingChildNodeWouldHaveNoDefinition() throws Exception {
        Node a = session.getRootNode().addNode("a", "baseType");
        a.addMixin(MIXIN_TYPE_B);
        a.addNode("nodeB");
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
    }

    @SuppressWarnings( {"unchecked"} )
    protected NodeTypeDefinition[] getTestTypes() throws RepositoryException {
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

        NodeTypeTemplate basePrimaryType = mgr.createNodeTypeTemplate();
        basePrimaryType.setName("baseType");

        NodeTypeTemplate mixinTypeA = mgr.createNodeTypeTemplate();
        mixinTypeA.setName("mixinTypeA");
        mixinTypeA.setMixin(true);

        NodeDefinitionTemplate childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeA.getNodeDefinitionTemplates().add(childNodeA);

        PropertyDefinitionTemplate propertyA = mgr.createPropertyDefinitionTemplate();
        propertyA.setName("propertyA");
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        mixinTypeA.getPropertyDefinitionTemplates().add(propertyA);

        NodeTypeTemplate mixinTypeB = mgr.createNodeTypeTemplate();
        mixinTypeB.setName("mixinTypeB");
        mixinTypeB.setMixin(true);

        NodeDefinitionTemplate childNodeB = mgr.createNodeDefinitionTemplate();
        childNodeB.setName("nodeB");
        childNodeB.setDefaultPrimaryTypeName("nt:base");
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeB.getNodeDefinitionTemplates().add(childNodeB);

        PropertyDefinitionTemplate propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName("propertyB");
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.BINARY);
        mixinTypeB.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate mixinTypeC = mgr.createNodeTypeTemplate();
        mixinTypeC.setName("mixinTypeC");
        mixinTypeC.setMixin(true);

        childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeC.getNodeDefinitionTemplates().add(childNodeA);

        propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName("propertyB");
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.STRING);
        mixinTypeC.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate mixinTypeWithAutoChild = mgr.createNodeTypeTemplate();
        mixinTypeWithAutoChild.setName("mixinTypeWithAutoCreatedChildNode");
        mixinTypeWithAutoChild.setMixin(true);

        childNodeB = mgr.createNodeDefinitionTemplate();
        childNodeB.setName("nodeB");
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        childNodeB.setMandatory(true);
        childNodeB.setAutoCreated(true);
        childNodeB.setDefaultPrimaryTypeName("nt:unstructured");
        childNodeB.setRequiredPrimaryTypeNames(new String[] {"nt:unstructured"});
        mixinTypeWithAutoChild.getNodeDefinitionTemplates().add(childNodeB);

        NodeTypeTemplate mixinTypeWithAutoProperty = mgr.createNodeTypeTemplate();
        mixinTypeWithAutoProperty.setName("mixinTypeWithAutoCreatedProperty");
        mixinTypeWithAutoProperty.setMixin(true);

        propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName("propertyB");
        propertyB.setMandatory(true);
        propertyB.setAutoCreated(true);
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.LONG);
        propertyB.setDefaultValues(new Value[] {session.getValueFactory().createValue("10")});
        mixinTypeWithAutoProperty.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate primaryTypeA = mgr.createNodeTypeTemplate();
        primaryTypeA.setName("primaryTypeA");

        childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        primaryTypeA.getNodeDefinitionTemplates().add(childNodeA);

        propertyA = mgr.createPropertyDefinitionTemplate();
        propertyA.setName("propertyA");
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        primaryTypeA.getPropertyDefinitionTemplates().add(propertyA);

        return new NodeTypeDefinition[] {basePrimaryType, mixinTypeA, mixinTypeB, mixinTypeC, mixinTypeWithAutoChild,
            mixinTypeWithAutoProperty, primaryTypeA,};
    }

}
