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
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class JcrShareableNodesTest {

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;
    private boolean print;

    @Before
    public void beforeEach() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("source").usingClass(InMemoryRepositorySource.class).setDescription("The content store");
        configuration.repository("repo")
                     .setSource("source")
                     .registerNamespace("stest", "http://www.modeshape.org/test/shareablenodes/1.0")
                     .addNodeTypes(resourceUrl("shareableNodes.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                ModeShapeRoles.READONLY + "," + ModeShapeRoles.READWRITE + "," + ModeShapeRoles.ADMIN)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        engine = configuration.build();
        engine.start();

        // Start the repository ...
        repository = engine.getRepository("repo");

        // Log into a session ...
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
                repository = null;
                configuration = null;
            }
        }
    }

    @BeforeClass
    public static void beforeAll() {
        // Initialize the JAAS configuration to allow for an admin login later
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    @Test
    public void shouldStartUp() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldImportCarsSystemViewWithCreateNewBehaviorWhenImportedContentDoesNotContainJcrRoot() throws Exception {
        // Create some simple nodes ...
        Node root = session.getRootNode();
        Node a1 = root.addNode("a", "stest:A");
        Node b1 = a1.addNode("b", "stest:B");
        Node c1 = b1.addNode("c", "stest:C");
        session.save();

        assertNode("/a");
        assertNode("/a/b");
        assertNode("/a/b/c");
        assertNoNode("/a/c");

        // Now clone c1 ...
        Workspace workspace = session.getWorkspace();
        VersionManager versionManager = workspace.getVersionManager();
        versionManager.checkout(a1.getPath());
        workspace.clone(workspace.getName(), c1.getPath(), "/a/c", false);
        versionManager.checkin(a1.getPath());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------------------------------------------------

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected String asString( Object value ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(value);
    }

    protected Node assertNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        String relativePath = relativePath(path);
        Node root = session.getRootNode();
        if (relativePath.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(root, is(notNullValue()));
            return session.getNode(path);
        }
        if (print && !root.hasNode(relativePath)) {
            Node parent = root;
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }
        assertThat(root.hasNode(relativePath), is(true));
        return session.getNode(path);
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat(session.getRootNode().hasNode(relativePath(path)), is(false));
    }

    protected boolean hasMixin( Node node,
                                String mixinNodeType ) throws RepositoryException {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.getName().equals(mixinNodeType)) return true;
        }
        return false;
    }

    protected void print() throws RepositoryException {
        print(session.getRootNode(), true);
    }

    protected void print( String path ) throws RepositoryException {
        Node node = session.getRootNode().getNode(relativePath(path));
        print(node, true);
    }

    protected void print( Node node,
                          boolean includeSystem ) throws RepositoryException {
        if (print) {
            if (!includeSystem && node.getPath().equals("/jcr:system")) return;
            if (node.getDepth() != 0) {
                int snsIndex = node.getIndex();
                String segment = node.getName() + (snsIndex > 1 ? ("[" + snsIndex + "]") : "");
                System.out.println(StringUtil.createString(' ', 2 * node.getDepth()) + '/' + segment);
            }
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                print(children.nextNode(), includeSystem);
            }
        }
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

}
