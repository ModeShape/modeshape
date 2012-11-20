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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.testing.mock.MockConnector;
import org.modeshape.jcr.api.federation.FederationManager;

/**
 * Unit test for {@link ModeShapeFederationManager}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * //TODO author=Horia Chiorean date=11/20/12 description=I think at some point we should split this up into different tests
 * for real connectors
 */
public class ModeShapeFederationManagerTest extends SingleUseAbstractTest {

    protected static final String PATH_TO_FILESYSTEM_SOURCE = "target/federation/files";
    protected static final String FILESYSTEM_SOURCE_NAME = "file-resources";
    protected static final String TEXT_CONTENT = "Some text content";

    private FederationManager federationManager;
    private AbstractJcrNode testRoot;
    private File testDir;

    @Before
    public void before() throws Exception {
        // Remove and then make the directory for our federation test ...
        testDir = new File(PATH_TO_FILESYSTEM_SOURCE);
        if (testDir.exists()) FileUtil.delete(testDir);
        testDir.mkdirs();
        // Make some content ...
        new File(testDir, "dir1").mkdir();
        new File(testDir, "dir2").mkdir();
        new File(testDir, "dir3").mkdir();
        File simpleJson = new File(testDir, "dir3/simple.json");
        IoUtil.write(getClass().getClassLoader().getResourceAsStream("data/simple.json"), new FileOutputStream(simpleJson));
        File simpleTxt = new File(testDir, "dir3/simple.txt");
        IoUtil.write(TEXT_CONTENT, new FileOutputStream(simpleTxt));

        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/repo-config-federation.json"));

        testRoot = session.getRootNode().addNode("testFilesRoot");
        testRoot = session.getRootNode().addNode("testRoot");
        testRoot.addNode("node1");
        session.save();

        federationManager = session.getWorkspace().getFederationManager();
    }

    @Test
    public void shouldCreateProjectionWithAlias() throws Exception {
        // link the first external document
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc1", "federated1");
        assertEquals(2, testRoot.getNodes().getSize());

        Node doc1Federated = session.getNode("/testRoot/federated1");
        assertNotNull(doc1Federated);
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());

        // link a second external document with a sub-child
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc2", "federated2");
        assertEquals(3, testRoot.getNodes().getSize());

        Node doc2Federated = session.getNode("/testRoot/federated2");
        assertNotNull(doc2Federated);
        assertEquals(testRoot.getIdentifier(), doc2Federated.getParent().getIdentifier());
        assertEquals("another string", doc2Federated.getProperty("federated2_prop1").getString());
        assertEquals(false, doc2Federated.getProperty("federated2_prop2").getBoolean());

