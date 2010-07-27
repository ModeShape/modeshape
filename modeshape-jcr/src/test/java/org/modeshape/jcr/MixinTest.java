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
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;
import org.modeshape.jcr.nodetype.NodeDefinitionTemplate;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

public class MixinTest extends AbstractSessionTest {

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
    static final Name MIXIN_TYPE_A = new BasicName("", "mixinTypeA");
    static final Name MIXIN_TYPE_B = new BasicName("", "mixinTypeB");
    static final Name MIXIN_TYPE_C = new BasicName("", "mixinTypeC");
    static final Name MIXIN_TYPE_WITH_AUTO_PROP = new BasicName("", "mixinTypeWithAutoCreatedProperty");
    static final Name MIXIN_TYPE_WITH_AUTO_CHILD = new BasicName("", "mixinTypeWithAutoCreatedChildNode");
    static final Name PRIMARY_TYPE_A = new BasicName("", "primaryTypeA");

    static final Name PROPERTY_A = new BasicName("", "propertyA");
    static final Name PROPERTY_B = new BasicName("", "propertyB");

    static final Name CHILD_NODE_A = new BasicName("", "nodeA");
    static final Name CHILD_NODE_B = new BasicName("", "nodeB");

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullMixinTypeName() throws Exception {
        graph.create("/a").and();
        graph.set("jcr:primaryType").on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyMixinTypeName() throws Exception {
        graph.create("/a").and();
        graph.set("jcr:primaryType").on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("");
    }

    @Test( expected = NoSuchNodeTypeException.class )
    public void shouldNotAllowInvalidMixinTypeName() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        nodeA.canAddMixin("foo");
    }

