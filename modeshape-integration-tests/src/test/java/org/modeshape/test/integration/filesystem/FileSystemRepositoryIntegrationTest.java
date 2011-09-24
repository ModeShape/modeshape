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
package org.modeshape.test.integration.filesystem;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.Future;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.test.ModeShapeSingleUseTest;

public class FileSystemRepositoryIntegrationTest extends ModeShapeSingleUseTest {

    protected static final String README_RESOURCE_PATH = "svn/local_repos/dummy_svn_repos/README.txt";
    protected static final String MODETEST_URL = "http://modeshape.org/test";

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeSingleUseTest#beforeEach()
     */
    @Override
    public void beforeEach() throws Exception {
        // Delete the directory used for the FS store ...
        FileUtil.delete("target/fsRepoWithProps");

        // Now make the root directory ...
        new File("target/fsRepoWithProps/root").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder2").mkdirs();

        super.beforeEach();
    }

    @Test
    public void shouldAllowCreatingReferenceToContentNode() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");

        // Make sure the required mixins are registered ...
        addPointableMixin();

        // Create the nodes ...
        Session session = session();
        Node root = session.getRootNode();
        root.addNode("A", "nt:folder");
        Node meta = root.addNode("meta", "nt:folder");
        meta.addMixin("modetest:pointable");
        Node readme = uploadFile(README_RESOURCE_PATH, "/A");

        // Now create a reference on '/meta' to the '/A/README.txt/jcr:content' node.
        Node content = readme.getNode("jcr:content");
        content.addMixin("mix:referenceable");
        meta.setProperty("contentPointer", content);

        // Resolve the node before saving, since resolution can use the transient state of the session ...
        Node referenced = meta.getProperty("contentPointer").getNode();
        assertThat(referenced.getPath(), is(content.getPath()));
        assertThat(referenced, is(content));

        // Now save ...
        session.save();

        logout();

        // Get another session and verify the reference exists and can be resolved ...
        session = session();
        root = session.getRootNode();
        meta = root.getNode("meta");
        content = root.getNode("A/README.txt/jcr:content");