        Node doc2FederatedChild = session.getNode("/testRoot/federated2/federated3");
        assertNotNull(doc2FederatedChild);
        assertEquals("yet another string", doc2FederatedChild.getProperty("federated3_prop1").getString());
    }

    @Test
    public void shouldCreateProjectionWithoutAlias() throws Exception {
        // link the first external document
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc1", null);
        assertEquals(2, testRoot.getNodes().getSize());

        Node doc1Federated = session.getNode("/testRoot/doc1");
        assertNotNull(doc1Federated);
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());
    }

    @Test
    public void shouldCreateExternalNode() throws Exception {
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc1", "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        // create an external node with a property and a child
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);
        externalNode1.setProperty("prop1", "a value");
        externalNode1.addNode("federated1_1_1", null);

        session.save();

        Node federated1_1 = doc1Federated.getNode("federated1_1");
        assertNotNull(federated1_1);
        assertEquals(doc1Federated, federated1_1.getParent());
        assertEquals(1, doc1Federated.getNodes().getSize());
        assertNotNull(session.getNode("/testRoot/federated1/federated1_1"));
        assertEquals("a value", federated1_1.getProperty("prop1").getString());

        AbstractJcrNode federated1_1_1 = session.getNode("/testRoot/federated1/federated1_1/federated1_1_1");
        assertEquals(federated1_1, federated1_1_1.getParent());
        assertNotNull(federated1_1_1);
    }

    @Test
    public void shouldUpdateExternalNode() throws Exception {
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc1", "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);
        externalNode1.setProperty("prop1", "a value");
        session.save();

        externalNode1.setProperty("prop1", "edited value");
        session.save();

        Node federated1_1 = doc1Federated.getNode("federated1_1");
        assertEquals("edited value", federated1_1.getProperty("prop1").getString());
    }

    @Test
    public void shouldRemoveExternalNode() throws Exception {
        federationManager.createExternalProjection("/testRoot", MockConnector.SOURCE_NAME, "/doc1", "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        doc1Federated.addNode("federated1_1", null);
        session.save();

        Node externalNode = session.getNode("/testRoot/federated1/federated1_1");
        externalNode.remove();
        session.save();

        try {
            session.getNode("/testRoot/federated1/federated1_1");
            fail("External node should've been deleted");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    @Test
    public void shouldNavigateChildrenFromPagedConnector() throws Exception {
        federationManager.createExternalProjection("/testRoot",
                                                   MockConnector.SOURCE_NAME,
                                                   MockConnector.PAGED_DOC_LOCATION,
                                                   "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        NodeIterator nodesIterator = doc1Federated.getNodes();
        assertEquals(3, nodesIterator.getSize());

        List<String> childrenNames = new ArrayList<String>(3);
        while (nodesIterator.hasNext()) {
            childrenNames.add(nodesIterator.nextNode().getName());
        }
        assertEquals(Arrays.asList("federated4", "federated5", "federated6"), childrenNames);
    }

    @Test
    public void shouldReadExistingFileNodesFromExternalFileSystemSource() throws Exception {
        federationManager.createExternalProjection("/testRoot", FILESYSTEM_SOURCE_NAME, "/", "files");
        Node files = session.getNode("/testRoot/files");
        assertThat(files.getName(), is("files"));
        assertThat(files.getPrimaryNodeType().getName(), is("nt:folder"));
        Node dir1 = session.getNode("/testRoot/files/dir1");
        Node dir2 = session.getNode("/testRoot/files/dir2");
        Node dir3 = session.getNode("/testRoot/files/dir3");
        Node simpleJson = session.getNode("/testRoot/files/dir3/simple.json");
        Node simpleText = session.getNode("/testRoot/files/dir3/simple.txt");
        assertFolder(dir1, "dir1");
        assertFolder(dir2, "dir2");
        assertFolder(dir3, "dir3");
        assertFile(simpleJson, "dir3/simple.json");
        assertFile(simpleText, "dir3/simple.txt");

        // Look for a node that isn't there ...
        try {
            session.getNode("/testRoot/files/dir3/non-existant.oops");
            fail("Should not have been able to find a non-existing file");
        } catch (PathNotFoundException e) {
            // expected
        }

    }

    protected void assertFolder( Node node,
                                 String relativeDirPath ) throws RepositoryException {
        File dir = getTestFile(relativeDirPath);
        assertThat(node.getName(), is(dir.getName()));
        assertThat(node.getIndex(), is(1));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
        assertThat(node.getProperty("jcr:created").getLong(), is(dir.lastModified()));
    }

    protected void assertFile( Node node,
                               String relativeFilePath ) throws RepositoryException {
        File file = getTestFile(relativeFilePath);
        long lastModified = file.lastModified();
        assertThat(node.getName(), is(file.getName()));
        assertThat(node.getIndex(), is(1));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:file"));
        assertThat(node.getProperty("jcr:created").getLong(), is(lastModified));
        Node content = node.getNode("jcr:content");
        assertThat(content.getName(), is("jcr:content"));
        assertThat(content.getIndex(), is(1));
        assertThat(content.getPrimaryNodeType().getName(), is("nt:resource"));
        assertThat(content.getProperty("jcr:lastModified").getLong(), is(lastModified));
    }

    protected File getTestFile( String relativePath ) {
        return new File(testDir, relativePath);
    }

}
