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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * A base class for tests that require a single shared JcrSession and JcrRepository for all test methods.
 */
public abstract class MultiUseAbstractTest extends AbstractJcrRepositoryTest {

    private static final String REPO_NAME = "testRepo";

    private static Environment environment;
    private static RepositoryConfiguration config;
    protected static JcrRepository repository;
    protected static JcrSession session;

    protected static void startRepository() throws Exception {
        startRepository(null);
    }

    protected static void startRepository( RepositoryConfiguration configuration ) throws Exception {
        environment = new TestingEnvironment();
        if (configuration != null) {
            config = new RepositoryConfiguration(configuration.getDocument(), configuration.getName(), environment);
        } else {
            config = new RepositoryConfiguration(REPO_NAME, environment);
        }
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    protected static void stopRepository() throws Exception {
        try {
            TestingUtil.killRepositories(repository);
        } finally {
            repository = null;
            config = null;
            environment.shutdown();
        }
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        startRepository();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        stopRepository();
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        // create a new session for each test ...
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        // log out of the session after each test ...
        try {
            session.logout();
        } finally {
            session = null;
        }
    }

    @Override
    public JcrRepository repository() {
        return repository;
    }

    @Override
    public JcrSession session() {
        return session;
    }

    public static org.modeshape.jcr.api.Session getSession() {
        return session;
    }

    protected static InputStream resourceStream( String name ) {
        return MultiUseAbstractTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static void registerNodeTypes( String resourceName ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Workspace workspace = session.getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        ntMgr.registerNodeTypes(stream, true);
    }

    protected static void registerNodeTypes( List<? extends NodeTypeDefinition> nodeTypes ) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        NodeTypeDefinition[] defns = nodeTypes.toArray(new NodeTypeDefinition[nodeTypes.size()]);
        ntMgr.registerNodeTypes(defns, true);
    }

    protected static void importContent( Node parent,
                                         String resourceName,
                                         int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        parent.getSession().getWorkspace().importXML(parent.getPath(), stream, uuidBehavior);
    }

    protected static void importContent( String parentPath,
                                         String resourceName,
                                         int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        session.getWorkspace().importXML(parentPath, stream, uuidBehavior);
    }
}
