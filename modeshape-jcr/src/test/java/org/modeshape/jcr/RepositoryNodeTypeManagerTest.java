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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.basic.BasicName;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RepositoryNodeTypeManagerTest extends AbstractTransactionalTest {

    private RepositoryConfiguration config;
    private JcrRepository repository;
    private ExecutionContext context;
    private RepositoryNodeTypeManager repoTypeManager;
    private JcrSession session;

    @Before
    public void beforeEach() throws Exception {
        config = new RepositoryConfiguration("repoName");
        repository = new JcrRepository(config);
        repository.start();
        context = repository.runningState().context();
        repoTypeManager = repository.nodeTypeManager();
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        try {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            repository = null;
            config = null;
        }
    }

    @Test
    public void shouldOnlyHaveOneNamespacesNode() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        Node rootNode = session.getRootNode();
        assertThat(rootNode, is(notNullValue()));

        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));
        assertThat(systemNode, is(notNullValue()));

        NodeIterator namespacesNodes = systemNode.getNodes(ModeShapeLexicon.NAMESPACES.getString(registry));
        assertThat(namespacesNodes.getSize(), is(1L));
    }

    @Test
    public void shouldOnlyHaveOneNodeTypesNode() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        Node rootNode = session.getRootNode();
        assertThat(rootNode, is(notNullValue()));

        Node systemNode = rootNode.getNode(JcrLexicon.SYSTEM.getString(registry));
        assertThat(systemNode, is(notNullValue()));

        NodeIterator nodeTypesNodes = systemNode.getNodes(JcrLexicon.NODE_TYPES.getString(registry));
        assertThat(nodeTypesNodes.getSize(), is(1L));
    }

    @Test
    public void shouldAllowMultipleSiblingsDefinitionIfOneSibling() throws Exception {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        // There's no definition for this node or for a * child node that does not allow SNS
        JcrNodeDefinition def = repoTypeManager.getNodeTypes().findChildNodeDefinition(JcrNtLexicon.NODE_TYPE,
                                                                                       null,
                                                                                       JcrLexicon.PROPERTY_DEFINITION,
                                                                                       JcrNtLexicon.PROPERTY_DEFINITION,
                                                                                       1,
                                                                                       false);

        assertThat(def, is(notNullValue()));
        assertThat(def.getName(), is(JcrLexicon.PROPERTY_DEFINITION.getString(registry)));
    }

    @Test
    @FixFor("MODE-1807")
    public void shouldAllowOverridingChildDefinitionWithSubtypeOfOriginalDefinition() throws Exception {
        InputStream cndStream = getClass().getResourceAsStream("/cnd/orc.cnd");
        assertThat(cndStream, is(notNullValue()));
        nodeTypeManager().registerNodeTypes(cndStream, true);
    }

    @Test
    @FixFor("MODE-1857")
    public void shouldAllowOverridingOfPropertyDefinitions() throws Exception {
        InputStream cnd = getClass().getClassLoader().getResourceAsStream("cnd/overridingPropertyDefinition.cnd");
        assertThat(cnd, is(notNullValue()));
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cnd, true);

        Node car = session.getRootNode().addNode("car", "car");
        car.setProperty("engine", "4CYL");
        Node cycle = session.getRootNode().addNode("cycle", "motorcycle");
        cycle.setProperty("engine", "2CYL");
        session.save();

        try {
            car.setProperty("engine", "2CYL");
            fail("Should not have allowed setting the 'engine' property on a node of type 'car' to \"2CYL\"");
        } catch (ConstraintViolationException e) {
            // expected ...
        }
        try {
            cycle.setProperty("engine", "4CYL");
            fail("Should not have allowed setting the 'engine' property on a node of type 'car' to \"2CYL\"");
        } catch (ConstraintViolationException e) {
            // expected ...
        }
    }

    @Test
    @FixFor("MODE-1857")
    public void shouldAllowOverridingOfPropertyDefinitionsWithResidualDefinitions() throws Exception {
        InputStream cnd = getClass().getClassLoader().getResourceAsStream("cnd/overridingPropertyDefinitionWithResidual.cnd");
        assertThat(cnd, is(notNullValue()));
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cnd, true);

        Node car = session.getRootNode().addNode("car", "car");
        car.setProperty("engine", "4CYL");
        Node cycle = session.getRootNode().addNode("cycle", "motorcycle");
        cycle.setProperty("engine", "2CYL");
        session.save();

        try {
            car.setProperty("engine", "2CYL");
            fail("Should not have allowed setting the 'engine' property on a node of type 'car' to \"2CYL\"");
        } catch (ConstraintViolationException e) {
            // expected ...
        }
        try {
            cycle.setProperty("engine", "4CYL");
            fail("Should not have allowed setting the 'engine' property on a node of type 'car' to \"2CYL\"");
        } catch (ConstraintViolationException e) {
            // expected ...
        }
    }

    @Test
    @FixFor( "MODE-1916" )
    public void shouldFindPublicChildNodeDefinitionsWhenBothPublicAndProtectedAreDefined() throws Exception {
        InputStream cndStream = getClass().getResourceAsStream("/cnd/protectedDefinitions.cnd");
        assertThat(cndStream, is(notNullValue()));
        nodeTypeManager().registerNodeTypes(cndStream, true);

        Name parking = new BasicName(null, "parking");
        Name level = new BasicName(null, "level");
        Collection<Name> garage = Arrays.<Name>asList(new BasicName(null, "garage"));

        JcrNodeDefinition def = repoTypeManager.getNodeTypes().findChildNodeDefinition(parking,
                                                                                       garage,
                                                                                       level,
                                                                                       level,
                                                                                       0,
                                                                                       true);

        assertNotNull(def);
        Name car = new BasicName(null, "car");
        def = repoTypeManager.getNodeTypes().findChildNodeDefinition(parking,
                                                                     garage,
                                                                     car,
                                                                     car,
                                                                     0,
                                                                     true);
        assertNotNull(def);
    }

    private JcrNodeTypeManager nodeTypeManager() throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager();
    }
}
