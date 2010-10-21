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
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.collection.Problem;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;

/**
 * Test of reading node type definitions from Jackrabbit XML files. These test cases focus on ensuring that an import of a type
 * from a Jackrabbit XML file registers the expected type rather than attempting to validate all of the type registration
 * functionality already tested in {@link TypeRegistrationTest}.
 */
public class JackrabbitXmlNodeTypeRegistrationTest {

    /** Location of XML files for this test */
    private static final String XML_LOCATION = "/xmlNodeTypeRegistration/";

    private ExecutionContext context;
    private RepositoryNodeTypeManager repoTypeManager;
    private JackrabbitXmlNodeTypeReader factory;
    @Mock
    protected JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        when(repository.getExecutionContext()).thenReturn(context);

        repoTypeManager = new RepositoryNodeTypeManager(repository, null, true, true);
        try {
            CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
            cndFactory.readBuiltInTypes();
            repoTypeManager.registerNodeTypes(cndFactory);
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }
        factory = new JackrabbitXmlNodeTypeReader(context);
    }

    @Test
    public void shouldLoadCustomNodeTypes() throws Exception {
        register(XML_LOCATION + "custom_nodetypes.xml");

        NodeType nodeType = repoTypeManager.getNodeType(name("mgnl:metaData"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mix:referenceable"));
        assertThat(nodeType.getDeclaredSupertypes()[1].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(0));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));

        JcrPropertyDefinition property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        // mgnl:content
        nodeType = repoTypeManager.getNodeType(name("mgnl:content"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mix:referenceable"));
        assertThat(nodeType.getDeclaredSupertypes()[1].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(2));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(2));

        JcrNodeDefinition childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:base"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[1];
        assertThat(childNode.getName(), is("MetaData"));
        assertThat(childNode.getDefaultPrimaryType().getName(), is("mgnl:metaData"));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("mgnl:metaData"));
        assertThat(childNode.allowsSameNameSiblings(), is(false));
        assertThat(childNode.isAutoCreated(), is(true));
        assertThat(childNode.isMandatory(), is(true));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(true));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[1];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        // mgnl:group
        nodeType = repoTypeManager.getNodeType(name("mgnl:group"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mgnl:content"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(0));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(0));

        // mgnl:folder
        nodeType = repoTypeManager.getNodeType(name("mgnl:folder"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mix:referenceable"));
        assertThat(nodeType.getDeclaredSupertypes()[1].getName(), is("nt:folder"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(2));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(2));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:base"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[1];
        assertThat(childNode.getName(), is("MetaData"));
        assertThat(childNode.getDefaultPrimaryType().getName(), is("mgnl:metaData"));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("mgnl:metaData"));
        assertThat(childNode.allowsSameNameSiblings(), is(false));
        assertThat(childNode.isAutoCreated(), is(true));
        assertThat(childNode.isMandatory(), is(true));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(true));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[1];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        // mgnl:user
        nodeType = repoTypeManager.getNodeType(name("mgnl:user"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mgnl:content"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(0));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(0));

        // mgnl:user
        nodeType = repoTypeManager.getNodeType(name("mgnl:resource"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(false));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("nt:resource"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(0));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        // mgnl:contentNode
        nodeType = repoTypeManager.getNodeType(name("mgnl:contentNode"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mix:referenceable"));
        assertThat(nodeType.getDeclaredSupertypes()[1].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(2));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(2));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:base"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[1];
        assertThat(childNode.getName(), is("MetaData"));
        assertThat(childNode.getDefaultPrimaryType().getName(), is("mgnl:metaData"));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("mgnl:metaData"));
        assertThat(childNode.allowsSameNameSiblings(), is(false));
        assertThat(childNode.isAutoCreated(), is(true));
        assertThat(childNode.isMandatory(), is(true));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(true));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[1];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        // mgnl:role
        nodeType = repoTypeManager.getNodeType(name("mgnl:role"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("mgnl:content"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(0));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(0));

        // mgnl:reserve
        nodeType = repoTypeManager.getNodeType(name("mgnl:reserve"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(1));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:base"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.COPY));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));
    }

    @Test
    public void shouldLoadMagnoliaForumTypes() throws Exception {
        register(XML_LOCATION + "magnolia_forum_nodetypes.xml");
    }

    @Test
    public void shouldLoadOwfeNodeTypes() throws Exception {
        register(XML_LOCATION + "owfe_nodetypes.xml");

        NodeType nodeType = repoTypeManager.getNodeType(name("workItem"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(1));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));

        JcrNodeDefinition childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:hierarchyNode"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.VERSION));

        JcrPropertyDefinition property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));

        nodeType = repoTypeManager.getNodeType(name("expression"));
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(false));
        assertThat(nodeType.isMixin(), is(false));
        assertThat(nodeType.isQueryable(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getPrimaryItemName(), is(nullValue()));
        assertThat(nodeType.getDeclaredSupertypes().length, is(1));
        assertThat(nodeType.getDeclaredSupertypes()[0].getName(), is("nt:hierarchyNode"));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(1));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));

        childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("*"));
        assertThat(childNode.getDefaultPrimaryType(), is(nullValue()));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("nt:hierarchyNode"));
        assertThat(childNode.allowsSameNameSiblings(), is(true));
        assertThat(childNode.isAutoCreated(), is(false));
        assertThat(childNode.isMandatory(), is(false));
        assertThat(childNode.isProtected(), is(false));
        assertThat(childNode.getOnParentVersion(), is(OnParentVersionAction.VERSION));

        property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.UNDEFINED));
        assertThat(property.getValueConstraints().length, is(0));
        assertThat(property.getDefaultValues().length, is(0));
        assertThat(property.isAutoCreated(), is(false));
        assertThat(property.isMandatory(), is(false));
        assertThat(property.isMultiple(), is(false));
        assertThat(property.isProtected(), is(false));
        assertThat(property.getOnParentVersion(), is(OnParentVersionAction.COPY));
    }

    @Test
    public void shouldLoadAllMagnoliaTypes() throws Exception {
        register(XML_LOCATION + "magnolia_forum_nodetypes.xml", XML_LOCATION + "custom_nodetypes.xml", XML_LOCATION
                                                                                                       + "owfe_nodetypes.xml");
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundOnClasspath() throws Exception {
        factory.read("this/resource/file/does/not/exist");
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsRelativeFile() throws Exception {
        factory.read("/this/resource/file/does/not/exist");
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsUrl() throws Exception {
        factory.read("file://this/resource/file/does/not/exist");
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundOnClasspath() throws Exception {
        factory.read(XML_LOCATION + "magnolia_forum_nodetypes.xml");
        assertThat(factory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundWithRelativePathOnFileSystem() throws Exception {
        factory.read("src/test/resources/" + XML_LOCATION + "magnolia_forum_nodetypes.xml");
        assertThat(factory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundWithAbsolutePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/" + XML_LOCATION + "magnolia_forum_nodetypes.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        factory.read(file.getAbsolutePath());
        assertThat(factory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromUrl() throws Exception {
        File file = new File("src/test/resources/" + XML_LOCATION + "magnolia_forum_nodetypes.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        URL url = file.toURI().toURL();
        factory.read(url.toString());
        assertThat(factory.getProblems().isEmpty(), is(true));
    }

    protected List<JcrNodeType> register( String... resourceNames ) throws Exception {
        for (String resourceName : resourceNames) {
            factory.read(resourceName);
        }
        if (factory.getProblems().hasProblems()) {
            System.out.println("Problems reading node types:" + factory);
            for (Problem problem : factory.getProblems()) {
                System.out.println(" " + problem);
            }
        }
        assertThat(factory.getProblems().hasProblems(), is(false));
        return repoTypeManager.registerNodeTypes(Arrays.asList(factory.getNodeTypeDefinitions()));
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }
}
