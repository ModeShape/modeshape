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
import java.io.IOException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

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
    private JcrNodeTypeSource nodeTypes;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        repoTypeManager = new RepositoryNodeTypeManager(context);
        try {
            this.repoTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {"/org/jboss/dna/jcr/jsr_170_builtins.cnd",
                "/org/jboss/dna/jcr/dna_builtins.cnd"}));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowRedefinitionOfExistingType() throws Exception {
        nodeTypes = new CndNodeTypeSource(CND_LOCATION + "existingType.cnd");

        repoTypeManager.registerNodeTypes(nodeTypes);
    }

    @Test
    public void shouldRegisterValidTypes() throws Exception {
        nodeTypes = new CndNodeTypeSource(CND_LOCATION + "validType.cnd");

        repoTypeManager.registerNodeTypes(nodeTypes);
        Name testNodeName = context.getValueFactories().getNameFactory().create(TestLexicon.Namespace.URI, "testType");

        NodeType nodeType = repoTypeManager.getNodeType(testNodeName);
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isMixin(), is(true));
        assertThat(nodeType.hasOrderableChildNodes(), is(true));
        assertThat(nodeType.getDeclaredSupertypes().length, is(2));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(1));
        JcrNodeDefinition childNode = (JcrNodeDefinition)nodeType.getDeclaredChildNodeDefinitions()[0];
        assertThat(childNode.getName(), is("dnatest:namespace"));
        assertThat(childNode.getDefaultPrimaryType().getName(), is("dna:namespace"));
        assertThat(childNode.getRequiredPrimaryTypes().length, is(1));
        assertThat(childNode.getRequiredPrimaryTypes()[0].getName(), is("dna:namespace"));
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
