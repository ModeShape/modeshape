/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.cache.SiblingCounter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.basic.BasicName;

public class RepositoryNodeTypeManagerTest {

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
        JcrNodeDefinition def = repoTypeManager.getNodeTypes()
                                               .findChildNodeDefinitions(JcrNtLexicon.NODE_TYPE, null)
                                               .findBestDefinitionForChild(JcrLexicon.PROPERTY_DEFINITION,
                                                                           JcrNtLexicon.PROPERTY_DEFINITION, false,
                                                                           SiblingCounter.oneSibling());

        assertThat(def, is(notNullValue()));
        assertThat(def.getName(), is(JcrLexicon.PROPERTY_DEFINITION.getString(registry)));
    }

    @Test
    @FixFor( "MODE-1807" )
    public void shouldAllowOverridingChildDefinitionWithSubtypeOfOriginalDefinition() throws Exception {
        InputStream cndStream = getClass().getResourceAsStream("/cnd/orc.cnd");
        assertThat(cndStream, is(notNullValue()));
        nodeTypeManager().registerNodeTypes(cndStream, true);
    }

    @Test
    @FixFor( "MODE-1857" )
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
    @FixFor( "MODE-1857" )
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
        Set<Name> garage = Collections.<Name>singleton(new BasicName(null, "garage"));

        JcrNodeDefinition def = repoTypeManager.getNodeTypes().findChildNodeDefinitions(parking, garage)
                                               .findBestDefinitionForChild(level, level, true, SiblingCounter.noSiblings());

        assertNotNull(def);
        Name car = new BasicName(null, "car");
        def = repoTypeManager.getNodeTypes().findChildNodeDefinitions(parking, garage)
                             .findBestDefinitionForChild(car, car, true, SiblingCounter.noSiblings());
        assertNotNull(def);
    }

    private JcrNodeTypeManager nodeTypeManager() throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager();
    }
}