    @Test
    public void shouldNotAllowAddingMixinToProtectedNodes() throws Exception {
        Node rootNode = session.getRootNode();
        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));

        assertThat(systemNode.getDefinition().isProtected(), is(true));
        assertThat(systemNode.canAddMixin(JcrMixLexicon.VERSIONABLE.getString(registry)), is(false));
    }

    @Test
    public void shouldAllowAddingMixinIfNoConflict() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
    }

    @Test
    public void shouldAllowAddingMixinIfPrimaryTypeConflicts() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C.getString(registry)), is(true));
    }

    @Test
    public void shouldAllowAddingMixinIfMixinTypeConflicts() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_C.getString(registry)), is(true));
    }

    @Test
    public void shouldAutoCreateAutoCreatedPropertiesOnAddition() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
        Property prop = nodeA.getProperty(PROPERTY_B.getString(registry));

        assertThat(prop, is(notNullValue()));
        assertThat(prop.getLong(), is(10L));
    }

    @Test
    public void shouldAutoCreateAutoCreatedChildNodesOnAddition() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
        Node childNode = nodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(childNode, is(notNullValue()));
        assertThat(childNode.getPrimaryNodeType().getName(), is(JcrNtLexicon.UNSTRUCTURED.getString(registry)));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualPropertyConflicts() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B.getString(registry), "Not a boolean");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
    }

    @Test
    public void shouldAllowAdditionIfResidualPropertyDoesNotConflict() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.setProperty(PROPERTY_B.getString(registry), 10L);

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_PROP.getString(registry));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowAdditionIfResidualChildNodeConflicts() throws Exception {
        graph.create("/a").and().create("/a/" + CHILD_NODE_B).and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a/" + CHILD_NODE_B).to(JcrNtLexicon.BASE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(false));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
    }

    @Test
    public void shouldAllowAdditionIfResidualChildNodeDoesNotConflict() throws Exception {
        graph.create("/a").and().create("/a/" + CHILD_NODE_B).and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED.getString(registry));
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry))
             .on("/a/" + CHILD_NODE_B)
             .to(JcrNtLexicon.UNSTRUCTURED.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_WITH_AUTO_CHILD.getString(registry));
        session.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");

        Node newNodeB = newNodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(newNodeA.isNodeType(MIXIN_TYPE_WITH_AUTO_CHILD.getLocalName()), is(true));
        assertThat(newNodeB.isNodeType("nt:unstructured"), is(true));
        assertThat(newNodeB, is(notNullValue()));

        // Uncomment this to see the problem with the session cache not refreshing the immutable info for /a after a save
        // assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B.getString(registry)));

    }

    @Test
    public void shouldAllowSettingNewPropertyAfterAddingMixin() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_B.getString(registry));

        nodeA.setProperty(PROPERTY_B.getString(registry), "some string");
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");
        Property propB = nodeA.getProperty(PROPERTY_B.getString(registry));

        assertThat(propB, is(notNullValue()));
        assertThat(propB.getValue().getString(), is("some string"));
    }

    @Test
    public void shouldAllowAddingNewChildNodeAfterAddingMixin() throws Exception {
        graph.create("/a").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(JcrMixLexicon.REFERENCEABLE.getString(registry));

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");

        assertThat(nodeA.canAddMixin(MIXIN_TYPE_B.getString(registry)), is(true));
        nodeA.addMixin(MIXIN_TYPE_B.getString(registry));

        nodeA.addNode(CHILD_NODE_B.getString(registry));
        session.save();

        Node newRootNode = session.getRootNode();
        Node newNodeA = newRootNode.getNode("a");
        Node newNodeB = newNodeA.getNode(CHILD_NODE_B.getString(registry));

        assertThat(newNodeB, is(notNullValue()));
        assertThat(newNodeB.getDefinition().getDeclaringNodeType().getName(), is(MIXIN_TYPE_B.getString(registry)));
    }

    @Test
    public void shouldAllowRemovalIfNoConflict() throws Exception {
        graph.create("/a").and().create("/a/nodeB");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test
    public void shouldAllowRemovalIfExistingPropertyWouldHaveDefinition() throws Exception {
        graph.create("/a").and().create("/a");
        graph.set(PROPERTY_B).on("/a").to("true");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingPropertyWouldHaveNoDefinition() throws Exception {
        graph.create("/a").and().create("/a");
        graph.set(PROPERTY_B).on("/a").to("true");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
        session.save();

        rootNode = session.getRootNode();
        nodeA = rootNode.getNode("a");

        assertThat(nodeA.getMixinNodeTypes().length, is(0));
    }

    @Test
    public void shouldAllowRemovalIfExistingChildNodeWouldHaveDefinition() throws Exception {
        graph.create("/a").and().create("/a/nodeB");
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(JcrNtLexicon.UNSTRUCTURED);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
    }

    @Test( expected = ConstraintViolationException.class )
    public void shouldNotAllowRemovalIfExistingChildNodeWouldHaveNoDefinition() throws Exception {
        graph.create("/a").and().create("/a/nodeB").and();
        graph.set(JcrLexicon.PRIMARY_TYPE.getString(registry)).on("/a").to(PRIMARY_TYPE_A);
        graph.set(JcrLexicon.MIXIN_TYPES.getString(registry)).on("/a").to(MIXIN_TYPE_B);

        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.getNode("a");
        nodeA.removeMixin(MIXIN_TYPE_B.getString(registry));
    }

    @SuppressWarnings( {"unchecked", "deprecation"} )
    @Override
    protected List<NodeTypeDefinition> getTestTypes() throws ConstraintViolationException {
        NodeTypeTemplate mixinTypeA = new JcrNodeTypeTemplate(this.context);
        mixinTypeA.setName("mixinTypeA");
        mixinTypeA.setMixin(true);

        NodeDefinitionTemplate childNodeA = new JcrNodeDefinitionTemplate(this.context);
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeA.getNodeDefinitionTemplates().add(childNodeA);

        PropertyDefinitionTemplate propertyA = new JcrPropertyDefinitionTemplate(this.context);
        propertyA.setName("propertyA");
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        mixinTypeA.getPropertyDefinitionTemplates().add(propertyA);

        NodeTypeTemplate mixinTypeB = new JcrNodeTypeTemplate(this.context);
        mixinTypeB.setName("mixinTypeB");
        mixinTypeB.setMixin(true);

        NodeDefinitionTemplate childNodeB = new JcrNodeDefinitionTemplate(this.context);
        childNodeB.setName("nodeB");
        childNodeB.setDefaultPrimaryTypeName("nt:base");
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeB.getNodeDefinitionTemplates().add(childNodeB);

        PropertyDefinitionTemplate propertyB = new JcrPropertyDefinitionTemplate(this.context);
        propertyB.setName("propertyB");
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.BINARY);
        mixinTypeB.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate mixinTypeC = new JcrNodeTypeTemplate(this.context);
        mixinTypeC.setName("mixinTypeC");
        mixinTypeC.setMixin(true);

        childNodeA = new JcrNodeDefinitionTemplate(this.context);
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        mixinTypeC.getNodeDefinitionTemplates().add(childNodeA);

        propertyB = new JcrPropertyDefinitionTemplate(this.context);
        propertyB.setName("propertyB");
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.STRING);
        mixinTypeC.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate mixinTypeWithAutoChild = new JcrNodeTypeTemplate(this.context);
        mixinTypeWithAutoChild.setName("mixinTypeWithAutoCreatedChildNode");
        mixinTypeWithAutoChild.setMixin(true);

        childNodeB = new JcrNodeDefinitionTemplate(this.context);
        childNodeB.setName("nodeB");
        childNodeB.setOnParentVersion(OnParentVersionAction.IGNORE);
        childNodeB.setMandatory(true);
        childNodeB.setAutoCreated(true);
        childNodeB.setDefaultPrimaryTypeName("nt:unstructured");
        childNodeB.setRequiredPrimaryTypeNames(new String[] {"nt:unstructured"});
        mixinTypeWithAutoChild.getNodeDefinitionTemplates().add(childNodeB);

        NodeTypeTemplate mixinTypeWithAutoProperty = new JcrNodeTypeTemplate(this.context);
        mixinTypeWithAutoProperty.setName("mixinTypeWithAutoCreatedProperty");
        mixinTypeWithAutoProperty.setMixin(true);

        propertyB = new JcrPropertyDefinitionTemplate(this.context);
        propertyB.setName("propertyB");
        propertyB.setMandatory(true);
        propertyB.setAutoCreated(true);
        propertyB.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyB.setRequiredType(PropertyType.LONG);
        propertyB.setDefaultValues(new String[] {"10"});
        mixinTypeWithAutoProperty.getPropertyDefinitionTemplates().add(propertyB);

        NodeTypeTemplate primaryTypeA = new JcrNodeTypeTemplate(this.context);
        primaryTypeA.setName("primaryTypeA");

        childNodeA = new JcrNodeDefinitionTemplate(this.context);
        childNodeA.setName("nodeA");
        childNodeA.setOnParentVersion(OnParentVersionAction.IGNORE);
        primaryTypeA.getNodeDefinitionTemplates().add(childNodeA);

        propertyA = new JcrPropertyDefinitionTemplate(this.context);
        propertyA.setName("propertyA");
        propertyA.setOnParentVersion(OnParentVersionAction.IGNORE);
        propertyA.setRequiredType(PropertyType.STRING);
        primaryTypeA.getPropertyDefinitionTemplates().add(propertyA);

        return Arrays.asList(new NodeTypeDefinition[] {mixinTypeA, mixinTypeB, mixinTypeC, mixinTypeWithAutoChild,
            mixinTypeWithAutoProperty, primaryTypeA,});
    }

}
