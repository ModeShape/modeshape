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

package org.modeshape.connector.filesystem;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * Unit test for {@link org.modeshape.connector.filesystem.FileSystemConnector}
 */
public class FileSystemConnectorTest extends SingleUseAbstractTest {

    protected static final String TEXT_CONTENT = "Some text content";

    private Node testRoot;
    private Projection readOnlyProjection;
    private Projection readOnlyProjectionWithExclusion;
    private Projection readOnlyProjectionWithInclusion;
    private Projection storeProjection;
    private Projection jsonProjection;
    private Projection legacyProjection;
    private Projection noneProjection;
    private Projection pagedProjection;
    private Projection largeFilesProjection;
    private Projection largeFilesProjectionDefault;
    private Projection monitoringProjection;
    private Projection[] projections;
    private JcrTools tools;

    @Before
    public void before() throws Exception {
        tools = new JcrTools();
        readOnlyProjection = new Projection("readonly-files", "target/federation/files-read");
        readOnlyProjectionWithExclusion = new Projection("readonly-files-with-exclusion",
                                                         "target/federation/files-read-exclusion");
        readOnlyProjectionWithInclusion = new Projection("readonly-files-with-inclusion",
                                                         "target/federation/files-read-inclusion");
        storeProjection = new Projection("mutable-files-store", "target/federation/files-store");
        jsonProjection = new Projection("mutable-files-json", "target/federation/files-json");
        legacyProjection = new Projection("mutable-files-legacy", "target/federation/files-legacy");
        noneProjection = new Projection("mutable-files-none", "target/federation/files-none");
        pagedProjection = new PagedProjection("paged-files", "target/federation/paged-files");
        largeFilesProjection = new LargeFilesProjection("large-files", "target/federation/large-files");
        largeFilesProjectionDefault = new LargeFilesProjection("large-files-default", "target/federation/large-files-default");
        monitoringProjection = new Projection("monitoring", "target/federation/monitoring");

        projections = new Projection[] {readOnlyProjection, readOnlyProjectionWithInclusion, readOnlyProjectionWithExclusion,
            storeProjection, jsonProjection, legacyProjection, noneProjection, pagedProjection, largeFilesProjection,
            largeFilesProjectionDefault, monitoringProjection};

        // Remove and then make the directory for our federation test ...
        for (Projection projection : projections) {
            projection.initialize();
        }

        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-filesystem-federation.json"));
        registerNodeTypes("cnd/flex.cnd");

        Session session = (Session)jcrSession();
        testRoot = session.getRootNode().addNode("testRoot");
        testRoot.addNode("node1");
        session.save();

        readOnlyProjection.create(testRoot, "readonly");
        storeProjection.create(testRoot, "store");
        jsonProjection.create(testRoot, "json");
        legacyProjection.create(testRoot, "legacy");
        noneProjection.create(testRoot, "none");
        pagedProjection.create(testRoot, "pagedFiles");
        largeFilesProjection.create(testRoot, "largeFiles");
        largeFilesProjectionDefault.create(testRoot, "largeFilesDefault");
        monitoringProjection.create(testRoot, "monitoring");
    }

    @Test
    public void shouldBrowseExternalWorkspace() throws Exception {
        Session session2 = session.getRepository().login("readonly-fls");
        assertTrue(session2 != null);
        
        Node node = session2.getNode("/");
//        System.out.println("Root=" + node.getName());
        
//        System.out.println("Level1------------");
        NodeIterator it = node.getNodes();
        
        ArrayList<String> dirs = new ArrayList<>();
        dirs.add("dir1");
        dirs.add("dir2");
        dirs.add("dir3");
        
        while (it.hasNext()) {
            dirs.remove(it.nextNode().getName());
        }
        
        assertEquals(0, dirs.size());
    }
    
    @Test
    @FixFor( "MODE-2061" )
    public void largeFilesTest() throws Exception {
        largeFilesURIBased();
        largeFilesContentBased();
    }

    private void largeFilesURIBased() throws Exception {
        String childName = "largeFiles";
        Session session = (Session)testRoot.getSession();
        String path = testRoot.getPath() + "/" + childName;

        Node files = session.getNode(path);
        assertThat(files.getName(), is(childName));
        assertThat(files.getPrimaryNodeType().getName(), is("nt:folder"));
        long before = System.currentTimeMillis();
        Node node1 = session.getNode(path + "/large-file1.png");
        long after = System.currentTimeMillis();
        long elapsed = after - before;
        assertThat(node1.getName(), is("large-file1.png"));
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:file"));

        before = System.currentTimeMillis();
        Node node1Content = node1.getNode("jcr:content");
        after = System.currentTimeMillis();
        elapsed = after - before;
        assertThat(node1Content.getName(), is("jcr:content"));
        assertThat(node1Content.getPrimaryNodeType().getName(), is("nt:resource"));

        Binary binary = (Binary)node1Content.getProperty("jcr:data").getBinary();
        before = System.currentTimeMillis();
        String dsChecksum = binary.getHexHash();
        after = System.currentTimeMillis();
        assertThat(dsChecksum, is(notNullValue()));
        elapsed = after - before;

        before = System.currentTimeMillis();
        dsChecksum = binary.getHexHash();
        after = System.currentTimeMillis();
        elapsed = after - before;
        assertTrue(elapsed < 1000);
    }

