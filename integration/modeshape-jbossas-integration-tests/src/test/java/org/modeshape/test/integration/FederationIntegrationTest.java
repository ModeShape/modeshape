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
package org.modeshape.test.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.connector.meta.jdbc.JdbcMetadataLexicon;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;

/**
 * Integration test which verifies that various external sources are correctly set-up via the JBoss AS subsystem.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class FederationIntegrationTest {

    @Resource( mappedName = "/jcr/federatedRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "federatedRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF")).addClass(JdbcMetadataLexicon.class);
        return archive;
    }

    @Test
    public void shouldHaveFileSystemSourceConfigured() throws Exception {
        Session defaultSession = repository.login();
        // predefined
        assertNotNull(defaultSession.getNode("/projection1"));

        FederationManager federationManager = defaultSession.getWorkspace().getFederationManager();
        federationManager.createProjection("/", "filesystem", "/", "testProjection");
        assertNotNull(defaultSession.getNode("/testProjection"));

        Session otherSession = repository.login("other");
        // predefined
        assertNotNull(otherSession.getNode("/projection1"));
    }

    @Test
    public void shouldNotAllowWritesIfConfiguredAsReadonly() throws Exception {
        Session defaultSession = repository.login();
        Node projection1 = defaultSession.getNode("/projection1");
        try {
            projection1.addNode("test", "nt:file");
            defaultSession.save();
            fail("Write operation should not be possible if connector is readonly");
        } catch (RepositoryException e) {
            // expected
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
            // check that the projection is created correctly
            fedMgr.createProjection(testRoot.getPath(), "git", "/", "git-modeshape");
            Node gitNode = session.getNode("/repos/git-modeshape");
            assertNotNull(gitNode);
            assertNotNull(gitNode.getNode("branches"));
            assertNotNull(gitNode.getNode("tags"));

            // check configured queryable branches
            workspace.reindex(gitNode.getPath() + "/tree/master/.gitignore");
            Query query = workspace.getQueryManager()
                                   .createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/master/%'", Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());

            workspace.reindex(gitNode.getPath() + "/tree/2.x/.gitignore");
            query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/2.x/%'",
                                                            Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());
        } finally {
            testRoot.remove();
            session.save();
        }
    }

    @Test
    public void shouldHaveJdbcMetadataSourceConfigured() throws Exception {
        Session defaultSession = repository.login();
        // predefined
        Node dbRoot = defaultSession.getNode("/ModeShapeTestDb");
        assertNotNull(dbRoot);

        assertEquals("nt:unstructured", dbRoot.getPrimaryNodeType().getName());
        assertNotNull(dbRoot.getProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_NAME.toString()));
        assertNotNull(dbRoot.getProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION.toString()));
        assertNotNull(dbRoot.getProperty(JdbcMetadataLexicon.DATABASE_MAJOR_VERSION.toString()));
        assertNotNull(dbRoot.getProperty(JdbcMetadataLexicon.DATABASE_MINOR_VERSION.toString()));

        for (NodeType mixin : dbRoot.getMixinNodeTypes()) {
            if (mixin.getName().equalsIgnoreCase("mj:databaseRoot")) {
                return;
            }
        }
        fail("mj:databaseRoot not found on the root database node");
    }
}
