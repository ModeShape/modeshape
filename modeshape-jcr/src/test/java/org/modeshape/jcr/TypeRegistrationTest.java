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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.jcr.nodetype.InvalidNodeTypeDefinitionException;
import org.modeshape.jcr.nodetype.NodeDefinitionTemplate;
import org.modeshape.jcr.nodetype.NodeTypeExistsException;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

@SuppressWarnings( "deprecation" )
public class TypeRegistrationTest extends AbstractSessionTest {

    private static final String TEST_TYPE_NAME = "mode:testNode";
    private static final String TEST_TYPE_NAME2 = "mode:testNode2";
    private static final String TEST_PROPERTY_NAME = "mode:testProperty";
    private static final String TEST_CHILD_NODE_NAME = "mode:testChildNode";

    private JcrNodeTypeTemplate ntTemplate;
    private NamespaceRegistry registry;
    private NameFactory nameFactory;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        ntTemplate = new JcrNodeTypeTemplate(context);
        registry = context.getNamespaceRegistry();
        nameFactory = context.getValueFactories().getNameFactory();
    }

    @Ignore
    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNodeTypeWithInvalidName() throws Exception {
        ntTemplate.setName("nt-name");
    }

    @Ignore
    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowPropertyWithInvalidName() throws Exception {
        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(":foo[2]");
        System.out.println(prop.getName());
    }

    @Ignore
    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowChildNodeWithInvalidName() throws Exception {
        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(":foo[2]");
    }

    @Ignore
    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowChildNodeWithInvalidRequiredPrimaryTypeName() throws Exception {
        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setRequiredPrimaryTypeNames(new String[] {"nt-Name"});
    }

    @Ignore
    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowChildNodeWithInvalidDefaultPrimaryTypeName() throws Exception {
        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setDefaultPrimaryTypeName("nt-Name");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNodeTypeWithNoName() throws Exception {
        ntTemplate.setName(null);
    }

    @Test
    public void shouldTranslateNameImmediately() throws Exception {
        ntTemplate.setName("{" + ModeShapeLexicon.Namespace.URI + "}foo");
        assertThat(ntTemplate.getName(), is("mode:foo"));
    }

    @Test
    public void shouldAllowNewDefinitionWithNoChildNodesOrProperties() throws Exception {
        Name testTypeName = nameFactory.create(TEST_TYPE_NAME);
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base"});

        JcrNodeType testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);

        assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
        JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
        assertThat(nodeTypeFromRepo, is(notNullValue()));
        assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));
    }

    @Test( expected = NodeTypeExistsException.class )
    public void shouldNotAllowModificationIfAllowUpdatesIsFalse() throws Exception {
        ntTemplate.setName("nt:base");
        repoTypeManager.registerNodeType(ntTemplate, false);
    }

    @Test( expected = NodeTypeExistsException.class )
    public void shouldNotAllowRedefinitionOfNewTypeIfAllowUpdatesIsFalse() throws Exception {
        Name testTypeName = nameFactory.create(TEST_TYPE_NAME);
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base"});

        JcrNodeType testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);

        assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
        JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
        assertThat(nodeTypeFromRepo, is(notNullValue()));
        assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));

        testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);
    }

    @Test
    public void shouldAllowDefinitionWithExistingSupertypes() throws Exception {
        Name testTypeName = nameFactory.create(TEST_TYPE_NAME);
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeType testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);

        assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
        JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
        assertThat(nodeTypeFromRepo, is(notNullValue()));
        assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));

    }

    @Test
    public void shouldAllowDefinitionWithSupertypesFromTypesRegisteredInSameCall() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeTypeTemplate ntTemplate2 = new JcrNodeTypeTemplate(context);
        ntTemplate2.setName(TEST_TYPE_NAME2);
        ntTemplate2.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, ntTemplate2});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowDefinitionWithSupertypesFromTypesRegisteredInSameCallInWrongOrder() throws Exception {
        // Try to register the supertype AFTER the class that registers it
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeTypeTemplate ntTemplate2 = new JcrNodeTypeTemplate(context);
        ntTemplate2.setName(TEST_TYPE_NAME2);
        ntTemplate2.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        repoTypeManager.registerNodeTypes(Arrays.asList(new NodeTypeDefinition[] {ntTemplate2, ntTemplate}), false);
    }

    @Test
    public void shouldAllowDefinitionWithAProperty() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setRequiredType(PropertyType.LONG);

        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowDefinitionWithProperties() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setRequiredType(PropertyType.LONG);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrPropertyDefinitionTemplate prop2 = new JcrPropertyDefinitionTemplate(this.context);
        prop2.setRequiredType(PropertyType.STRING);
        prop2.setMultiple(true);
        ntTemplate.getPropertyDefinitionTemplates().add(prop2);

        JcrPropertyDefinitionTemplate prop3 = new JcrPropertyDefinitionTemplate(this.context);
        prop3.setName(TEST_PROPERTY_NAME);
        prop3.setRequiredType(PropertyType.STRING);
        ntTemplate.getPropertyDefinitionTemplates().add(prop3);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowAutocreatedPropertyWithNoDefault() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.LONG);
        prop.setAutoCreated(true);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowAutocreatedResidualProperty() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(JcrNodeType.RESIDUAL_ITEM_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        prop.setAutoCreated(true);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowAutocreatedNamedPropertyWithDefault() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.STRING);
        prop.setAutoCreated(true);
        prop.setDefaultValues(new String[] {"<default>"});
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowSingleValuedPropertyWithMultipleDefaults() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.STRING);
        prop.setAutoCreated(true);
        prop.setDefaultValues(new String[] {"<default>", "too many values"});
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowMandatoryResidualProperty() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(JcrNodeType.RESIDUAL_ITEM_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        prop.setMandatory(true);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowTypeWithChildNode() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setSameNameSiblings(true);

        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowTypeWithMultipleChildNodes() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setSameNameSiblings(true);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:unstructured"});
        child.setSameNameSiblings(false);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME + "2");
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setSameNameSiblings(true);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowAutocreatedChildNodeWithNoDefaultPrimaryType() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setAutoCreated(true);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowMandatoryResidualChildNode() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrNodeType.RESIDUAL_ITEM_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setMandatory(true);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingProtectedProperty() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base", "mix:referenceable"});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(JcrLexicon.PRIMARY_TYPE.getString(registry));
        prop.setRequiredType(PropertyType.NAME);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingProtectedChildNode() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"mode:root", "mix:referenceable"});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingMandatoryChildNodeWithOptionalChildNode() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"jcr:versionHistory",});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        ntTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowOverridingPropertyFromCommonAncestor() throws Exception {
        /*
         * testNode declares prop testProperty
         * testNodeB extends testNode
         * testNodeC extends testNode
         * testNodeD extends testNodeB and testNodeC and overrides testProperty --> LEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrNodeTypeTemplate nodeCTemplate = new JcrNodeTypeTemplate(this.context);
        nodeCTemplate.setName(TEST_TYPE_NAME + "C");
        nodeCTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrNodeTypeTemplate nodeDTemplate = new JcrNodeTypeTemplate(this.context);
        nodeDTemplate.setName(TEST_TYPE_NAME + "D");
        nodeDTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME + "B", TEST_TYPE_NAME + "C"});

        prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.STRING);
        nodeDTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate, nodeCTemplate,
            nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingPropertyFromDifferentAncestors() throws Exception {
        /*
         * testNode 
         * testNodeB extends testNode and declares prop testProperty
         * testNodeC extends testNode and declares prop testProperty
         * testNodeD extends testNodeB and testNodeC and overrides testProperty --> ILLEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        nodeBTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeCTemplate = new JcrNodeTypeTemplate(this.context);
        nodeCTemplate.setName(TEST_TYPE_NAME + "C");
        nodeCTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        nodeCTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeDTemplate = new JcrNodeTypeTemplate(this.context);
        nodeDTemplate.setName(TEST_TYPE_NAME + "D");
        nodeDTemplate.setDeclaredSuperTypeNames(new String[] {nodeBTemplate.getName(), nodeCTemplate.getName()});

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate, nodeCTemplate,
            nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowOverridingChildNodeFromCommonAncestor() throws Exception {
        /*
         * testNode declares node testChildNode
         * testNodeB extends testNode
         * testNodeC extends testNode
         * testNodeD extends testNodeB and testNodeC and overrides testChildNode --> LEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        ntTemplate.getNodeDefinitionTemplates().add(child);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrNodeTypeTemplate nodeCTemplate = new JcrNodeTypeTemplate(this.context);
        nodeCTemplate.setName(TEST_TYPE_NAME + "C");
        nodeCTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrNodeTypeTemplate nodeDTemplate = new JcrNodeTypeTemplate(this.context);
        nodeDTemplate.setName(TEST_TYPE_NAME + "D");
        nodeDTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME + "B", TEST_TYPE_NAME + "C"});

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:unstructured"});
        nodeDTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate, nodeCTemplate,
            nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingChildNodeFromDifferentAncestors() throws Exception {
        /*
         * testNode 
         * testNodeB extends testNode and declares node testChildNode
         * testNodeC extends testNode and declares node testChildNode
         * testNodeD extends testNodeB and testNodeC and overrides testChildNode --> ILLEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        nodeBTemplate.getNodeDefinitionTemplates().add(child);

        JcrNodeTypeTemplate nodeCTemplate = new JcrNodeTypeTemplate(this.context);
        nodeCTemplate.setName(TEST_TYPE_NAME + "C");
        nodeCTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(JcrLexicon.SYSTEM.getString(registry));
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        nodeCTemplate.getNodeDefinitionTemplates().add(child);

        JcrNodeTypeTemplate nodeDTemplate = new JcrNodeTypeTemplate(this.context);
        nodeDTemplate.setName(TEST_TYPE_NAME + "D");
        nodeDTemplate.setDeclaredSuperTypeNames(new String[] {nodeBTemplate.getName(), nodeCTemplate.getName()});

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate, nodeCTemplate,
            nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowExtendingPropertyIfMultipleChanges() throws Exception {
        /*
         * testNode declares SV property testProperty
         * testNodeB extends testNode with MV property testProperty with incompatible type -> LEGAL
         * testNodeC extends testNode, testNodeB -> LEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.DOUBLE);
        prop.setMultiple(false);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.BOOLEAN);
        prop.setMultiple(true);
        nodeBTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeCTemplate = new JcrNodeTypeTemplate(this.context);
        nodeCTemplate.setName(TEST_TYPE_NAME + "C");
        nodeCTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME, nodeBTemplate.getName()});

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate, nodeCTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowOverridingPropertyIfTypeNarrows() throws Exception {
        /*
         * testNode declares SV property testProperty of type UNDEFINED
         * testNodeB extends testNode with SV property testProperty of type STRING -> LEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.UNDEFINED);
        prop.setMultiple(false);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.STRING);
        prop.setMultiple(false);
        nodeBTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingPropertyIfTypeDoesNotNarrow() throws Exception {
        /*
         * testNode declares SV property testProperty of type BOOLEAN
         * testNodeB extends testNode with SV property testProperty of type DOUBLE -> ILLEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.BOOLEAN);
        prop.setMultiple(false);
        ntTemplate.getPropertyDefinitionTemplates().add(prop);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        prop = new JcrPropertyDefinitionTemplate(this.context);
        prop.setName(TEST_PROPERTY_NAME);
        prop.setRequiredType(PropertyType.DOUBLE);
        prop.setMultiple(false);
        nodeBTemplate.getPropertyDefinitionTemplates().add(prop);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test
    public void shouldAllowOverridingChildNodeIfRequiredTypesNarrow() throws Exception {
        /*
         * testNode declares No-SNS childNode testChildNode requiring type nt:hierarchy
         * testNodeB extends testNode with No-SNS childNode testChildNode requiring type nt:file -> LEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:hierarchyNode"});
        child.setSameNameSiblings(false);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:file"});
        child.setSameNameSiblings(false);
        nodeBTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowOverridingChildNodeIfRequiredTypesDoNotNarrow() throws Exception {
        /*
         * testNode declares No-SNS childNode testChildNode requiring type nt:hierarchy
         * testNodeB extends testNode with No-SNS childNode testChildNode requiring type nt:base -> ILLEGAL
         */
        ntTemplate.setName(TEST_TYPE_NAME);
        ntTemplate.setDeclaredSuperTypeNames(new String[] {"nt:base",});

        JcrNodeDefinitionTemplate child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:hierarchyNode"});
        child.setSameNameSiblings(false);
        ntTemplate.getNodeDefinitionTemplates().add(child);

        JcrNodeTypeTemplate nodeBTemplate = new JcrNodeTypeTemplate(this.context);
        nodeBTemplate.setName(TEST_TYPE_NAME + "B");
        nodeBTemplate.setDeclaredSuperTypeNames(new String[] {TEST_TYPE_NAME});

        child = new JcrNodeDefinitionTemplate(this.context);
        child.setName(TEST_CHILD_NODE_NAME);
        child.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        child.setSameNameSiblings(false);
        nodeBTemplate.getNodeDefinitionTemplates().add(child);

        List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate, nodeBTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    }

    /*
     * Unregistration tests
     */

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowUnregisteringNullCollection() throws Exception {
        repoTypeManager.unregisterNodeType(null);
    }

    @Test( expected = NoSuchNodeTypeException.class )
    public void shouldNotAllowUnregisteringInvalidTypeNames() throws Exception {
        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {JcrNtLexicon.FILE, JcrLexicon.DATA}));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringSupertype() throws Exception {
        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {JcrNtLexicon.HIERARCHY_NODE,}));

    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringRequiredPrimaryType() throws Exception {
        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {JcrNtLexicon.FROZEN_NODE,}));
    }

    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringDefaultPrimaryType() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);

        JcrNodeDefinitionTemplate childNode = new JcrNodeDefinitionTemplate(this.context);
        childNode.setDefaultPrimaryType(JcrNtLexicon.FILE.getString(this.registry));
        ntTemplate.getNodeDefinitionTemplates().add(childNode);

        try {
            repoTypeManager.registerNodeType(ntTemplate, false);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {JcrNtLexicon.FILE,}));
    }

    @Test
    public void shouldAllowUnregisteringUnusedType() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);

        JcrNodeDefinitionTemplate childNode = new JcrNodeDefinitionTemplate(this.context);
        childNode.setDefaultPrimaryType(JcrNtLexicon.FILE.getString(this.registry));
        ntTemplate.getNodeDefinitionTemplates().add(childNode);

        try {
            repoTypeManager.registerNodeType(ntTemplate, false);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        Name typeNameAsName = nameFactory.create(TEST_TYPE_NAME);
        int nodeTypeCount = repoTypeManager.getAllNodeTypes().size();
        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {typeNameAsName}));
        assertThat(repoTypeManager.getAllNodeTypes().size(), is(nodeTypeCount - 1));
        assertThat(repoTypeManager.getNodeType(typeNameAsName), is(nullValue()));
    }

    @Test
    public void shouldAllowUnregisteringUnusedTypesWithMutualDependencies() throws Exception {
        ntTemplate.setName(TEST_TYPE_NAME);

        JcrNodeDefinitionTemplate childNode = new JcrNodeDefinitionTemplate(this.context);
        childNode.setDefaultPrimaryType(TEST_TYPE_NAME2);
        ntTemplate.getNodeDefinitionTemplates().add(childNode);

        NodeTypeTemplate ntTemplate2 = new JcrNodeTypeTemplate(this.context);
        ntTemplate2.setName(TEST_TYPE_NAME2);

        JcrNodeDefinitionTemplate childNode2 = new JcrNodeDefinitionTemplate(this.context);
        childNode2.setDefaultPrimaryType(TEST_TYPE_NAME);
        ntTemplate2.getNodeDefinitionTemplates().add(childNode2);

        try {
            repoTypeManager.registerNodeTypes(Arrays.asList(new NodeTypeDefinition[] {ntTemplate, ntTemplate2}), false);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        Name typeNameAsName = nameFactory.create(TEST_TYPE_NAME);
        Name type2NameAsName = nameFactory.create(TEST_TYPE_NAME2);
        int nodeTypeCount = repoTypeManager.getAllNodeTypes().size();
        repoTypeManager.unregisterNodeType(Arrays.asList(new Name[] {typeNameAsName, type2NameAsName}));
        assertThat(repoTypeManager.getAllNodeTypes().size(), is(nodeTypeCount - 2));
        assertThat(repoTypeManager.getNodeType(typeNameAsName), is(nullValue()));
        assertThat(repoTypeManager.getNodeType(type2NameAsName), is(nullValue()));

    }

    private void compareTemplatesToNodeTypes( List<NodeTypeDefinition> templates,
                                              List<JcrNodeType> nodeTypes ) {
        assertThat(templates.size(), is(nodeTypes.size()));

        for (int i = 0; i < nodeTypes.size(); i++) {
            JcrNodeTypeTemplate jntt = (JcrNodeTypeTemplate)templates.get(i);
            compareTemplateToNodeType(jntt, null);
            compareTemplateToNodeType(jntt, nodeTypes.get(i));
        }
    }

    private void compareTemplateToNodeType( JcrNodeTypeTemplate template,
                                            JcrNodeType nodeType ) {
        Name nodeTypeName = nameFactory.create(template.getName());
        if (nodeType == null) {
            nodeType = repoTypeManager.getNodeType(nodeTypeName);
            assertThat(nodeType.nodeTypeManager(), is(notNullValue()));
        }

        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.getName(), is(template.getName()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(template.getDeclaredSupertypeNames().length));
        for (int i = 0; i < template.getDeclaredSupertypeNames().length; i++) {
            assertThat(template.getDeclaredSupertypeNames()[i], is(nodeType.getDeclaredSupertypes()[i].getName()));
        }
        assertThat(template.isMixin(), is(nodeType.isMixin()));
        assertThat(template.hasOrderableChildNodes(), is(nodeType.hasOrderableChildNodes()));

        PropertyDefinition[] propertyDefs = nodeType.getDeclaredPropertyDefinitions();
        List<PropertyDefinitionTemplate> propertyTemplates = template.getPropertyDefinitionTemplates();

        assertThat(propertyDefs.length, is(propertyTemplates.size()));
        for (PropertyDefinitionTemplate pt : propertyTemplates) {
            JcrPropertyDefinitionTemplate propertyTemplate = (JcrPropertyDefinitionTemplate)pt;

            PropertyDefinition matchingDefinition = null;
            for (int i = 0; i < propertyDefs.length; i++) {
                PropertyDefinition pd = propertyDefs[i];

                String ptName = propertyTemplate.getName() == null ? JcrNodeType.RESIDUAL_ITEM_NAME : propertyTemplate.getName();
                if (pd.getName().equals(ptName) && pd.getRequiredType() == propertyTemplate.getRequiredType()
                    && pd.isMultiple() == propertyTemplate.isMultiple()) {
                    matchingDefinition = pd;
                    break;
                }
            }

            comparePropertyTemplateToPropertyDefinition(propertyTemplate, (JcrPropertyDefinition)matchingDefinition);
        }

        NodeDefinition[] childNodeDefs = nodeType.getDeclaredChildNodeDefinitions();
        List<NodeDefinitionTemplate> childNodeTemplates = template.getNodeDefinitionTemplates();

        assertThat(childNodeDefs.length, is(childNodeTemplates.size()));
        for (NodeDefinitionTemplate nt : childNodeTemplates) {
            JcrNodeDefinitionTemplate childNodeTemplate = (JcrNodeDefinitionTemplate)nt;

            NodeDefinition matchingDefinition = null;
            for (int i = 0; i < childNodeDefs.length; i++) {
                NodeDefinition nd = childNodeDefs[i];

                String ntName = childNodeTemplate.getName() == null ? JcrNodeType.RESIDUAL_ITEM_NAME : childNodeTemplate.getName();
                if (nd.getName().equals(ntName) && nd.allowsSameNameSiblings() == childNodeTemplate.allowsSameNameSiblings()) {

                    if (nd.getRequiredPrimaryTypes().length != childNodeTemplate.getRequiredPrimaryTypeNames().length) continue;

                    boolean matchesOnRequiredTypes = true;
                    for (int j = 0; j < nd.getRequiredPrimaryTypes().length; j++) {
                        String ndName = nd.getRequiredPrimaryTypes()[j].getName();
                        String tempName = childNodeTemplate.getRequiredPrimaryTypeNames()[j];
                        if (!ndName.equals(tempName)) {
                            matchesOnRequiredTypes = false;
                            break;
                        }
                    }

                    if (matchesOnRequiredTypes) {
                        matchingDefinition = nd;
                        break;
                    }
                }
            }

            compareNodeTemplateToNodeDefinition(childNodeTemplate, (JcrNodeDefinition)matchingDefinition);
        }

    }

    private void comparePropertyTemplateToPropertyDefinition( JcrPropertyDefinitionTemplate template,
                                                              JcrPropertyDefinition definition ) {

        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(emptyIfNull(template.getValueConstraints()), is(definition.getValueConstraints()));
        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.getRequiredType(), is(definition.getRequiredType()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isMultiple(), is(definition.isMultiple()));
        assertThat(template.isProtected(), is(definition.isProtected()));
    }

    private void compareNodeTemplateToNodeDefinition( JcrNodeDefinitionTemplate template,
                                                      JcrNodeDefinition definition ) {
        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isProtected(), is(definition.isProtected()));

        assertThat(template.getDefaultPrimaryType(), is(definition.getDefaultPrimaryType()));
        assertThat(template.allowsSameNameSiblings(), is(definition.allowsSameNameSiblings()));

        // assertThat(template.getRequiredPrimaryTypeNames(), is(definition.getRequiredPrimaryTypeNames()));

    }

    private String[] emptyIfNull( String[] incoming ) {
        if (incoming != null) return incoming;
        return new String[0];
    }
}
