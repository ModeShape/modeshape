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

import javax.jcr.Session;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.JcrConstants;

/**
 * Arquillian integration tests that checks if certain "preconfiguration features" work correctly (see standalone-modeshape.xml)
 * Among the preconfiguration features, there is: initial content import, node types import.
 *
 * @author Horia Chiorean
 */

@RunWith( Arquillian.class )
public class PreconfiguredRepositoryIntegrationTest {

    @Resource( mappedName = "java:/jcr/preconfiguredRepository" )
    private JcrRepository preconfiguredRepository;

    @Resource( mappedName = "java:/jcr/artifacts" )
    private JcrRepository artifactsRepository;

    @Resource( mappedName = "java:/jcr/journalingRepository" )
    private JcrRepository journalingRepository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "preconfiguredRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Before
    public void before() {
        assertNotNull("preconfiguredRepository repository not found", preconfiguredRepository);
        assertNotNull("artifactsRepository repository not found", artifactsRepository);
    }

    @Test
    public void shouldImportDefaultInitialContentIntoAllPredefinedWorkspaces() throws Exception {
        assertDefaultContentImportedInWorkspace("default");
        assertDefaultContentImportedInWorkspace("other");
    }

    @Test
    public void shouldImportCustomInitialContentIntoWorkspace() throws Exception {
        JcrSession session = preconfiguredRepository.login("extra");

        Node folder = session.getNode("/folder");
        assertEquals(JcrConstants.NT_FOLDER, folder.getPrimaryNodeType().getName());

        Node file1 = session.getNode("/folder/file1");
        assertEquals(JcrConstants.NT_FILE, file1.getPrimaryNodeType().getName());
        Node file1Content = session.getNode("/folder/file1/jcr:content");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, file1Content.getPrimaryNodeType().getName());

        Node file2 = session.getNode("/folder/file2");
        assertEquals(JcrConstants.NT_FILE, file2.getPrimaryNodeType().getName());
        Node file2Content = session.getNode("/folder/file2/jcr:content");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, file2Content.getPrimaryNodeType().getName());
    }

    @Test
    public void shouldNotImportContentIntoWorkspaceConfiguredWithEmptyContent() throws Exception {
        JcrSession session = preconfiguredRepository.login("empty");
        Node rootNode = session.getRootNode();
        assertEquals("Only the system node is expected under the root node", 1, rootNode.getNodes().getSize());
    }

    private void assertDefaultContentImportedInWorkspace( String workspaceName ) throws RepositoryException {
        JcrSession session = preconfiguredRepository.login(workspaceName);

        Node cars = session.getNode("/cars");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, cars.getPrimaryNodeType().getName());
    }

    @Test
    public void shouldImportNodeTypes() throws Exception {
        Session session = preconfiguredRepository.login();
        session.getRootNode().addNode("car", "car:Car");
        session.save();

        Node car = session.getNode("/car");
        assertEquals("car:Car", car.getPrimaryNodeType().getName());
    }

    @Test
    @FixFor( "MODE-1919" )
    public void artifactsRepositoryShouldBePublishArea() throws Exception {
        Session session = artifactsRepository.login();
        Node filesFolder = session.getNode("/files");
        assertNotNull(filesFolder);
        assertEquals("nt:folder", filesFolder.getPrimaryNodeType().getName());
        NodeType[] mixins = filesFolder.getMixinNodeTypes();
        assertEquals(1, mixins.length);
        assertEquals("mode:publishArea", mixins[0].getName());
        session = artifactsRepository.login("other");
        session.logout();
        session = artifactsRepository.login("extra");
        session.logout();
    }

    @Test
    @FixFor( "MODE-1683" )
    public void shouldEnableJournaling() throws Exception {
        Session session = journalingRepository.login();
        session.getRootNode().addNode("testNode");
        session.save();
        session.getNode("/testNode").remove();
        session.save();
        session.logout();
        ////TODO author=Horia Chiorean date=11/21/13 description=Once JCR event journaling is in place, validate the entry was stored
    }
}
