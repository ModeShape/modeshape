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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.io.File;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;

/**
 * Tests that related to repeatedly starting/stopping repositories (without another repository configured in the @Before and @After
 * methods).
 * 
 * @author rhauch
 * @author hchiorean
 */
public class JcrRepositoryStartupTest extends AbstractTransactionalTest {

    @Test
    @FixFor( {"MODE-1526", "MODE-1512", "MODE-1617"} )
    public void shouldKeepPersistentDataAcrossRestart() throws Exception {
        File contentFolder = new File("target/persistent_repository/store/persistentRepository");

        final boolean testNodeShouldExist = contentFolder.exists() && contentFolder.isDirectory();
        final String newWs = "newWs_" + UUID.randomUUID().toString();
        final String newWs1 = "newWs_" + UUID.randomUUID().toString();

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                if (testNodeShouldExist) {
                    assertNotNull(session.getNode("/testNode"));
                } else {
                    session.getRootNode().addNode("testNode");
                    session.save();
                }

                // create 2 new workspaces
                session.getWorkspace().createWorkspace(newWs);
                session.getWorkspace().createWorkspace(newWs1);
                session.logout();

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {

                Session session = repository.login();
                assertNotNull(session.getNode("/testNode"));
                session.logout();

                // check the workspaces were persisted
                Session newWsSession = repository.login(newWs);
                newWsSession.getRootNode().addNode("newWsTestNode");
                newWsSession.save();
                newWsSession.logout();

                Session newWs1Session = repository.login(newWs1);
                newWs1Session.getWorkspace().deleteWorkspace(newWs1);
                newWs1Session.logout();

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session newWsSession = repository.login(newWs);
                assertNotNull(newWsSession.getNode("/newWsTestNode"));
                newWsSession.logout();

                // check a workspace was deleted
                try {
                    repository.login(newWs1);
                    fail("Workspace was not deleted from the repository");
                } catch (NoSuchWorkspaceException e) {
                    // expected
                }
                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    public void shouldNotImportInitialContentIfWorkspaceContentsChanged() throws Exception {
        // remove the ISPN local data, so we always start fresh
        FileUtil.delete("target/persistent_repository_initial_content/store");

        String repositoryConfigFile = "config/repo-config-persistent-cache-initial-content.json";
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession ws1Session = repository.login("ws1");
                // check the initial import
                AbstractJcrNode node = ws1Session.getNode("/cars");
                assertNotNull(node);
                // remove the node initially imported and add a new one
                node.remove();
                ws1Session.getRootNode().addNode("testNode");

                ws1Session.save();
                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession ws1Session = repository.login("ws1");
                try {
                    ws1Session.getNode("/cars");
                    fail("The initial content should be be re-imported if a workspace is not empty");
                } catch (PathNotFoundException e) {
                    // expected
                }
                ws1Session.getNode("/testNode");
                return null;
            }
        }, repositoryConfigFile);
    }

    protected void startRunStop( RepositoryOperation operation,
                                 String repositoryConfigFile ) throws Exception {
        URL configUrl = getClass().getClassLoader().getResource(repositoryConfigFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = null;

        try {
            repository = new JcrRepository(config);
            repository.start();

            operation.setRepository(repository).call();
        } finally {
            if (repository != null) {
                TestingUtil.killRepositoryAndContainer(repository);
            }
        }
    }

    protected abstract class RepositoryOperation implements Callable<Void> {
        protected JcrRepository repository;

        protected RepositoryOperation setRepository( JcrRepository repository ) {
            this.repository = repository;
            return this;
        }
    }
}