    public void largeFilesContentBased() throws Exception {
        String childName = "largeFilesDefault";
        Session session = (Session)testRoot.getSession();
        String path = testRoot.getPath() + "/" + childName;

        Node files = session.getNode(path);
        assertThat(files.getName(), is(childName));
        assertThat(files.getPrimaryNodeType().getName(), is("nt:folder"));
        long before = System.currentTimeMillis();
        Node node1 = session.getNode(path + "/large-file1.png");
        long after = System.currentTimeMillis();
        long elapsed = after - before;
        assertThat(node1.getName(), is("large-file1.png"));
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:file"));

        before = System.currentTimeMillis();
        Node node1Content = node1.getNode("jcr:content");
        after = System.currentTimeMillis();
        elapsed = after - before;
        assertThat(node1Content.getName(), is("jcr:content"));
        assertThat(node1Content.getPrimaryNodeType().getName(), is("nt:resource"));

        Binary binary = (Binary)node1Content.getProperty("jcr:data").getBinary();
        before = System.currentTimeMillis();
        String dsChecksum = binary.getHexHash();
        after = System.currentTimeMillis();
        assertThat(dsChecksum, is(notNullValue()));
        elapsed = after - before;

        before = System.currentTimeMillis();
        dsChecksum = binary.getHexHash();
        after = System.currentTimeMillis();
        elapsed = after - before;
        assertTrue(elapsed < 1000);
    }

    @Test
    @FixFor( "MODE-1982" )
    public void shouldReadNodesInAllProjections() throws Exception {
        readOnlyProjection.testContent(testRoot, "readonly");
        storeProjection.testContent(testRoot, "store");
        jsonProjection.testContent(testRoot, "json");
        legacyProjection.testContent(testRoot, "legacy");
        noneProjection.testContent(testRoot, "none");
        pagedProjection.testContent(testRoot, "pagedFiles");
        largeFilesProjection.testContent(testRoot, "largeFiles");
        largeFilesProjectionDefault.testContent(testRoot, "largeFilesDefault");
    }

    @Test
    @FixFor( "MODE-1951" )
    public void shouldReadNodesInProjectionWithInclusionFilter() throws Exception {
        readOnlyProjectionWithInclusion.create(testRoot, "readonly-inclusion");

        assertNotNull(session.getNode("/testRoot/readonly-inclusion"));
        assertNotNull(session.getNode("/testRoot/readonly-inclusion/dir3"));
        assertNotNull(session.getNode("/testRoot/readonly-inclusion/dir3/simple.json"));
        assertNotNull(session.getNode("/testRoot/readonly-inclusion/dir3/simple.txt"));

        assertPathNotFound("/testRoot/readonly-inclusion/dir1");
        assertPathNotFound("/testRoot/readonly-inclusion/dir2");
    }

    @Test
    @FixFor( "MODE-1951" )
    public void shouldReadNodesInProjectionWithExclusionFilter() throws Exception {
        readOnlyProjectionWithExclusion.create(testRoot, "readonly-exclusion");

        assertNotNull(session.getNode("/testRoot/readonly-exclusion"));
        assertNotNull(session.getNode("/testRoot/readonly-exclusion/dir3"));
        assertNotNull(session.getNode("/testRoot/readonly-exclusion/dir1"));
        assertNotNull(session.getNode("/testRoot/readonly-exclusion/dir2"));
        assertPathNotFound("/testRoot/readonly-exclusion/dir3/simple.json");
        assertPathNotFound("/testRoot/readonly-exclusion/dir3/simple.txt");
    }

    @Test
    public void shouldReadNodesInReadOnlyProjection() throws Exception {
        readOnlyProjection.testContent(testRoot, "readonly");
    }