        referenced = meta.getProperty("contentPointer").getNode();
        assertThat(referenced.getPath(), is(content.getPath()));
        assertThat(referenced, is(content));
    }

    @FixFor( "MODE-1221" )
    @Test
    public void shouldFindAllNodesWhenQueryingContent() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");

        // Create the nodes ...
        Session session = session();
        Node root = session.getRootNode();
        root.addNode("A", "nt:folder");
        root.addNode("meta", "nt:folder");
        Node readme = uploadFile(README_RESOURCE_PATH, "/A");

        // Now create a reference on '/meta' to the '/A/README.txt/jcr:content' node.
        Node content = readme.getNode("jcr:content");
        content.addMixin("mix:referenceable");

        // Now save ...
        session.save();

        logout();

        // Get another session and verify the reference exists and can be resolved ...
        session = session();
        // print = true;
        printQuery("SELECT * FROM [nt:base]", 7L, Collections.<String, String>emptyMap());
    }

    /*
     * The test verifies that maxPathLength is being overridden in the configuration
     * by setting to something small and should result in a <code>RepositoryException</code> 
     * being thrown to confirm it was overriding the 255 default.
     */
    @FixFor( "MODE-1249" )
    @Test( expected = javax.jcr.RepositoryException.class )
    public void shouldThrowExceptionIfPathExceedsMaxLength() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemPropValidation.xml");

        // Create the nodes ...
        Session session = session();
        Node root = session.getRootNode();
        root.addNode("A", "nt:folder");
        root.addNode("meta", "nt:folder");
        Node readme = uploadFile(README_RESOURCE_PATH, "/A");

        // Now create a reference on '/meta' to the '/A/README.txt/jcr:content' node.
        Node content = readme.getNode("jcr:content");
        content.addMixin("mix:referenceable");

        // Now save ...
        session.save();

        logout();

    }

    @FixFor( "MODE-1263" )
    @Test
    public void shouldIncludeExistingContent() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");
        Session session = session();

        // Create a new Node (after session is started)
        session.getRootNode().addNode("NewFolder", "nt:folder");
        session.save();

        // Query all nodes
        // print = true;
        printQuery("SELECT * FROM [nt:base]", 4L, Collections.<String, String>emptyMap());

        logout();
    }

    @FixFor( "MODE-1269" )
    @Test
    public void shouldAllowReindexingAllContent() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");
        Session session = session();

        // Reindex all content synchronously ...
        org.modeshape.jcr.api.Workspace workspace = (org.modeshape.jcr.api.Workspace)session.getWorkspace();
        workspace.reindex();

        printQuery("SELECT * FROM [nt:base]", 3L, Collections.<String, String>emptyMap());

        // Add a folder to the file system ...
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1/file.txt").createNewFile();

        // Query should show old results (because file system connector does not monitor file system) ...
        printQuery("SELECT * FROM [nt:base]", 3L, Collections.<String, String>emptyMap());

        // Reindex all content synchronously ...
        workspace.reindex();

        printQuery("SELECT * FROM [nt:base]", 6L, Collections.<String, String>emptyMap());

        logout();
    }

    @FixFor( "MODE-1269" )
    @Test
    public void shouldAllowReindexingAllContentAsynchronously() throws Exception {
        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");
        Session session = session();

        // Reindex all content synchronously ...
        org.modeshape.jcr.api.Workspace workspace = (org.modeshape.jcr.api.Workspace)session.getWorkspace();
        Future<Boolean> future = workspace.reindexAsync();
        assertThat(future.get(), is(true)); // blocks

        printQuery("SELECT * FROM [nt:base]", 3L, Collections.<String, String>emptyMap());

        // Add a folder to the file system ...
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1/file.txt").createNewFile();

        // Query should show old results (because file system connector does not monitor file system) ...
        printQuery("SELECT * FROM [nt:base]", 3L, Collections.<String, String>emptyMap());

        // Reindex all content synchronously ...
        future = workspace.reindexAsync();
        assertThat(future.get(), is(true)); // blocks

        printQuery("SELECT * FROM [nt:base]", 6L, Collections.<String, String>emptyMap());

        logout();
    }

    @FixFor( "MODE-1269" )
    @Test
    public void shouldAllowReindexingSubgraph() throws Exception {
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/file.txt").createNewFile();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder2").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1/file.txt").createNewFile();

        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");
        Session session = session();

        // Reindex some of the content synchronously ...
        org.modeshape.jcr.api.Workspace workspace = (org.modeshape.jcr.api.Workspace)session.getWorkspace();
        workspace.reindex("/folder1");

        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 9L, Collections.<String, String>emptyMap());

        // Add a folder to the file system ...
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder3").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder3/file3.txt").createNewFile();

        // Query should show old results (because file system connector does not monitor file system) ...
        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 9L, Collections.<String, String>emptyMap());

        // Reindex some content synchronously ...
        workspace.reindex("/folder1");
        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 12L, Collections.<String, String>emptyMap());

        File newName = new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder4");
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder3").renameTo(newName);

        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 12L, Collections.<String, String>emptyMap());

        // Reindex some content synchronously (now deep enough to get everything) ...
        workspace.reindex("/folder1");
        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 12L, Collections.<String, String>emptyMap());

        logout();
    }

    @FixFor( "MODE-1269" )
    @Test
    public void shouldAllowReindexingSubgraphAsynchronously() throws Exception {
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/file.txt").createNewFile();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder2").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder1/file.txt").createNewFile();

        startEngineUsing("config/configRepositoryForFileSystemWithExtraProperties.xml");
        Session session = session();

        // Reindex some of the content synchronously ...
        org.modeshape.jcr.api.Workspace workspace = (org.modeshape.jcr.api.Workspace)session.getWorkspace();
        Future<Boolean> future = workspace.reindexAsync("/folder1");
        assertThat(future.get(), is(true)); // blocks

        printQuery("SELECT * FROM [nt:base]", 9L, Collections.<String, String>emptyMap());

        // Add a folder to the file system ...
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder3").mkdirs();
        new File("target/fsRepoWithProps/root/defaultWorkspace/folder1/subfolder3/file3.txt").createNewFile();

        // Query should show old results (because file system connector does not monitor file system) ...
        printQuery("SELECT * FROM [nt:base]", 9L, Collections.<String, String>emptyMap());

        // Reindex some content synchronously (now deep enough to get everything) ...
        future = workspace.reindexAsync("/folder1");
        assertThat(future.get(), is(true)); // blocks
        printQuery("SELECT * FROM [nt:base]", 12L, Collections.<String, String>emptyMap());

        logout();
    }

    protected void registryNamespaceIfMissing( String prefix,
                                               String url ) throws RepositoryException {
        NamespaceRegistry registry = session().getWorkspace().getNamespaceRegistry();
        try {
            registry.getURI(prefix);
        } catch (NamespaceException e) {
            registry.registerNamespace(prefix, url);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void addPointableMixin() throws RepositoryException {
        registryNamespaceIfMissing("modetest", MODETEST_URL);
        String typeName = "modetest:pointable";
        NodeTypeManager mgr = session().getWorkspace().getNodeTypeManager();
        try {
            mgr.getNodeType(typeName);
        } catch (NoSuchNodeTypeException e) {
            PropertyDefinitionTemplate anySinglePropertyDefn = mgr.createPropertyDefinitionTemplate();
            anySinglePropertyDefn.setName("*");
            anySinglePropertyDefn.setRequiredType(PropertyType.REFERENCE);
            anySinglePropertyDefn.setMultiple(false);

            NodeTypeTemplate type = mgr.createNodeTypeTemplate();
            type.setName(typeName);
            type.setMixin(true);
            type.getPropertyDefinitionTemplates().add(anySinglePropertyDefn);

            mgr.registerNodeType(type, false);
        }
    }
}
