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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
import org.modeshape.common.FixFor;

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
     * mixinProtected with child node childProtected and property propertyProtected
     */
    static final String MIXIN_TYPE_A = "mixinTypeA";
    static final String MIXIN_TYPE_B = "mixinTypeB";
    static final String MIXIN_TYPE_C = "mixinTypeC";
    static final String BASE_PRIMARY_TYPE = "baseType";
    static final String MIXIN_TYPE_WITH_AUTO_PROP = "mixinTypeWithAutoCreatedProperty";
    static final String MIXIN_TYPE_WITH_AUTO_CHILD = "mixinTypeWithAutoCreatedChildNode";
    static final String PRIMARY_TYPE_A = "primaryTypeA";
    static final String MIXIN_PROTECTED = "mixinProtected";

    static final String PROPERTY_A = "propertyA";
    static final String PROPERTY_B = "propertyB";
    static final String PROPERTY_PROTECTED = "propertyProtected";

    static final String CHILD_NODE_A = "nodeA";
    static final String CHILD_NODE_B = "nodeB";
    static final String CHILD_PROTECTED = "childProtected";

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
        Property b = nodeA.setProperty(PROPERTY_B, "Not a boolean");
        assertThat(b.getType(), is(not(PropertyType.UNDEFINED))); // see JavaDoc on javax.jcr.PropertyType.UNDEFINED

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
        a.addNode(CHILD_NODE_B);
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
        a.addNode(CHILD_NODE_B);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingChildNodeWouldHaveNoDefinition() throws Exception {
        Node a = session.getRootNode().addNode("a", BASE_PRIMARY_TYPE);
        a.addMixin(MIXIN_TYPE_B);
        a.addNode(CHILD_NODE_B);
        session.save();

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B);
    }

    @Test
    @FixFor( "MODE-1912" )
    public void shouldRemoveProtectedChildrenAndPropertiesWhenRemovingMixin() throws Exception {
        Node protectedNode = session.getRootNode().addNode("protected", BASE_PRIMARY_TYPE);
        protectedNode.addMixin(MIXIN_PROTECTED);
        protectedNode.addNode(CHILD_PROTECTED);
        protectedNode.setProperty(PROPERTY_PROTECTED, "a value");

        session.save();

        //change the item definitions to protected and run the validations
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate mixinProtected = createMixinProtected(mgr, true);
        mgr.registerNodeType(mixinProtected, true);

        String childPath = "/protected/" + CHILD_PROTECTED;
        String propertyPath = "/protected/" + PROPERTY_PROTECTED;

        protectedNode = session.getNode("/protected");
        assertNotNull(session.getNode(childPath));
        assertNotNull(session.getProperty(propertyPath));
        protectedNode.removeMixin(MIXIN_PROTECTED);
        session.save();

        try {
            session.getNode(childPath);
            fail("Protected child should have been removed");
        } catch (PathNotFoundException path) {
            //expected
        }

        try {
            session.getProperty(propertyPath);
            fail("Protected property should have been removed");
        } catch (PathNotFoundException path) {
            //expected
        }
    }

    @Test
    @FixFor( "MODE-1949")
    public void shouldNotAllowMixinRemovalIfPropertiesDefinedInParentMixinArePresent() throws Exception {
        Node folder = session.getNode("/").addNode("folder", "nt:folder");
        //add the publishArea mixin which inherits from mix:title
        folder.addMixin("mode:publishArea");
        folder.setProperty("jcr:title", "title");
        folder.setProperty("jcr:description", "description");
        session.save();

        try {
            folder.removeMixin("mode:publishArea");
            session.save();
            fail("The mixin should not be removed because there are still properties defined from the parent mixin");
        } catch (ConstraintViolationException e) {
            //expected
        }

        folder.setProperty("jcr:title", (Value)null);
        session.save();

        try {
            folder.removeMixin("mode:publishArea");
            session.save();
            fail("The mixin should not be removed because there are still properties defined from the parent mixin");
        } catch (ConstraintViolationException e) {
            //expected
        }

        folder.setProperty("jcr:description", (Value)null);
        session.save();
        folder.removeMixin("mode:publishArea");
        session.save();
    }

    protected NodeTypeDefinition[] getTestTypes() throws RepositoryException {
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

        NodeTypeTemplate basePrimaryType = createBasePrimaryType(mgr);
        NodeTypeTemplate mixinTypeA = createMixinA(mgr);
        NodeTypeTemplate mixinTypeB = createMixinB(mgr);
        NodeTypeTemplate mixinTypeC = createMixinC(mgr);
        NodeTypeTemplate mixinTypeWithAutoChild = createMixinWithAutoChild(mgr);
        NodeTypeTemplate mixinTypeWithAutoProperty = createMixinWithAutoProperty(mgr);
        NodeTypeTemplate primaryTypeA = createPrimaryTypeA(mgr);
        //create the items on mixin protected as "un-protected" first, so we can create children & properties
        NodeTypeTemplate mixinProtected = createMixinProtected(mgr, false);

        return new NodeTypeDefinition[] { basePrimaryType, mixinTypeA, mixinTypeB, mixinTypeC, mixinTypeWithAutoChild,
                mixinTypeWithAutoProperty, primaryTypeA, mixinProtected};
    }

    private NodeTypeTemplate createBasePrimaryType( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate basePrimaryType = mgr.createNodeTypeTemplate();
        basePrimaryType.setName(BASE_PRIMARY_TYPE);
        return basePrimaryType;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinWithAutoProperty( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate mixinTypeWithAutoProperty = mgr.createNodeTypeTemplate();
        mixinTypeWithAutoProperty.setName("mixinTypeWithAutoCreatedProperty");
        mixinTypeWithAutoProperty.setMixin(true);

        PropertyDefinitionTemplate propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName(PROPERTY_B);
        propertyB.setMandatory(true);
        propertyB.setAutoCreated(true);
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.LONG);
        propertyB.setDefaultValues(new Value[] {session.getValueFactory().createValue("10")});
        mixinTypeWithAutoProperty.getPropertyDefinitionTemplates().add(propertyB);
        return mixinTypeWithAutoProperty;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinWithAutoChild( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate mixinTypeWithAutoChild = mgr.createNodeTypeTemplate();
        mixinTypeWithAutoChild.setName("mixinTypeWithAutoCreatedChildNode");
        mixinTypeWithAutoChild.setMixin(true);

        NodeDefinitionTemplate childNodeB = mgr.createNodeDefinitionTemplate();
        childNodeB.setName(CHILD_NODE_B);
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        childNodeB.setMandatory(true);
        childNodeB.setAutoCreated(true);
        childNodeB.setDefaultPrimaryTypeName("nt:unstructured");
        childNodeB.setRequiredPrimaryTypeNames(new String[] {"nt:unstructured"});
        mixinTypeWithAutoChild.getNodeDefinitionTemplates().add(childNodeB);
        return mixinTypeWithAutoChild;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createPrimaryTypeA( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate primaryTypeA = mgr.createNodeTypeTemplate();
        primaryTypeA.setName(PRIMARY_TYPE_A);

        NodeDefinitionTemplate childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName(CHILD_NODE_A);
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        primaryTypeA.getNodeDefinitionTemplates().add(childNodeA);

        PropertyDefinitionTemplate propertyA = mgr.createPropertyDefinitionTemplate();
        propertyA.setName(PROPERTY_A);
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        primaryTypeA.getPropertyDefinitionTemplates().add(propertyA);
        return primaryTypeA;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinC( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate mixinTypeC = mgr.createNodeTypeTemplate();
        mixinTypeC.setName(MIXIN_TYPE_C);
        mixinTypeC.setMixin(true);

        PropertyDefinitionTemplate propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName(PROPERTY_B);
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.STRING);
        mixinTypeC.getPropertyDefinitionTemplates().add(propertyB);
        return mixinTypeC;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinB( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate mixinTypeB = mgr.createNodeTypeTemplate();
        mixinTypeB.setName(MIXIN_TYPE_B);
        mixinTypeB.setMixin(true);

        NodeDefinitionTemplate childNodeB = mgr.createNodeDefinitionTemplate();
        childNodeB.setName(CHILD_NODE_B);
        childNodeB.setDefaultPrimaryTypeName("nt:base");
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeB.getNodeDefinitionTemplates().add(childNodeB);

        PropertyDefinitionTemplate propertyB = mgr.createPropertyDefinitionTemplate();
        propertyB.setName(PROPERTY_B);
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.BINARY);
        mixinTypeB.getPropertyDefinitionTemplates().add(propertyB);

        NodeDefinitionTemplate childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName(CHILD_NODE_A);
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeB.getNodeDefinitionTemplates().add(childNodeA);
        return mixinTypeB;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinProtected( NodeTypeManager mgr, boolean makeItemsProtected ) throws RepositoryException {
        NodeTypeTemplate mixinProtected = mgr.createNodeTypeTemplate();
        mixinProtected.setName(MIXIN_PROTECTED);
        mixinProtected.setMixin(true);

        NodeDefinitionTemplate childProtected = mgr.createNodeDefinitionTemplate();
        childProtected.setName(CHILD_PROTECTED);
        childProtected.setProtected(makeItemsProtected);
        childProtected.setDefaultPrimaryTypeName("nt:base");
        childProtected.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinProtected.getNodeDefinitionTemplates().add(childProtected);

        PropertyDefinitionTemplate propertyProtected = mgr.createPropertyDefinitionTemplate();
        propertyProtected.setProtected(makeItemsProtected);
        propertyProtected.setName(PROPERTY_PROTECTED);
        propertyProtected.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyProtected.setRequiredType(PropertyType.STRING);
        mixinProtected.getPropertyDefinitionTemplates().add(propertyProtected);

        return mixinProtected;
    }

    @SuppressWarnings( {"unchecked"} )
    private NodeTypeTemplate createMixinA( NodeTypeManager mgr ) throws RepositoryException {
        NodeTypeTemplate mixinTypeA = mgr.createNodeTypeTemplate();
        mixinTypeA.setName(MIXIN_TYPE_A);
        mixinTypeA.setMixin(true);

        NodeDefinitionTemplate childNodeA = mgr.createNodeDefinitionTemplate();
        childNodeA.setName(CHILD_NODE_A);
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeA.getNodeDefinitionTemplates().add(childNodeA);

        PropertyDefinitionTemplate propertyA = mgr.createPropertyDefinitionTemplate();
        propertyA.setName(PROPERTY_A);
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        mixinTypeA.getPropertyDefinitionTemplates().add(propertyA);
        return mixinTypeA;
    }

}
