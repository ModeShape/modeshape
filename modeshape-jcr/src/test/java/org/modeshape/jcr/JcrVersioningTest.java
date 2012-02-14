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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;

/**
 * Unit test for versioning behaviour (see JSR_285#15)
 *
 * @author Horia Chiorean
 */
public class JcrVersioningTest {

    private Session session;
    private VersionManager versionManager;

    @Before
    public void beforeEach() throws RepositoryException {
        String repositorySource = "store";
        String repositoryName = "r1";

        JcrConfiguration config = createInMemoryConfig(repositorySource, repositoryName);
        JcrEngine engine = startEngine(config);
        createTestSession(repositoryName, engine);

        versionManager = session.getWorkspace().getVersionManager();
    }

    private void createTestSession( String repositoryName,
                                    JcrEngine engine ) throws RepositoryException {
        JcrRepository repository = engine.getRepository(repositoryName);
        session = repository.login();
        assertNotNull(session);
    }

    private JcrEngine startEngine( JcrConfiguration config ) {
        JcrEngine engine = config.build();
        engine.start();
        assertThat(engine.getProblems().hasErrors(), is(false));
        return engine;
    }

    private JcrConfiguration createInMemoryConfig( String repositorySource,
                                                   String repositoryName ) {
        JcrConfiguration config = new JcrConfiguration();
        config.repositorySource("store")
              .usingClass(InMemoryRepositorySource.class)
              .setRetryLimit(100)
              .setProperty("defaultWorkspaceName", "ws1");
        config.repository(repositoryName).setSource(repositorySource).setOption(JcrRepository.Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        config.save();
        return config;
    }

    @Test
    @FixFor("MODE-1302")
    public void shouldHaveVersionHistoryWhenRefreshIsCalled() throws Exception {
        Node outerNode = session.getRootNode().addNode("outerFolder");
        Node innerNode = outerNode.addNode("innerFolder");
        Node fileNode  = innerNode.addNode("testFile.dat");
        fileNode.setProperty("jcr:mimeType", "text/plain");
        fileNode.setProperty("jcr:data", "Original content");
        session.save();

        assertFalse(hasVersionHistory(fileNode));
        fileNode.addMixin("mix:versionable");
        session.refresh(true);
        assertTrue(hasVersionHistory(fileNode));
    }

    private boolean hasVersionHistory( Node node ) throws RepositoryException {
        try {
            VersionHistory history = versionManager.getVersionHistory(node.getPath());
            assertNotNull(history);
            return true;
        } catch (UnsupportedRepositoryOperationException e) {
            return false;
        }
    }
}
