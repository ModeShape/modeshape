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

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.JcrConstants;
import java.io.File;

/**
 * Arquillian integration tests that checks if the workspace-specific and defalu initial content has been imported correctly
 * (see standalone-modeshape.xml)
 * 
 * @author Horia Chiorean
 */

@RunWith( Arquillian.class )
public class InitialContentIntegrationTest {

    @Resource( mappedName = "/jcr/initialContentRepo" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "initial-content-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }


    @Before
    public void before() {
        assertNotNull("JNDI repository not found", repository);
    }

    @Test
    public void shouldImportDefaultInitialContentIntoAllPredefinedWorkspaces() throws Exception {
        assertDefaultContentImportedInWorkspace("default");
        assertDefaultContentImportedInWorkspace("other");
    }

    @Test
    public void shouldImportCustomInitialContentIntoWorkspace() throws Exception {
        JcrSession session = repository.login("extra");

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
        JcrSession session = repository.login("empty");
        Node rootNode = session.getRootNode();
        assertEquals("Only the system node is expected under the root node", 1, rootNode.getNodes().getSize());
    }

    private void assertDefaultContentImportedInWorkspace( String workspaceName ) throws RepositoryException {
        JcrSession session = repository.login(workspaceName);

        Node cars = session.getNode("/cars");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, cars.getPrimaryNodeType().getName());
    }

}