    @Test
    public void shouldNotAllowUpdatingNodesInReadOnlyProjection() throws Exception {
        Node file = session.getNode("/testRoot/readonly/dir3/simple.json");
        try {
            file.addMixin("flex:anyProperties");
            file.setProperty("extraProp", "extraValue");
            session.save();
            fail("failed to throw read-only exception");
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Test
    public void shouldNotAllowRemovingNodesInReadOnlyProjection() throws Exception {
        Node file = session.getNode("/testRoot/readonly/dir3/simple.json");
        try {
            session.refresh(false);
            file.remove();
            session.save();
            fail("failed to throw read-only exception");
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Test
    public void shouldAllowUpdatingNodesInWritableStoreBasedProjection() throws Exception {
        Node file = session.getNode("/testRoot/store/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        session.save();
        assertNoSidecarFile(storeProjection, "dir3/simple.json.modeshape");
        Node file2 = session.getNode("/testRoot/store/dir3/simple.json");
        assertThat(file2.getProperty("extraProp").getString(), is("extraValue"));
    }

    @Test
    @FixFor( {"MODE-1971", "MODE-1976"} )
    public void shouldBeAbleToCopyExternalNodesInTheSameSource() throws Exception {
        ((Workspace)session.getWorkspace()).copy("/testRoot/store/dir3/simple.json", "/testRoot/store/dir3/simple2.json");
        Node file = session.getNode("/testRoot/store/dir3/simple2.json");
        assertNotNull(file);
        assertEquals("nt:file", file.getPrimaryNodeType().getName());

        ((Workspace)session.getWorkspace()).copy("/testRoot/store/dir3", "/testRoot/store/dir4");
        Node folder = session.getNode("/testRoot/store/dir4");
        assertNotNull(folder);
        assertEquals("nt:folder", folder.getPrimaryNodeType().getName());
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldBeAbleToCopyExternalNodesIntoTheRepository() throws Exception {
        jcrSession().getRootNode().addNode("files");
        jcrSession().save();
        jcrSession().getWorkspace().copy("/testRoot/store/dir3/simple.json", "/files/simple.json");
        Node file = session.getNode("/files/simple.json");
        assertNotNull(file);
        assertEquals("nt:file", file.getPrimaryNodeType().getName());
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldBeAbleToCopyFromRepositoryToExternalSource() throws Exception {
        jcrSession().getRootNode().addNode("files").addNode("dir", "nt:folder");
        jcrSession().save();
        jcrSession().getWorkspace().copy("/files/dir", "/testRoot/store/dir");
        Node dir = session.getNode("/testRoot/store/dir");
        assertNotNull(dir);
        assertEquals("nt:folder", dir.getPrimaryNodeType().getName());
    }

    @Test
    @FixFor( {"MODE-1971", "MODE-1977", "MODE-2256"} )
    public void shouldBeAbleToRenameExternalNodes() throws Exception {
        Node file = session.getNode("/testRoot/json/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        assertEquals("nt:file", file.getPrimaryNodeType().getName());
        session.save();

        //rename the file
        ((Workspace)session.getWorkspace()).move("/testRoot/json/dir3/simple.json", "/testRoot/json/dir3/simple2.json");

        Node renamedFile = session.getNode("/testRoot/json/dir3/simple2.json");
        assertThat(renamedFile.getProperty("extraProp").getString(), is("extraValue"));
    }

    @Test
    @FixFor( {"MODE-1971", "MODE-1977", "MODE-2256"} )
    public void shouldBeAbleToMoveExternalNodes() throws Exception {
        Node dir3 = session.getNode("/testRoot/json/dir3");
        dir3.addNode("dir4", "nt:folder");
        session.save();
        assertEquals("nt:folder", ((Node)session.getNode("/testRoot/json/dir3/dir4")).getPrimaryNodeType().getName());

        Node file = session.getNode("/testRoot/json/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        assertEquals("nt:file", file.getPrimaryNodeType().getName());
        session.save();

        ((Workspace)session.getWorkspace()).move("/testRoot/json/dir3/simple.json", "/testRoot/json/dir3/dir4/simple.json");

        Node movedFile = session.getNode("/testRoot/json/dir3/dir4/simple.json");
        assertEquals("nt:file", movedFile.getPrimaryNodeType().getName());
        assertThat(movedFile.getProperty("extraProp").getString(), is("extraValue"));
    }

    @Test
    public void shouldAllowUpdatingNodesInWritableJsonBasedProjection() throws Exception {
        Node file = session.getNode("/testRoot/json/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        Node content = file.getNode("jcr:content");
        content.addMixin("flex:anyProperties");
        content.setProperty("extraProp2", "extraValue2");
        session.save();
        assertThat(file.getProperty("extraProp").getString(), is("extraValue"));
        assertThat(file.getProperty("jcr:content/extraProp2").getString(), is("extraValue2"));
        assertJsonSidecarFile(jsonProjection, "dir3/simple.json");
        assertJsonSidecarFile(jsonProjection, "dir3/simple.json");
        Node file2 = session.getNode("/testRoot/json/dir3/simple.json");
        assertThat(file2.getProperty("extraProp").getString(), is("extraValue"));
        try {
            // Make sure the sidecar file can't be seen via JCR ...
            session.getNode("/testRoot/json/dir3/simple.json.modeshape.json");
            fail("found sidecar file as JCR node");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    @Test
    public void shouldAllowUpdatingNodesInWritableLegacyBasedProjection() throws Exception {
        Node file = session.getNode("/testRoot/legacy/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        session.save();
        assertLegacySidecarFile(legacyProjection, "dir3/simple.json");
        Node file2 = session.getNode("/testRoot/legacy/dir3/simple.json");
        assertThat(file2.getProperty("extraProp").getString(), is("extraValue"));
        try {
            // Make sure the sidecar file can't be seen via JCR ...
            session.getNode("/testRoot/json/dir3/simple.json.modeshape");
            fail("found sidecar file as JCR node");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    @Test
    @FixFor( "MODE-1882" )
    public void shouldAllowCreatingNodesInWritablStoreBasedProjection() throws Exception {
        String actualContent = "This is the content of the file.";
        tools.uploadFile(session, "/testRoot/store/dir3/newFile.txt", new ByteArrayInputStream(actualContent.getBytes()));
        session.save();

        // Make sure the file on the file system contains what we put in ...
        assertFileContains(storeProjection, "dir3/newFile.txt", actualContent.getBytes());

        // Make sure that we can re-read the binary content via JCR ...
        Node contentNode = session.getNode("/testRoot/store/dir3/newFile.txt/jcr:content");
        Binary value = (Binary)contentNode.getProperty("jcr:data").getBinary();
        assertBinaryContains(value, actualContent.getBytes());
    }

    @Test
    @FixFor( "MODE-1802" )
    public void shouldSupportRootProjection() throws Exception {
        // Clean up the folder that the test creates
        FileUtil.delete("target/classes/test");

        javax.jcr.Session session = session();
        Node root = session.getNode("/fs");
        assertNotNull(root);
        Node folder1 = root.addNode("test", "nt:folder");
        session.save();
        Node folder2 = root.getNode("test");
        assertThat(folder1.getIdentifier(), is(folder2.getIdentifier()));
    }

    @Test
    @FixFor( "MODE-1802" )
    public void shouldIgnoreNamespaces() throws Exception {
        // Clean up the folder that the test creates
        FileUtil.delete("target/classes/test");

        javax.jcr.Session session = session();
        session.setNamespacePrefix("ms_test", "http://www.modeshape.org/test/");
        Node root = session.getNode("/fs");
        assertNotNull(root);

        root.addNode("ms_test:test", "nt:folder");
        session.save();

        assertNotNull(root.getNode("test"));
    }

    @Test
    @FixFor( "MODE-2073" )
    public void shouldBeAbleToCopyExternalNodesWithBinaryValuesIntoTheRepository() throws Exception {
        javax.jcr.Binary externalBinary = jcrSession().getNode("/testRoot/store/dir3/simple.json/jcr:content")
                                                      .getProperty("jcr:data").getBinary();
        jcrSession().getRootNode().addNode("files");
        jcrSession().save();
        jcrSession().getWorkspace().copy("/testRoot/store/dir3/simple.json", "/files/simple.json");
        Node file = session.getNode("/files/simple.json");
        assertNotNull(file);
        assertEquals("nt:file", file.getPrimaryNodeType().getName());
        Property property = file.getNode("jcr:content").getProperty("jcr:data");
        assertNotNull(property);
        javax.jcr.Binary copiedBinary = property.getBinary();
        assertFalse(copiedBinary instanceof ExternalBinaryValue);
        assertArrayEquals(IoUtil.readBytes(externalBinary.getStream()), IoUtil.readBytes(copiedBinary.getStream()));
    }

    @Test
    @FixFor( {"MODE-2040", "MODE-2189"} )
    public void shouldReceiveNotificationsWhenCreatingFiles() throws Exception {
        // print = true;
        File rootFolder = new File("target/federation/monitoring");
        assertTrue(rootFolder.exists() && rootFolder.isDirectory());

        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        // for each file we expect 7 events; for each folder 3 events (see below)
        int expectedEventCount = 31;
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        FSListener listener = new FSListener(latch);
        observationManager.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED, "/testRoot/monitoring", true,
                                            null, null, false);
        Thread.sleep(300);
        addFile(rootFolder, "testfile1", "data/simple.json");
        Thread.sleep(300);
        addFile(rootFolder, "testfile2", "data/simple.json");
        File folder1 = new File(rootFolder, "folder1");
        assertTrue(folder1.mkdirs() && folder1.exists() && folder1.isDirectory());
        // wait a bit to make sure the new folder is being watched
        Thread.sleep(1600);
        addFile(folder1, "testfile11", "data/simple.json");
        Thread.sleep(300);
        addFile(rootFolder, "dir1/testfile11", "data/simple.json");
        Thread.sleep(1600);

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Events not received from connector");
        }

        Map<Integer, List<String>> receivedEvents = listener.getReceivedEventTypeAndPaths();
        assertFalse(receivedEvents.isEmpty());
        List<String> nodeAddedPaths = receivedEvents.get(Event.NODE_ADDED);
        assertFalse("Expected NODE_ADDED events", nodeAddedPaths.isEmpty());
        List<String> propertyAddedPaths = receivedEvents.get(Event.PROPERTY_ADDED);
        assertFalse("Expected PROPERTY_ADDED events", propertyAddedPaths.isEmpty());

        // the root paths are defined in the monitoring projection
        assertEventsFiredOnCreate("/testRoot/monitoring/testfile1", true, nodeAddedPaths, propertyAddedPaths);
        assertEventsFiredOnCreate("/testRoot/monitoring/testfile2", true, nodeAddedPaths, propertyAddedPaths);
        assertEventsFiredOnCreate("/testRoot/monitoring/folder1", false, nodeAddedPaths, propertyAddedPaths);
        assertEventsFiredOnCreate("/testRoot/monitoring/folder1/testfile11", true, nodeAddedPaths, propertyAddedPaths);
        assertEventsFiredOnCreate("/testRoot/monitoring/dir1/testfile11", true, nodeAddedPaths, propertyAddedPaths);
    }

    private void assertEventsFiredOnCreate( String fileAbsPath,
                                            boolean isFile,
                                            List<String> actualNodePaths,
                                            List<String> actualPropertyPaths ) {
        assertTrue(actualNodePaths.contains(fileAbsPath));
        assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:createdBy"));
        assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:created"));

        if (isFile) {
            assertTrue(actualNodePaths.contains(fileAbsPath + "/jcr:content"));

            assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:data"));
            // assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:mimeType"));
            assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:lastModified"));
            assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:lastModifiedBy"));
        }
    }

    @Test
    @FixFor( "MODE-2189" )
    public void shouldSequenceFilesAddedExternally() throws Exception {
        // print = true;
        FileUtil.delete("target/federation/monitoring/images.cnd");
        File rootFolder = new File("target/federation/monitoring");
        assertTrue(rootFolder.exists() && rootFolder.isDirectory());

        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        // for each file we expect 7 events + 1 sequencing event
        // on Windows, both an ENTRY_CREATED & ENTRY_MODIFIED is fired when creating the file, so it will really be sequenced
        // twice
        int expectedEventCount = 8;
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        FSListener listener = new FSListener(latch);
        int eventTypes = Event.NODE_ADDED | Event.PROPERTY_ADDED | org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED;
        observationManager.addEventListener(listener, eventTypes, "/testRoot/monitoring", true, null, null, false);
        addFile(rootFolder, "images.cnd", "sequencer/cnd/images.cnd");
        Thread.sleep(300);

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Events not received from connector");
        }

        Map<Integer, List<String>> receivedEvents = listener.getReceivedEventTypeAndPaths();
        List<String> nodeAddedPaths = receivedEvents.get(Event.NODE_ADDED);
        assertFalse("Expected NODE_ADDED events", nodeAddedPaths.isEmpty());
        List<String> propertyAddedPaths = receivedEvents.get(Event.PROPERTY_ADDED);
        assertFalse("Expected PROPERTY_ADDED events", propertyAddedPaths.isEmpty());

        assertEventsFiredOnCreate("/testRoot/monitoring/images.cnd", true, nodeAddedPaths, propertyAddedPaths);
        List<String> nodeSequencedPaths = receivedEvents.get(org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED);
        assertFalse("Expected NODE_SEQUENCED events", nodeSequencedPaths.isEmpty());

        // verify that the CND sequencer has fired
        Node sequencedRoot = session.getNode("/testRoot/sequenced/images.cnd"); // configured as such
        assertTrue(sequencedRoot.getNodes().hasNext());
    }

    @Ignore( "Doesn't work correctly on OSX" )
    @Test
    @FixFor( {"MODE-2040", "MODE-2189"} )
    public void shouldReceiveNotificationsWhenChangingFileContent() throws Exception {
        File rootFolder = new File("target/federation/monitoring");
        assertTrue(rootFolder.exists() && rootFolder.isDirectory());

        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        int expectedEventCount = 3;
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        FSListener listener = new FSListener(latch);
        observationManager.addEventListener(listener, Event.PROPERTY_CHANGED, null, true, null, null, false);

        File dir3 = new File(rootFolder, "dir3");
        assertTrue(dir3.exists() && dir3.isDirectory());
        addFile(dir3, "simple.txt", "data/simple.json");
        Thread.sleep(3000);

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Events not received from connector");
        }

        Map<Integer, List<String>> receivedEvents = listener.getReceivedEventTypeAndPaths();
        assertFalse(receivedEvents.isEmpty());
        List<String> propertyChangedPaths = receivedEvents.get(Event.PROPERTY_CHANGED);
        assertEventsFiredOnModify("/testRoot/monitoring/dir3/simple.txt", true, propertyChangedPaths);
    }

    private void assertEventsFiredOnModify( String fileAbsPath,
                                            boolean isFile,
                                            List<String> actualPropertyPaths ) {
        if (!isFile) {
            return;
        }
        assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:data"));
        assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:lastModified"));
        assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:lastModifiedBy"));
        // assertTrue(actualPropertyPaths.contains(fileAbsPath + "/jcr:content/jcr:mimeType"));
    }

    @Test
    @FixFor( "MODE-2040" )
    public void shouldReceiveNotificationsWhenRemovingFiles() throws Exception {
        File rootFolder = new File("target/federation/monitoring");
        assertTrue(rootFolder.exists() && rootFolder.isDirectory());

        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        int expectedEventCount = 3;
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        FSListener listener = new FSListener(latch);
        observationManager.addEventListener(listener, Event.NODE_REMOVED, null, true, null, null, false);
        Thread.sleep(300);
        File dir3 = new File(rootFolder, "dir3");
        FileUtil.delete(new File(dir3, "simple.json"));
        Thread.sleep(300);
        FileUtil.delete(new File(dir3, "simple.txt"));
        Thread.sleep(1700);
        FileUtil.delete(dir3);

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Events not received from connector");
        }

        Map<Integer, List<String>> receivedEvents = listener.getReceivedEventTypeAndPaths();
        assertFalse(receivedEvents.isEmpty());
        List<String> receivedPaths = receivedEvents.get(Event.NODE_REMOVED);
        assertNotNull(receivedPaths);
        assertEquals(expectedEventCount, receivedPaths.size());

        assertTrue(receivedPaths.contains("/testRoot/monitoring/dir3/simple.json"));
        assertTrue(receivedPaths.contains("/testRoot/monitoring/dir3/simple.txt"));
        assertTrue(receivedPaths.contains("/testRoot/monitoring/dir3"));
    }

    @Test
    @FixFor( "MODE-2252" )
    public void shouldStoreFilesOnMultiplePagesInWritableProjection() throws Exception {
        int count = FileSystemConnector.DEFAULT_PAGE_SIZE + 1;
        for (int i = 1; i <= count; i++) {
            String actualContent = "This is the content of the file "  + i;
            String filePath = "dir3/newFile_" + i + ".txt";
            tools.uploadFile(session, "/testRoot/store/" + filePath, new ByteArrayInputStream(actualContent.getBytes()));
            session.save();

            // Make sure the file on the file system contains what we put in ...
            assertFileContains(storeProjection, filePath, actualContent.getBytes());

            // Make sure that we can re-read the binary content via JCR ...
            Node contentNode = session.getNode("/testRoot/store/" + filePath + "/jcr:content");
            Binary value = (Binary)contentNode.getProperty("jcr:data").getBinary();
            assertBinaryContains(value, actualContent.getBytes());
        }
        assertTrue(((Node)session.getNode("/testRoot/store/dir3")).getNodes().getSize() >= count);
    }

    @Test
    @FixFor( "MODE-2254" )
    public void shouldSupportVariousPropertyTypesInJsonSidecar() throws Exception {
        Node file = session.getNode("/testRoot/json/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("string1", "extraValue");
        file.setProperty("string2", "111111111111111111111");
        file.setProperty("boolean", true);
        file.setProperty("double", 12.4);
        file.setProperty("decimal", BigDecimal.valueOf(12.4));
        file.setProperty("long", 12l);
        Calendar now = Calendar.getInstance();
        file.setProperty("date", now);

        session.save();
        assertThat(file.getProperty("string1").getString(), is("extraValue"));
        assertThat(file.getProperty("string2").getString(), is("111111111111111111111"));
        assertThat(file.getProperty("boolean").getBoolean(), is(true));
        assertThat(file.getProperty("double").getDouble(), is(12.4));
        assertThat(file.getProperty("decimal").getDecimal(), is(BigDecimal.valueOf(12.4)));
        assertThat(file.getProperty("long").getLong(), is(12l));
        assertThat(file.getProperty("date").getDate(), is(now));

        assertJsonSidecarFile(jsonProjection, "dir3/simple.json");
    }

    @Test
    @FixFor( "MODE-2255" )
    public void shouldQueryRepositoryManagedContent() throws Exception {
        Node file = session.getNode("/testRoot/store/dir3/simple.json");
        assertNotNull(file);
        //query for a file created during startup via a projection
        QueryManager queryManager = jcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(
                "SELECT file.[jcr:path] FROM [nt:file] as file where file.[jcr:path] = '/testRoot/store/dir3/simple.json'",
                Query.JCR_SQL2);
        NodeIterator nodesIterator = query.execute().getNodes();
        assertEquals(1, nodesIterator.getSize());
        assertEquals("/testRoot/store/dir3/simple.json", nodesIterator.nextNode().getPath());

        //update the file with a custom property and search again
        file.addMixin("flex:anyProperties");
        file.setProperty("customProp", "customValue");
        session.save();

        query = queryManager.createQuery(
                "SELECT node.[jcr:path] FROM [flex:anyProperties] as node where node.customProp = 'customValue'",
                Query.JCR_SQL2);
        nodesIterator = query.execute().getNodes();
        assertEquals(1, nodesIterator.getSize());
        assertEquals("/testRoot/store/dir3/simple.json", nodesIterator.nextNode().getPath());

        //add a new folder and search for it
        Node folder =  session.getNode("/testRoot/store/dir3");
        folder.addNode("sub_dir_3", "nt:folder");
        session.save();
        query = queryManager.createQuery(
                "SELECT folder.[jcr:path] FROM [nt:folder] as folder where folder.[jcr:path] = '/testRoot/store/dir3/sub_dir_3'",
                Query.JCR_SQL2);
        nodesIterator = query.execute().getNodes();
        assertEquals(1, nodesIterator.getSize());
        assertEquals("/testRoot/store/dir3/sub_dir_3", nodesIterator.nextNode().getPath());
    }

    @Test
    @FixFor( "MODE-2255" )
    public void shouldQueryExternallyManagedContent() throws Exception {
        //we need to use a projection which has monitoring enabled...
        File rootFolder = new File("target/federation/monitoring");
        assertTrue(rootFolder.exists() && rootFolder.isDirectory());

        //register a repo listener
        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        int expectedEventCount = 1;
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        FSListener listener = new FSListener(latch);
        observationManager.addEventListener(listener, Event.NODE_ADDED, "/testRoot/monitoring", true,
                                            null, null, false);
        //add a file externally
        addFile(rootFolder, "some_file", "data/simple.json");
        //sleep to make sure events are fired
        Thread.sleep(300);

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Events not received from connector");
        }

        //query for the newly added file
        QueryManager queryManager = jcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(
                "SELECT file.[jcr:path] FROM [nt:file] as file where file.[jcr:path] = '/testRoot/monitoring/some_file'",
                Query.JCR_SQL2);
        NodeIterator nodesIterator = query.execute().getNodes();
        assertEquals(1, nodesIterator.getSize());
        assertEquals("/testRoot/monitoring/some_file", nodesIterator.nextNode().getPath());
    }
    
    @Test
    @FixFor( "MODE-2413 " )
    public void shouldExportAndImportSystemViewWithExistingFederatedData() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        session.exportSystemView("/testRoot/store", baos, false, false);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        // import via workspace ...
        jcrSession().getWorkspace().importXML("/testRoot/store", bais, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
    }

    protected void assertNoSidecarFile( Projection projection,
                                        String filePath ) {
        assertThat(projection.getTestFile(filePath + JsonSidecarExtraPropertyStore.DEFAULT_EXTENSION).exists(), is(false));
        assertThat(projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION).exists(), is(false));
        assertThat(projection.getTestFile(filePath + JsonSidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION).exists(),
                   is(false));
        assertThat(projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION).exists(),
                   is(false));
    }

    protected void assertJsonSidecarFile( Projection projection,
                                          String filePath ) {
        File sidecarFile = projection.getTestFile(filePath + JsonSidecarExtraPropertyStore.DEFAULT_EXTENSION);
        if (sidecarFile.exists()) return;
        sidecarFile = projection.getTestFile(filePath + JsonSidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION);
        assertThat(sidecarFile.exists(), is(true));
    }

    protected void assertFileContains( Projection projection,
                                       String filePath,
                                       InputStream expectedContent ) throws IOException {
        assertFileContains(projection, filePath, IoUtil.readBytes(expectedContent));
    }

    protected void assertFileContains( Projection projection,
                                       String filePath,
                                       byte[] expectedContent ) throws IOException {
        File contentFile = projection.getTestFile(filePath);
        assertThat(contentFile.exists(), is(true));
        byte[] actual = IoUtil.readBytes(contentFile);
        assertThat(actual, is(expectedContent));
    }

    protected void assertBinaryContains( Binary binaryValue,
                                         byte[] expectedContent ) throws IOException, RepositoryException {
        byte[] actual = IoUtil.readBytes(binaryValue.getStream());
        assertThat(actual, is(expectedContent));
    }

    protected void assertLegacySidecarFile( Projection projection,
                                            String filePath ) {
        File sidecarFile = projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION);
        if (sidecarFile.exists()) return;
        sidecarFile = projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION);
        assertThat(sidecarFile.exists(), is(true));
    }

    protected void assertFolder( Node node,
                                 File dir ) throws Exception {
        assertThat(dir.exists(), is(true));
        assertThat(dir.canRead(), is(true));
        assertThat(dir.isDirectory(), is(true));
        assertThat(node.getName(), is(dir.getName()));
        assertThat(node.getIndex(), is(1));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
        assertThat(node.getProperty("jcr:created").getLong(), is(createdTimeFor(dir)));
    }

    protected long createdTimeFor( File file ) throws IOException {
        Path path = java.nio.file.Paths.get(file.toURI());
        BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        return basicFileAttributes.creationTime().toMillis();
    }

    protected void assertFile( Node node,
                               File file ) throws Exception {
        assertThat(node.getName(), is(file.getName()));
        assertThat(node.getIndex(), is(1));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:file"));
        assertThat(node.getProperty("jcr:created").getLong(), is(createdTimeFor(file)));
        Node content = node.getNode("jcr:content");
        assertThat(content.getName(), is("jcr:content"));
        assertThat(content.getIndex(), is(1));
        assertThat(content.getPrimaryNodeType().getName(), is("nt:resource"));
        assertThat(content.getProperty("jcr:lastModified").getLong(), is(file.lastModified()));
    }

    private void assertPathNotFound( String path ) throws Exception {
        try {
            session.getNode(path);
            fail(path + " was found, even though it shouldn't have been");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected static void addFile( File directory,
                                   String path,
                                   String contentFile ) throws IOException {
        File file = new File(directory, path);
        IoUtil.write(FileSystemConnectorTest.class.getClassLoader().getResourceAsStream(contentFile), new FileOutputStream(file));
    }

    private class FSListener implements EventListener {

        private final CountDownLatch latch;
        private Map<Integer, List<String>> receivedEventTypeAndPaths;

        protected FSListener( CountDownLatch latch ) {
            this.latch = latch;
            this.receivedEventTypeAndPaths = new HashMap<>();
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    Event event = events.nextEvent();
                    int type = event.getType();

                    List<String> paths = receivedEventTypeAndPaths.get(type);
                    if (paths == null) {
                        paths = new ArrayList<>();
                        receivedEventTypeAndPaths.put(type, paths);
                    }
                    printMessage("Received event of type: " + type + " on path:" + event.getPath());
                    paths.add(event.getPath());
                    latch.countDown();
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        protected Map<Integer, List<String>> getReceivedEventTypeAndPaths() {
            return receivedEventTypeAndPaths;
        }
    }

    @Immutable
    protected class Projection {
        protected final File directory;
        private final String name;

        public Projection( String name,
                           String directoryPath ) {
            this.name = name;
            this.directory = new File(directoryPath);
        }

        public String getName() {
            return name;
        }

        public void create( Node parentNode,
                            String childName ) throws RepositoryException {
            Session session = (Session)parentNode.getSession();
            FederationManager fedMgr = session.getWorkspace().getFederationManager();
            fedMgr.createProjection(parentNode.getPath(), getName(), "/", childName);
        }

        public void initialize() throws IOException {
            if (directory.exists()) FileUtil.delete(directory);
            directory.mkdirs();
            // Make some content ...
            new File(directory, "dir1").mkdir();
            new File(directory, "dir2").mkdir();
            new File(directory, "dir3").mkdir();
            File simpleJson = new File(directory, "dir3/simple.json");
            IoUtil.write(getClass().getClassLoader().getResourceAsStream("data/simple.json"), new FileOutputStream(simpleJson));
            File simpleTxt = new File(directory, "dir3/simple.txt");
            IoUtil.write(TEXT_CONTENT, new FileOutputStream(simpleTxt));
        }

        public void delete() {
            if (directory.exists()) FileUtil.delete(directory);
        }

        public File getTestFile( String relativePath ) {
            return new File(directory, relativePath);
        }

        public void testContent( Node federatedNode,
                                 String childName ) throws Exception {
            Session session = (Session)federatedNode.getSession();
            String path = federatedNode.getPath() + "/" + childName;

            Node files = session.getNode(path);
            assertThat(files.getName(), is(childName));
            assertThat(files.getPrimaryNodeType().getName(), is("nt:folder"));
            Node dir1 = session.getNode(path + "/dir1");
            Node dir2 = session.getNode(path + "/dir2");
            Node dir3 = session.getNode(path + "/dir3");
            Node simpleJson = session.getNode(path + "/dir3/simple.json");
            Node simpleText = session.getNode(path + "/dir3/simple.txt");
            assertFolder(dir1, getTestFile("dir1"));
            assertFolder(dir2, getTestFile("dir2"));
            assertFolder(dir3, getTestFile("dir3"));
            assertFile(simpleJson, getTestFile("dir3/simple.json"));
            assertFile(simpleText, getTestFile("dir3/simple.txt"));

            // Look up a node by identifier ...
            String externalNodeId = simpleJson.getIdentifier();
            Node simpleJson2 = session.getNodeByIdentifier(externalNodeId);
            assertFile(simpleJson2, getTestFile("dir3/simple.json"));

            // Look up the node again by path ...
            Node simpleJson3 = session.getNode(path + "/dir3/simple.json");
            assertFile(simpleJson3, getTestFile("dir3/simple.json"));

            // Look for a node that isn't there ...
            try {
                session.getNode(path + "/dir3/non-existant.oops");
                fail("Should not have been able to find a non-existing file");
            } catch (PathNotFoundException e) {
                // expected
            }
        }

        @Override
        public String toString() {
            return "Projection: " + name + " (at '" + directory.getAbsolutePath() + "')";
        }
    }

    protected class PagedProjection extends Projection {

        public PagedProjection( String name,
                                String directoryPath ) {
            super(name, directoryPath);
        }

        @Override
        public void testContent( Node federatedNode,
                                 String childName ) throws RepositoryException {
            Session session = (Session)federatedNode.getSession();
            String path = federatedNode.getPath() + "/" + childName;

            assertFolder(session, path, "dir1", "dir2", "dir3", "dir4", "dir5");
            assertFolder(session, path + "/dir1", "simple1.json", "simple2.json", "simple3.json", "simple4.json", "simple5.json",
                         "simple6.json");
            assertFolder(session, path + "/dir2", "simple1.json", "simple2.json");
            assertFolder(session, path + "/dir3", "simple1.json");
            assertFolder(session, path + "/dir4", "simple1.json", "simple2.json", "simple3.json");
            assertFolder(session, path + "/dir5", "simple1.json", "simple2.json", "simple3.json", "simple4.json", "simple5.json");
        }

        private void assertFolder( Session session,
                                   String path,
                                   String... childrenNames ) throws RepositoryException {
            Node folderNode = session.getNode(path);
            assertThat(folderNode.getPrimaryNodeType().getName(), is("nt:folder"));
            List<String> expectedChildren = new ArrayList<String>(Arrays.asList(childrenNames));

            NodeIterator nodes = folderNode.getNodes();
            assertEquals(expectedChildren.size(), nodes.getSize());
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String nodeName = node.getName();
                assertTrue(expectedChildren.contains(nodeName));
                expectedChildren.remove(nodeName);
            }
        }

        @Override
        public void initialize() throws IOException {
            if (directory.exists()) FileUtil.delete(directory);
            directory.mkdirs();
            // Make some content ...
            new File(directory, "dir1").mkdir();
            addFile(directory, "dir1/simple1.json", "data/simple.json");
            addFile(directory, "dir1/simple2.json", "data/simple.json");
            addFile(directory, "dir1/simple3.json", "data/simple.json");
            addFile(directory, "dir1/simple4.json", "data/simple.json");
            addFile(directory, "dir1/simple5.json", "data/simple.json");
            addFile(directory, "dir1/simple6.json", "data/simple.json");

            new File(directory, "dir2").mkdir();
            addFile(directory, "dir2/simple1.json", "data/simple.json");
            addFile(directory, "dir2/simple2.json", "data/simple.json");

            new File(directory, "dir3").mkdir();
            addFile(directory, "dir3/simple1.json", "data/simple.json");

            new File(directory, "dir4").mkdir();
            addFile(directory, "dir4/simple1.json", "data/simple.json");
            addFile(directory, "dir4/simple2.json", "data/simple.json");
            addFile(directory, "dir4/simple3.json", "data/simple.json");

            new File(directory, "dir5").mkdir();
            addFile(directory, "dir5/simple1.json", "data/simple.json");
            addFile(directory, "dir5/simple2.json", "data/simple.json");
            addFile(directory, "dir5/simple3.json", "data/simple.json");
            addFile(directory, "dir5/simple4.json", "data/simple.json");
            addFile(directory, "dir5/simple5.json", "data/simple.json");
        }
    }

    protected class LargeFilesProjection extends Projection {

        public LargeFilesProjection( String name,
                                     String directoryPath ) {
            super(name, directoryPath);
        }

        @Override
        public void testContent( Node federatedNode,
                                 String childName ) throws RepositoryException {
            Session session = (Session)federatedNode.getSession();
            String path = federatedNode.getPath() + "/" + childName;
            assertFolder(session, path, "large-file1.png");
        }

        private void assertFolder( Session session,
                                   String path,
                                   String... childrenNames ) throws RepositoryException {
            Node folderNode = session.getNode(path);
            assertThat(folderNode.getPrimaryNodeType().getName(), is("nt:folder"));
            List<String> expectedChildren = new ArrayList<String>(Arrays.asList(childrenNames));

            NodeIterator nodes = folderNode.getNodes();
            assertEquals(expectedChildren.size(), nodes.getSize());
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String nodeName = node.getName();
                assertTrue(expectedChildren.contains(nodeName));
                expectedChildren.remove(nodeName);
            }
        }

        @Override
        public void initialize() throws IOException {
            if (directory.exists()) FileUtil.delete(directory);
            directory.mkdirs();
            // Make some content ...
            addFile("large-file1.png", "data/large-file1.png");
        }

        private void addFile( String path,
                              String contentFile ) throws IOException {
            File file = new File(directory, path);
            IoUtil.write(getClass().getClassLoader().getResourceAsStream(contentFile), new FileOutputStream(file));
        }
    }
}
