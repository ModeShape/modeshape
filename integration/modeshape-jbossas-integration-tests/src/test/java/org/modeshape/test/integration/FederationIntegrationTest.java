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

import static org.junit.Assert.assertArrayEquals;
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
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.meta.jdbc.JdbcMetadataLexicon;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Session;
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
        archive.addAsResource(new File("src/test/resources/sequencer/image_file.jpg"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF")).addClass(JdbcMetadataLexicon.class);
        return archive;
    }

    @Test
    public void shouldAccessExternalSourceAsWorkspace() throws Exception {
        Session defaultSession = null;
        try {
            defaultSession = repository.login("filesystem");
        } finally {
            if ( defaultSession != null ) defaultSession.logout();
        }
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
    @FixFor( "MODE-2402" )
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
        JcrTools tools = new JcrTools();
        tools.uploadFile(session, "/root/sub_folder/file", bis);
        session.save();
        File file = new File(subFolder, "file");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("test string", IoUtil.read(file));
        
        //add a second file with a larger binary content and make sure no binaries are written to disk
        tools.uploadFile(session, "/root/sub_folder/image_file.jpg", getClass().getClassLoader().getResourceAsStream("image_file.jpg"));
        session.save();
        File image = new File(subFolder, "image_file.jpg");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        byte[] expectedContent = IoUtil.readBytes(getClass().getClassLoader().getResourceAsStream("image_file.jpg"));
        byte[] actualContent = IoUtil.readBytes(image);
        assertArrayEquals("File content not uploaded correctly", expectedContent, actualContent);
        
        //make sure that no binaries were persisted "by default" on the FS since there is no binary store explicitly configured
        File defaultFsBinaryFolder = new File(System.getProperty("jboss.server.data.dir") + "/modeshape/federatedRepository/binaries");
        assertFalse("There shouldn't be a FS binary folder", defaultFsBinaryFolder.exists());
    }

    @Test
    public void shouldHaveGitSourceConfigured() throws Exception {
        Session session = repository.login();
        assertNotNull(session.getNode("/modeshape_git"));
        assertNotNull(session.getNode("/modeshape_git/branches"));
        assertNotNull(session.getNode("/modeshape_git/tags"));
        assertNotNull(session.getNode("/modeshape_git/commits"));
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
