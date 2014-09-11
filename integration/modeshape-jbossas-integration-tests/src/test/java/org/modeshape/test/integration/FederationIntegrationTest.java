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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.meta.jdbc.JdbcMetadataLexicon;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.JcrTools;
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

        // create a new projection
        FederationManager federationManager = defaultSession.getWorkspace().getFederationManager();
        federationManager.createProjection("/", "filesystem_readonly", "/", "testProjection");
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
    public void shouldCorrectlyWriteFilesToDisk() throws Exception {
        Session session = repository.login();

        // configured via arquillian.xml
        String rootFolderPath = System.getProperty("rootDirectoryPath");
        assertNotNull(rootFolderPath);
        File rootFolder = new File(rootFolderPath);
        assertTrue(rootFolder.isDirectory());
        File subFolder = new File(rootFolder, "sub_folder");
        if (subFolder.exists()) {
            FileUtil.delete(subFolder);
        }
        assertFalse(subFolder.exists());

        // predefined
        Node rootProjection = session.getNode("/root");
        assertNotNull(rootProjection);

        //add a sub-folder
        rootProjection.addNode("sub_folder", "nt:folder");
        session.save();

        //check the newly added folder node was created
        subFolder = new File(rootFolder, "sub_folder");
        assertTrue(subFolder.exists());
        assertTrue(subFolder.isDirectory());

        //now add a file
        ByteArrayInputStream bis = new ByteArrayInputStream("test string".getBytes());
        new JcrTools().uploadFile(session, "/root/sub_folder/file", bis);
        session.save();
        File file = new File(subFolder, "file");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("test string", IoUtil.read(file));
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

            /**
             * //TODO author=Horia Chiorean date=11-Sep-14 description=This should be re-enabled after MODE-2178
            // check configured queryable branches
            workspace.reindex(gitNode.getPath() + "/tree/master/.gitignore");
            Query query = workspace.getQueryManager()
                                   .createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/master/%'", Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());

            workspace.reindex(gitNode.getPath() + "/tree/2.x/.gitignore");
            query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/2.x/%'",
                                                            Query.JCR_SQL2);
            assertEquals(2, query.execute().getNodes().getSize());
             */
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
