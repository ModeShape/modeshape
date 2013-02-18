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
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.value.NamespaceRegistry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class RepositoryNodeTypeManagerTest extends AbstractTransactionalTest {

    private RepositoryConfiguration config;
    private JcrRepository repository;
    private ExecutionContext context;
    private RepositoryNodeTypeManager repoTypeManager;
    private Session session;

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
    @FixFor( "MODE-1807" )
    public void shouldAllowOverridingChildDefinitionWithSubtypeOfOriginalDefinition() throws Exception {
        InputStream cndStream = getClass().getResourceAsStream("/cnd/orc.cnd");
        assertThat(cndStream, is(notNullValue()));
        nodeTypeManager().registerNodeTypes(cndStream, true);
    }


    private JcrNodeTypeManager nodeTypeManager() throws RepositoryException {
        return (JcrNodeTypeManager)session.getWorkspace().getNodeTypeManager();
    }

}
