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

package org.modeshape.test.integration;

import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * Integration test which verifies that various external sources are correctly set-up via the JBoss AS subsystem.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class FederationIntegrationTest {

    static {
        System.setProperty("arquillian.launch", "jboss7-test");
    }

    @AfterClass
    public static void clearActiveContainer() {
       System.clearProperty("arquillian.launch");
    }

    @Resource( mappedName = "/jcr/federatedRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "federatedRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Test
    public void shouldHaveFileSystemSourceConfigured() throws Exception {
        Session defaultSession = repository.login();
        //predefined
        assertNotNull(defaultSession.getNode("/projection1"));

        FederationManager federationManager = defaultSession.getWorkspace().getFederationManager();
        federationManager.createProjection("/", "filesystem", "/", "testProjection");
        assertNotNull(defaultSession.getNode("/testProjection"));

        Session otherSession = repository.login("other");
        //predefined
        assertNotNull(otherSession.getNode("/projection1"));
    }

    @Test
    public void shouldNotAllowWritesIfConfiguredAsReadonly() throws Exception {
        Session defaultSession = repository.login();
        Node projection1 = defaultSession.getNode("/projection1");
        projection1.addNode("test", "nt:file");
        try {
            defaultSession.save();
            fail("Write operation should not be possible if connector is readonly");
        } catch (RepositoryException e) {
            //expected
        }
    }

    @Test
    public void shouldHaveGitSourceConfigured() throws Exception {
        Session session = repository.login();
        Node testRoot = session.getRootNode().addNode("repos");
        session.save();

        try {
            Workspace workspace = session.getWorkspace();

            FederationManager fedMgr = workspace.getFederationManager();
            //check that the projection is created correctly
            fedMgr.createProjection(testRoot.getPath(), "git", "/", "git-modeshape");
            Node gitNode = session.getNode("/repos/git-modeshape");
            assertNotNull(gitNode);
            assertNotNull(gitNode.getNode("branches"));
            assertNotNull(gitNode.getNode("tags"));

            //check configured queryable branches
            workspace.reindex(gitNode.getPath() + "/tree/master/.gitignore");
            Query query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/master/%'", Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());

            workspace.reindex(gitNode.getPath() + "/tree/2.x/.gitignore");
            query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/2.x/%'", Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());
        } finally {
            testRoot.remove();
            session.save();
        }
    }
}
