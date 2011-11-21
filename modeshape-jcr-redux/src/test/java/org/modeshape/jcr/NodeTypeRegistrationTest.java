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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeExistsException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * Test of CND-based type definitions. These test cases focus on ensuring that an import of a type from a CND file registers the
 * expected type rather than attempting to validate all of the type registration functionality already tested in
 * {@link TypeRegistrationTest}.
 */
public class NodeTypeRegistrationTest extends SingleUseAbstractTest {

    private NodeTypeManager nodeTypeManager;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        this.nodeTypeManager = (NodeTypeManager)session.getWorkspace().getNodeTypeManager();
    }

    protected InputStream resourceAsStream( String path ) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    protected URL resourceAsUrl( String path ) {
        return getClass().getClassLoader().getResource(path);
    }

    protected NodeType assertNodeType( String name ) throws RepositoryException {
        NodeType type = nodeTypeManager.getNodeType(name);
        assertThat(type, is(notNullValue()));
        return type;
    }

    @Test
    public void shouldAccessCustomNodeTypeManagerViaCasting() throws Exception {
        NodeTypeManager nodeTypeMgr = (NodeTypeManager)session.getWorkspace().getNodeTypeManager();
        assertThat(nodeTypeMgr, is(notNullValue()));
    }

    @Test
    public void shouldAccessCustomNodeTypeManagerViaProtectedMethods() throws Exception {
        NodeTypeManager nodeTypeMgr = session.workspace().nodeTypeManager();
        assertThat(nodeTypeMgr, is(notNullValue()));
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsRelativeFile() throws Exception {
        File file = new File("/this/resource/file/does/not/exist");
        assertThat(file.exists(), is(false));
        nodeTypeManager.registerNodeTypeDefinitions(file);
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsUrl() throws Exception {
        URL url = new URL("file://this/resource/file/does/not/exist");
        nodeTypeManager.registerNodeTypeDefinitions(url);
    }

    @Test
    public void shouldLoadNodeTypesFromCndResourceFileFoundOnClasspath() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsStream("cnd/cars.cnd"));
        assertNodeType("car:Car");
    }

    @Test
    public void shouldLoadNodeTypesFromCndResourceFileFoundWithRelativePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/cnd/cars.cnd");
        if (file.exists()) {
            nodeTypeManager.registerNodeTypeDefinitions(file);
            assertNodeType("car:Car");
        }
    }

    @Test
    public void shouldLoadNodeTypesFromCndResourceFileFoundWithAbsolutePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/cnd/cars.cnd");
        if (file.exists()) {
            nodeTypeManager.registerNodeTypeDefinitions(file.getAbsoluteFile());
            assertNodeType("car:Car");
        }
    }

    @Test
    public void shouldLoadNodeTypesFromUrlToCndFile() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsUrl("cnd/cars.cnd"));
        assertNodeType("car:Car");
    }

    @Test( expected = NodeTypeExistsException.class )
    public void shouldNotAllowRedefinitionOfExistingTypesFromCndFile() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsUrl("cnd/existingType.cnd"));
        // assertNodeType("nt:folder");
    }

    @Test
    public void shouldLoadMagnoliaTypesFromCndFile() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsUrl("cnd/magnolia.cnd"));
        assertNodeType("mgnl:contentNode");
    }

    @Test
    public void shouldRegisterValidTypesFromCndFile() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsUrl("cnd/validType.cnd"));

        NodeType nodeType = assertNodeType("modetest:testType");
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isMixin(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(1));
        JcrNodeDefinition childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("modetest:namespace"));
        assertThat(childNode.getDefaultPrimaryType().getName(), is("mode:namespace"));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("mode:namespace"));
        assertThat(childNode.allowsSameNameSiblings(), is(false));
        assertThat(childNode.isMandatory(), is(false));

        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(1));
        JcrPropertyDefinition property = (JcrPropertyDefinition)nodeType.getDeclaredPropertyDefinitions()[0];
        assertThat(property.getName(), is("*"));
        assertThat(property.getRequiredType(), is(PropertyType.STRING));
        assertThat(property.getValueConstraints().length, is(3));
        assertThat(property.getValueConstraints()[0], is("foo"));
        assertThat(property.getValueConstraints()[1], is("bar"));
        assertThat(property.getValueConstraints()[2], is("baz"));
        assertThat(property.getDefaultValues().length, is(1));
        assertThat(property.getDefaultValues()[0].getString(), is("foo"));
    }

    @Test
    public void shouldLoadNodeTypesFromXmlResourceFileFoundOnClasspath() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsStream("xml/magnolia_forum_nodetypes.xml"));
        assertNodeType("mgnl:forum");
    }

    @Test
    public void shouldLoadNodeTypesFromXmlResourceFileFoundWithRelativePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/xml/magnolia_forum_nodetypes.xml");
        if (file.exists()) {
            nodeTypeManager.registerNodeTypeDefinitions(file);
            assertNodeType("mgnl:forum");
        }
    }

    @Test
    public void shouldLoadNodeTypesFromXmlResourceFileFoundWithAbsolutePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/xml/magnolia_forum_nodetypes.xml");
        if (file.exists()) {
            nodeTypeManager.registerNodeTypeDefinitions(file.getAbsoluteFile());
            assertNodeType("mgnl:forum");
        }
    }

    @Test
    public void shouldLoadNodeTypesFromUrl() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsUrl("xml/magnolia_forum_nodetypes.xml"));
        assertNodeType("mgnl:forum");
    }

    @Test
    public void shouldLoadMagnoliaNodeTypesFromXml() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsStream("xml/magnolia_forum_nodetypes.xml"));
        assertNodeType("mgnl:forum");
    }

    @Test
    public void shouldLoadOwfeNodeTypesFromXml() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsStream("xml/owfe_nodetypes.xml"));
        assertNodeType("expression");
    }

    @Test
    public void shouldLoadCustomNodeTypesFromXml() throws Exception {
        nodeTypeManager.registerNodeTypeDefinitions(resourceAsStream("xml/custom_nodetypes.xml"));
        assertNodeType("mgnl:reserve");
    }

}
