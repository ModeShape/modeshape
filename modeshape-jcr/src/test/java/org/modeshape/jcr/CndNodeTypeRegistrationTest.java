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
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeExistsException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;

/**
 * Test of CND-based type definitions. These test cases focus on ensuring that an import of a type from a CND file registers the
 * expected type rather than attempting to validate all of the type registration functionality already tested in
 * {@link TypeRegistrationTest}.
 */
public class CndNodeTypeRegistrationTest {

    /** Location of CND files for this test */
    private static final String CND_LOCATION = "/cndNodeTypeRegistration/";

    private ExecutionContext context;
    private RepositoryNodeTypeManager repoTypeManager;
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
            CndNodeTypeReader cndReader = new CndNodeTypeReader(context);
            cndReader.readBuiltInTypes();
            repoTypeManager.registerNodeTypes(cndReader);
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundOnClasspath() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("this/resource/file/does/not/exist");
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsRelativeFile() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("/this/resource/file/does/not/exist");
    }

    @Test( expected = IOException.class )
    public void shouldFailIfResourceFileCouldNotBeFoundAsUrl() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("file://this/resource/file/does/not/exist");
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundOnClasspath() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("cars.cnd");
        assertThat(cndFactory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundWithRelativePathOnFileSystem() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("src/test/resources/cars.cnd");
        assertThat(cndFactory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromResourceFileFoundWithAbsolutePathOnFileSystem() throws Exception {
        File file = new File("src/test/resources/cars.cnd");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read(file.getAbsolutePath());
        assertThat(cndFactory.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadNodeTypesFromUrl() throws Exception {
        File file = new File("src/test/resources/cars.cnd");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        URL url = file.toURI().toURL();
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read(url.toString());
        assertThat(cndFactory.getProblems().isEmpty(), is(true));
    }

    @Test( expected = NodeTypeExistsException.class )
    public void shouldNotAllowRedefinitionOfExistingTypes() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read(CND_LOCATION + "existingType.cnd");
        repoTypeManager.registerNodeTypes(cndFactory);
    }

    @Test
    public void shouldLoadMagnoliaTypes() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read("/magnolia.cnd");
        repoTypeManager.registerNodeTypes(cndFactory);
    }

    @Ignore
    @Test
    public void shouldRegisterValidTypes() throws Exception {
        CndNodeTypeReader cndFactory = new CndNodeTypeReader(context);
        cndFactory.read(CND_LOCATION + "validType.cnd");
        repoTypeManager.registerNodeTypes(cndFactory);

        Name testNodeName = context.getValueFactories().getNameFactory().create(TestLexicon.Namespace.URI, "testType");

        NodeType nodeType = repoTypeManager.getNodeType(testNodeName);
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
}
