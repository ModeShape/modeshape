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

import junit.framework.AssertionFailedError;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.naming.NamingException;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.api.JcrTools;

/**
 * A base class for tests that require a new JcrSession and JcrRepository for each test method.
 */
public abstract class SingleUseAbstractTest extends AbstractJcrRepositoryTest {

    protected static final String REPO_NAME = "testRepo";

    protected CacheContainer cm;
    protected RepositoryConfiguration config;
    protected JcrRepository repository;
    protected JcrSession session;
    protected JcrTools tools;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        Configuration c = new Configuration();
        c = c.fluent().transaction().transactionManagerLookup(new DummyTransactionManagerLookup()).build();
        cm = TestCacheManagerFactory.createCacheManager(c);

        // Configuration c = new Configuration();
        // cm = TestCacheManagerFactory.createCacheManager(c, true);
        config = createRepositoryConfiguration(REPO_NAME, cm);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
        tools = new JcrTools();
    }

    @After
    public void afterEach() throws Exception {
        try {
            TestingUtil.killRepositories(repository);
        } finally {
            repository = null;
            config = null;
            try {
                org.infinispan.test.TestingUtil.killCacheManagers(cm);
            } finally {
                cm = null;
            }
        }
    }

    @Override
    protected JcrSession session() {
        return session;
    }

    @Override
    protected JcrRepository repository() {
        return repository;
    }

    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     CacheContainer cacheContainer ) throws Exception {
        return new RepositoryConfiguration(repositoryName, cacheContainer);
    }

    protected void predefineWorkspace( String workspaceName ) {
        // Edit the configuration ...
        Editor editor = config.edit();
        EditableDocument workspaces = editor.getOrCreateDocument("workspaces");
        EditableArray predefined = workspaces.getOrCreateArray("predefined");
        predefined.addStringIfAbsent(workspaceName);

        // And apply the changes ...
        Changes changes = editor.getChanges();
        if (changes.isEmpty()) return;
        try {
            repository.apply(changes);
        } catch (NamingException e) {
            throw new AssertionFailedError("Unexpected error while predefining the \"" + workspaceName + "\" workspace:" + e.getMessage());
        } catch (IOException e) {
            throw new AssertionFailedError("Unexpected error while predefining the \"" + workspaceName + "\" workspace:" + e.getMessage());
        }
    }

    protected InputStream resourceStream( String name ) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    protected void registerNodeTypes( String resourceName ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Workspace workspace = session().getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        ntMgr.registerNodeTypes(stream, true);
    }

    protected void importContent( Node parent,
                                  String resourceName,
                                  int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        parent.getSession().getWorkspace().importXML(parent.getPath(), stream, uuidBehavior);
    }

    protected void importContent( String parentPath,
                                  String resourceName,
                                  int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        session().getWorkspace().importXML(parentPath, stream, uuidBehavior);
    }
}
