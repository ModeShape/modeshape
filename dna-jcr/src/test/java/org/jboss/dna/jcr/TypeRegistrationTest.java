/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class TypeRegistrationTest {

    private static final String TEST_TYPE_NAME = "dna:testNode";
    private static final String TEST_TYPE_NAME2 = "dna:testNode2";
    private static final String TEST_PROPERTY_NAME = "dna:testProperty";
    private static final String TEST_CHILD_NODE_NAME = "dna:testChildNode";

    private ExecutionContext context;
    private RepositoryNodeTypeManager repoTypeManager;
    private JcrNodeType ntTemplate;
    private NameFactory nameFactory;
    private JcrNodeType base;
    private JcrNodeType referenceable;
    private JcrNodeType unstructured;
    private JcrNodeType root;
    private JcrNodeType hierarchyNode;
    private JcrNodeType file;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        nameFactory = context.getValueFactories().getNameFactory();

        JcrNodeTypeSource source = null;
        source = new JcrBuiltinNodeTypeSource(context, source);
        source = new DnaBuiltinNodeTypeSource(this.context, source);
        repoTypeManager = new RepositoryNodeTypeManager(context, source);

        base = repoTypeManager.getNodeType(JcrNtLexicon.BASE);
        referenceable = repoTypeManager.getNodeType(JcrMixLexicon.REFERENCEABLE);
        unstructured = repoTypeManager.getNodeType(JcrNtLexicon.UNSTRUCTURED);
        root = repoTypeManager.getNodeType(DnaLexicon.ROOT);
        hierarchyNode = repoTypeManager.getNodeType(JcrNtLexicon.HIERARCHY_NODE);
        file = repoTypeManager.getNodeType(JcrNtLexicon.FILE);
    }

    private Name nameFor( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    private JcrNodeTypeSource sourceFor( final JcrNodeType nodeType ) {
        return new AbstractJcrNodeTypeSource() {
            @Override
            public List<JcrNodeType> getDeclaredNodeTypes() {
                return Collections.singletonList(nodeType);
            }
        };
    }

    private JcrNodeTypeSource sourceFor( final JcrNodeType... nodeTypes ) {
        return new AbstractJcrNodeTypeSource() {
            @Override
            public List<JcrNodeType> getDeclaredNodeTypes() {
                return Arrays.asList(nodeTypes);
            }
        };
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullDefinition() throws Exception {
        repoTypeManager.registerNodeTypes(null);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowTemplateWithNullContext() throws Exception {
        repoTypeManager.registerNodeTypes(sourceFor(new JcrNodeType(null, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                                    nameFor(TEST_TYPE_NAME), Arrays.asList(new JcrNodeType[] {}),
                                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES,
                                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowNodeTypeWithNoName() throws Exception {
        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, null,
                                     Arrays.asList(new JcrNodeType[] {}), AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES, AbstractJcrNodeTypeSource.NO_PROPERTIES,
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);
        repoTypeManager.registerNodeTypes(sourceFor(ntTemplate));
    }

    @Test
    public void shouldAllowNewDefinitionWithNoChildNodesOrProperties() throws Exception {
        Name testTypeName = nameFactory.create(TEST_TYPE_NAME);
        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, testTypeName,
                                     Arrays.asList(new JcrNodeType[] {}), AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES, AbstractJcrNodeTypeSource.NO_PROPERTIES,
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> testNodeTypes = repoTypeManager.registerNodeTypes(sourceFor(ntTemplate));
        JcrNodeType testNodeType = testNodeTypes.get(0);

        assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
        JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
        assertThat(nodeTypeFromRepo, is(notNullValue()));
        assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));
    }

    // @Test( expected = RepositoryException.class )
    // public void shouldNotAllowModificationIfAllowUpdatesIsFalse() throws Exception {
    // ntTemplate.setName("nt:base");
    // repoTypeManager.registerNodeType(ntTemplate, false);
    // }

    // @Test( expected = RepositoryException.class )
    // public void shouldNotAllowRedefinitionOfNewTypeIfAllowUpdatesIsFalse() throws Exception {
    // Name testTypeName = nameFactory.create(TEST_TYPE_NAME);
    // ntTemplate.setName(TEST_TYPE_NAME);
    // ntTemplate.setDeclaredSupertypeNames(new String[] {"nt:base"});
    //
    // JcrNodeType testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);
    //
    // assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
    // JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
    // assertThat(nodeTypeFromRepo, is(notNullValue()));
    // assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));
    //
    // testNodeType = repoTypeManager.registerNodeType(ntTemplate, false);
    // }

    @Test
    public void shouldAllowDefinitionWithExistingSupertypes() throws Exception {
        Name testTypeName = nameFactory.create(TEST_TYPE_NAME);

        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, testTypeName,
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> testNodeTypes = repoTypeManager.registerNodeTypes(sourceFor(ntTemplate));
        JcrNodeType testNodeType = testNodeTypes.get(0);

        assertThat(testNodeType.getName(), is(TEST_TYPE_NAME));
        JcrNodeType nodeTypeFromRepo = repoTypeManager.getNodeType(testTypeName);
        assertThat(nodeTypeFromRepo, is(notNullValue()));
        assertThat(nodeTypeFromRepo.getName(), is(TEST_TYPE_NAME));

    }

    @Test
    public void shouldAllowDefinitionWithSupertypesFromTypesRegisteredInSameCall() throws Exception {
        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType ntTemplate2 = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                  nameFor(TEST_TYPE_NAME2), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                  AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                  AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                  AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                  AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, ntTemplate2});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, ntTemplate2)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowDefinitionWithSupertypesFromTypesRegisteredInSameCallInWrongOrder() throws Exception {
        // Try to register the supertype AFTER the class that registers it
        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType ntTemplate2 = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                  nameFor(TEST_TYPE_NAME2), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                  AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                  AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                  AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                  AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate2, ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate2, ntTemplate)));
    }

    @Test
    public void shouldAllowDefinitionWithAProperty() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          null,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.LONG,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false)}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));
    }

    @Test
    public void shouldAllowDefinitionWithProperties() throws Exception {

        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {
                                         new JcrPropertyDefinition(context, null, null,
                                                                   OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                                   false, AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                   PropertyType.LONG, AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                   false),
                                         new JcrPropertyDefinition(context, null, null,
                                                                   OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                                   false, AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                   PropertyType.STRING, AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                   true),
                                         new JcrPropertyDefinition(context, null, nameFor(TEST_PROPERTY_NAME),
                                                                   OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                                   false, AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                   PropertyType.STRING, AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                   false)}), AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowAutocreatedPropertyWithNoDefault() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          nameFor(TEST_PROPERTY_NAME),
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          true,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.LONG,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowAutocreatedResidualProperty() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          null,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          true,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.UNDEFINED,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    // @Test
    // public void shouldAllowAutocreatedNamedPropertyWithDefault() throws Exception {
    // ntTemplate.setName(TEST_TYPE_NAME);
    // ntTemplate.setDeclaredSupertypeNames(new String[] {"nt:base", "mix:referenceable"});
    //
    // JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
    // prop.setName(TEST_PROPERTY_NAME);
    // prop.setRequiredType(PropertyType.UNDEFINED);
    // prop.setAutoCreated(true);
    // prop.setDefaultValues(new String[] {"<default>"});
    // ntTemplate.getPropertyDefinitionTemplates().add(prop);
    //
    // List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
    // compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    // }
    //
    // @Test( expected = RepositoryException.class )
    // public void shouldNotAllowSingleValuedPropertyWithMultipleDefaults() throws Exception {
    // ntTemplate.setName(TEST_TYPE_NAME);
    // ntTemplate.setDeclaredSupertypeNames(new String[] {"nt:base", "mix:referenceable"});
    //
    // JcrPropertyDefinitionTemplate prop = new JcrPropertyDefinitionTemplate(this.context);
    // prop.setName(TEST_PROPERTY_NAME);
    // prop.setRequiredType(PropertyType.UNDEFINED);
    // prop.setAutoCreated(true);
    // prop.setDefaultValues(new String[] {"<default>", "too many values"});
    // ntTemplate.getPropertyDefinitionTemplates().add(prop);
    //
    // List<NodeTypeDefinition> templates = Arrays.asList(new NodeTypeDefinition[] {ntTemplate});
    // compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(templates, false));
    // }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowMandatoryResidualProperty() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          null,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          true,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.UNDEFINED,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test
    public void shouldAllowTypeWithChildNode() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  nameFor(TEST_CHILD_NODE_NAME),
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, false, false, true,
                                                                                                  JcrNtLexicon.BASE,
                                                                                                  new JcrNodeType[] {base})}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test
    public void shouldAllowTypeWithMultipleChildNodes() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {
                                         new JcrNodeDefinition(context, null, nameFor(TEST_CHILD_NODE_NAME),
                                                               OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                               false, true, JcrNtLexicon.BASE, new JcrNodeType[] {base}),
                                         new JcrNodeDefinition(context, null, nameFor(TEST_CHILD_NODE_NAME),
                                                               OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                               false, false, JcrNtLexicon.BASE, new JcrNodeType[] {unstructured}),
                                         new JcrNodeDefinition(context, null, nameFor(TEST_CHILD_NODE_NAME + "2"),
                                                               OnParentVersionBehavior.VERSION.getJcrValue(), false, false,
                                                               false, true, JcrNtLexicon.BASE, new JcrNodeType[] {base}),

                                     }), AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowAutocreatedChildNodeWithNoDefaultPrimaryType() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  nameFor(TEST_CHILD_NODE_NAME),
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  true, false, false, false,
                                                                                                  null, new JcrNodeType[] {base}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowMandatoryResidualChildNode() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  null,
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, true, false, false,
                                                                                                  JcrNtLexicon.BASE,
                                                                                                  new JcrNodeType[] {base}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingProtectedProperty() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          JcrLexicon.PRIMARY_TYPE,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.NAME,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingProtectedChildNode() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {root, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  JcrLexicon.SYSTEM,
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, false, false, false,
                                                                                                  JcrNtLexicon.BASE,
                                                                                                  new JcrNodeType[] {base}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingMandatoryChildNodeWithOptionalChildNode() throws Exception {
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {file}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  JcrLexicon.CONTENT,
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, false, false, false,
                                                                                                  JcrNtLexicon.BASE,
                                                                                                  new JcrNodeType[] {base}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate)));

    }

    @Test
    public void shouldAllowOverridingPropertyFromCommonAncestor() throws Exception {
        /*
        * testNode declares prop testProperty
        * testNodeB extends testNode
        * testNodeC extends testNode
        * testNodeD extends testNodeB and testNodeC and overrides testProperty --> LEGAL
        */

        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          nameFor(TEST_PROPERTY_NAME),
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.UNDEFINED,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeCTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "C"), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeDTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "D"),
                                                    Arrays.asList(new JcrNodeType[] {nodeBTemplate, nodeCTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.STRING,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate, nodeCTemplate, nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate,
                                                                                           nodeBTemplate,
                                                                                           nodeCTemplate,
                                                                                           nodeDTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingPropertyFromDifferentAncestors() throws Exception {
        /*
        * testNode
        * testNodeB extends testNode and declares prop testProperty
        * testNodeC extends testNode and declares prop testProperty
        * testNodeD extends testNodeB and testNodeC and overrides testProperty --> ILLEGAL
        */

        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.UNDEFINED,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeCTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "C"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.UNDEFINED,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeDTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "D"),
                                                    Arrays.asList(new JcrNodeType[] {nodeBTemplate, nodeCTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.STRING,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate, nodeCTemplate, nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate,
                                                                                           nodeBTemplate,
                                                                                           nodeCTemplate,
                                                                                           nodeDTemplate)));
    }

    @Test
    public void shouldAllowOverridingChildNodeFromCommonAncestor() throws Exception {
        /*
        * testNode declares node testChildNode
        * testNodeB extends testNode
        * testNodeC extends testNode
        * testNodeD extends testNodeB and testNodeC and overrides testChildNode --> LEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  null,
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, false, false, false,
                                                                                                  JcrNtLexicon.BASE,
                                                                                                  new JcrNodeType[] {base}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeCTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "C"), Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeDTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "D"),
                                                    Arrays.asList(new JcrNodeType[] {nodeBTemplate, nodeCTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 JcrLexicon.SYSTEM,
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 JcrNtLexicon.UNSTRUCTURED,
                                                                                                                 new JcrNodeType[] {unstructured}),}),
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,

                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate, nodeCTemplate, nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate,
                                                                                           nodeBTemplate,
                                                                                           nodeCTemplate,
                                                                                           nodeDTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingChildNodeFromDifferentAncestors() throws Exception {
        /*
        * testNode
        * testNodeB extends testNode and declares node testChildNode
        * testNodeC extends testNode and declares node testChildNode
        * testNodeD extends testNodeB and testNodeC and overrides testChildNode --> ILLEGAL
        */
        ntTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER, nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME, AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 JcrNtLexicon.BASE,
                                                                                                                 new JcrNodeType[] {base}),}),

                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeCTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "C"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 JcrNtLexicon.BASE,
                                                                                                                 new JcrNodeType[] {base}),}),

                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeDTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "D"),
                                                    Arrays.asList(new JcrNodeType[] {nodeBTemplate, nodeCTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 JcrLexicon.SYSTEM,
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 JcrNtLexicon.UNSTRUCTURED,
                                                                                                                 new JcrNodeType[] {unstructured}),}),
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,

                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate, nodeCTemplate, nodeDTemplate});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate,
                                                                                           nodeBTemplate,
                                                                                           nodeCTemplate,
                                                                                           nodeDTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowExtendingChildNodeIfSnsChanges() throws Exception {
        /*
        * testNode declares node testChildNode with no SNS
        * testNodeB extends testNode with node testChildNode with SNS -> ILLEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  nameFor(TEST_CHILD_NODE_NAME),
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false, false, false, false,
                                                                                                  null, new JcrNodeType[] {root}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 nameFor(TEST_CHILD_NODE_NAME),
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 true,
                                                                                                                 JcrNtLexicon.UNSTRUCTURED,
                                                                                                                 new JcrNodeType[] {unstructured}),}),

                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, nodeBTemplate)));

    }

    @Test
    public void shouldAllowExtendingPropertyIfMultipleChanges() throws Exception {
        /*
        * testNode declares SV property testProperty
        * testNodeB extends testNode with MV property testProperty with incompatible type -> LEGAL
        * testNodeC extends testNode, testNodeB -> LEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          nameFor(TEST_PROPERTY_NAME),
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.LONG,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.DATE,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         true),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeCTemplate = new JcrNodeType(context, AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "C"), Arrays.asList(new JcrNodeType[] {ntTemplate,
                                                        nodeBTemplate}), AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate, nodeCTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate,
                                                                                           nodeBTemplate,
                                                                                           nodeCTemplate)));
    }

    @Test
    public void shouldAllowOverridingPropertyIfTypeNarrows() throws Exception {
        /*
        * testNode declares SV property testProperty of type UNDEFINED
        * testNodeB extends testNode with SV property testProperty of type STRING -> LEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          nameFor(TEST_PROPERTY_NAME),
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.STRING,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.LONG,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, nodeBTemplate)));
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingPropertyIfTypeDoesNotNarrow() throws Exception {
        /*
        * testNode declares SV property testProperty of type DATE
        * testNodeB extends testNode with SV property testProperty of type NAME -> ILLEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          nameFor(TEST_PROPERTY_NAME),
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                          PropertyType.DATE,
                                                                                                          AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                          false),}),
                                     AbstractJcrNodeTypeSource.NOT_MIXIN, AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    AbstractJcrNodeTypeSource.NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         nameFor(TEST_PROPERTY_NAME),
                                                                                                                         OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         AbstractJcrNodeTypeSource.NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.NAME,
                                                                                                                         AbstractJcrNodeTypeSource.NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, nodeBTemplate)));
    }

    @Test
    public void shouldAllowOverridingChildNodeIfRequiredTypesNarrow() throws Exception {
        /*
        * testNode declares No-SNS childNode testChildNode requiring type nt:hierarchy
        * testNodeB extends testNode with No-SNS childNode testChildNode requiring type nt:file -> LEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  nameFor(TEST_CHILD_NODE_NAME),
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false,
                                                                                                  false,
                                                                                                  false,
                                                                                                  false,
                                                                                                  null,
                                                                                                  new JcrNodeType[] {hierarchyNode}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 nameFor(TEST_CHILD_NODE_NAME),
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 true,
                                                                                                                 null,
                                                                                                                 new JcrNodeType[] {file}),}),

                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, nodeBTemplate)));

    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowOverridingChildNodeIfRequiredTypesDoNotNarrow() throws Exception {
        /*
        * testNode declares No-SNS childNode testChildNode requiring type nt:hierarchy
        * testNodeB extends testNode with No-SNS childNode testChildNode requiring type nt:base -> ILLEGAL
        */
        ntTemplate = new JcrNodeType(
                                     context,
                                     AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                     nameFor(TEST_TYPE_NAME),
                                     Arrays.asList(new JcrNodeType[] {base, referenceable}),
                                     AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                     Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                  context,
                                                                                                  null,
                                                                                                  nameFor(TEST_CHILD_NODE_NAME),
                                                                                                  OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                  false,
                                                                                                  false,
                                                                                                  false,
                                                                                                  false,
                                                                                                  null,
                                                                                                  new JcrNodeType[] {hierarchyNode}),}),
                                     AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                     AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeBTemplate = new JcrNodeType(
                                                    context,
                                                    AbstractJcrNodeTypeSource.NO_NODE_TYPE_MANAGER,
                                                    nameFor(TEST_TYPE_NAME + "B"),
                                                    Arrays.asList(new JcrNodeType[] {ntTemplate}),
                                                    AbstractJcrNodeTypeSource.NO_PRIMARY_ITEM_NAME,
                                                    Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                 context,
                                                                                                                 null,
                                                                                                                 nameFor(TEST_CHILD_NODE_NAME),
                                                                                                                 OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 true,
                                                                                                                 null,
                                                                                                                 new JcrNodeType[] {base}),}),

                                                    AbstractJcrNodeTypeSource.NO_PROPERTIES, AbstractJcrNodeTypeSource.NOT_MIXIN,
                                                    AbstractJcrNodeTypeSource.UNORDERABLE_CHILD_NODES);

        List<JcrNodeType> templates = Arrays.asList(new JcrNodeType[] {ntTemplate, nodeBTemplate,});
        compareTemplatesToNodeTypes(templates, repoTypeManager.registerNodeTypes(sourceFor(ntTemplate, nodeBTemplate)));

    }

    private void compareTemplatesToNodeTypes( List<JcrNodeType> templates,
                                              List<JcrNodeType> nodeTypes ) {
        assertThat(templates.size(), is(nodeTypes.size()));

        for (int i = 0; i < nodeTypes.size(); i++) {
            JcrNodeType jntt = templates.get(i);
            compareTemplateToNodeType(jntt, null);
            compareTemplateToNodeType(jntt, nodeTypes.get(i));
        }
    }

    private void compareTemplateToNodeType( JcrNodeType template,
                                            JcrNodeType nodeType ) {
        Name nodeTypeName = nameFactory.create(template.getName());
        if (nodeType == null) {
            nodeType = repoTypeManager.getNodeType(nodeTypeName);
            assertThat(nodeType.nodeTypeManager(), is(notNullValue()));
        }

        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.getName(), is(template.getName()));

        assertThat(nodeType.getDeclaredSupertypes().length, is(template.getDeclaredSupertypes().length));
        for (int i = 0; i < template.getDeclaredSupertypes().length; i++) {
            assertThat(template.getDeclaredSupertypes()[i].getName(), is(nodeType.getDeclaredSupertypes()[i].getName()));
        }
        assertThat(template.isMixin(), is(nodeType.isMixin()));
        assertThat(template.hasOrderableChildNodes(), is(nodeType.hasOrderableChildNodes()));

        PropertyDefinition[] propertyDefs = nodeType.getDeclaredPropertyDefinitions();
        List<JcrPropertyDefinition> propertyTemplates = Arrays.asList(template.getDeclaredPropertyDefinitions());

        assertThat(propertyDefs.length, is(propertyTemplates.size()));
        for (JcrPropertyDefinition pt : propertyTemplates) {
            JcrPropertyDefinition propertyTemplate = pt;

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
        List<JcrNodeDefinition> childNodeTemplates = Arrays.asList(template.getDeclaredChildNodeDefinitions());

        assertThat(childNodeDefs.length, is(childNodeTemplates.size()));
        for (JcrNodeDefinition nt : childNodeTemplates) {
            JcrNodeDefinition childNodeTemplate = nt;

            NodeDefinition matchingDefinition = null;
            for (int i = 0; i < childNodeDefs.length; i++) {
                JcrNodeDefinition nd = (JcrNodeDefinition)childNodeDefs[i];

                String ntName = childNodeTemplate.getName() == null ? JcrNodeType.RESIDUAL_ITEM_NAME : childNodeTemplate.getName();
                if (nd.getName().equals(ntName) && nd.allowsSameNameSiblings() == childNodeTemplate.allowsSameNameSiblings()) {

                    boolean matchesOnRequiredTypes = childNodeTemplate.getRequiredPrimaryTypeNames()
                                                                      .equals(nd.getRequiredPrimaryTypeNames());

                    if (matchesOnRequiredTypes) {
                        matchingDefinition = nd;
                        break;
                    }
                }
            }

            compareNodeTemplateToNodeDefinition(childNodeTemplate, (JcrNodeDefinition)matchingDefinition);
        }

    }

    private void comparePropertyTemplateToPropertyDefinition( JcrPropertyDefinition template,
                                                              JcrPropertyDefinition definition ) {

        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(template.getValueConstraints(), is(definition.getValueConstraints()));
        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.getRequiredType(), is(definition.getRequiredType()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isMultiple(), is(definition.isMultiple()));
        assertThat(template.isProtected(), is(definition.isProtected()));
    }

    private void compareNodeTemplateToNodeDefinition( JcrNodeDefinition template,
                                                      JcrNodeDefinition definition ) {
        assertThat(definition, is(notNullValue()));
        assertThat(definition.getDeclaringNodeType(), is(notNullValue()));
        // Had to match on name to even get to the definition
        // assertThat(template.getName(), is(definition.getName()));

        assertThat(template.getOnParentVersion(), is(definition.getOnParentVersion()));
        assertThat(template.isAutoCreated(), is(definition.isAutoCreated()));
        assertThat(template.isMandatory(), is(definition.isMandatory()));
        assertThat(template.isProtected(), is(definition.isProtected()));

        assertThat(template.with(repoTypeManager).getDefaultPrimaryType(), is(definition.getDefaultPrimaryType()));
        assertThat(template.allowsSameNameSiblings(), is(definition.allowsSameNameSiblings()));

        // assertThat(template.getRequiredPrimaryTypeNames(), is(definition.getRequiredPrimaryTypeNames()));

    }
}
