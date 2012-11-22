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

package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FileSystemConnectorTest extends SingleUseAbstractTest {

    protected static final String PATH_TO_FILESYSTEM_SOURCE = "target/federation/files";
    protected static final String FILESYSTEM_SOURCE_NAME = "file-resources";
    protected static final String TEXT_CONTENT = "Some text content";

    private Node testRoot;
    private Projection readOnlyProjection;
    private Projection storeProjection;
    private Projection jsonProjection;
    private Projection legacyProjection;
    private Projection noneProjection;
    private Projection[] projections;

    @Before
    public void before() throws Exception {
        readOnlyProjection = new Projection("readonly-files", "target/federation/files-read");
        storeProjection = new Projection("mutable-files-store", "target/federation/files-store");
        jsonProjection = new Projection("mutable-files-json", "target/federation/files-json");
        legacyProjection = new Projection("mutable-files-legacy", "target/federation/files-legacy");
        noneProjection = new Projection("mutable-files-none", "target/federation/files-none");

        projections = new Projection[] {readOnlyProjection, storeProjection, jsonProjection, legacyProjection, noneProjection};

        // Remove and then make the directory for our federation test ...
        for (Projection projection : projections) {
            projection.initialize();
        }

        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-filesystem-federation.json"));
        registerNodeTypes("cnd/flex.cnd");

        Session session = (Session)jcrSession();
        testRoot = session.getRootNode().addNode("testFilesRoot");
        testRoot = session.getRootNode().addNode("testRoot");
        testRoot.addNode("node1");
        session.save();

        readOnlyProjection.create(testRoot, "readonly");
        storeProjection.create(testRoot, "store");
        jsonProjection.create(testRoot, "json");
        legacyProjection.create(testRoot, "legacy");
        noneProjection.create(testRoot, "none");
    }

    @Test
    public void shouldReadNodesInAllProjections() throws Exception {
        readOnlyProjection.testContent(testRoot, "readonly");
        storeProjection.testContent(testRoot, "store");
        jsonProjection.testContent(testRoot, "json");
        legacyProjection.testContent(testRoot, "legacy");
        noneProjection.testContent(testRoot, "none");
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
    public void shouldAllowUpdatingNodesInWritableJsonBasedProjection() throws Exception {
        Node file = session.getNode("/testRoot/json/dir3/simple.json");
        file.addMixin("flex:anyProperties");
        file.setProperty("extraProp", "extraValue");
        session.save();
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

    protected void assertNoSidecarFile( Projection projection,
                                        String filePath ) {
        assertThat(projection.getTestFile(filePath + JsonSidecarExtraPropertyStore.DEFAULT_EXTENSION).exists(), is(false));
        assertThat(projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION).exists(),
                   is(false));
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

    protected void assertLegacySidecarFile( Projection projection,
                                            String filePath ) {
        File sidecarFile = projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION);
        if (sidecarFile.exists()) return;
        sidecarFile = projection.getTestFile(filePath + LegacySidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION);
        assertThat(sidecarFile.exists(), is(true));
    }

    protected void assertFolder( Node node,
                                 File dir ) throws RepositoryException {
        assertThat(dir.exists(), is(true));
        assertThat(dir.canRead(), is(true));
        assertThat(dir.isDirectory(), is(true));
        assertThat(node.getName(), is(dir.getName()));
        assertThat(node.getIndex(), is(1));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
        assertThat(node.getProperty("jcr:created").getLong(), is(dir.lastModified()));
    }

    protected void assertFile( Node node,
                               File file ) throws RepositoryException {
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

    @Immutable
    protected class Projection {
        private final String name;
        private final File directory;

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
            fedMgr.createExternalProjection(parentNode.getPath(), getName(), "/", childName);
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
                                 String childName ) throws RepositoryException {
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

}
