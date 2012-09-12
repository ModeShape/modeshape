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
package org.modeshape.test.ri;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class VersioningTest extends AbstractTest {

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        startTransientRepository("jackrabbitInMemoryBundleTestRepositoryConfig.xml");
    }

    @Test( expected = RepositoryException.class )
    public void shouldFailToGetVersionHistoryOfTransientNode() throws Exception {
        VersionManager vm = session().getWorkspace().getVersionManager();
        Node node = session().getRootNode().addNode("my node", "nt:unstructured");
        node.addMixin("mix:versionable");
        vm.getVersionHistory(node.getPath());
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldFailToGetVersionHistoryOfPersistentNonVersionableNodeThatWasTransientlyMadeVersionable() throws Exception {
        Node node = session().getRootNode().addNode("my node", "nt:unstructured");
        VersionManager vm = session().getWorkspace().getVersionManager();
        session().save();
        node.addMixin("mix:versionable");
        vm.getVersionHistory(node.getPath());
    }

    @Test
    public void shouldHaveSinglePredecessorAndSuccessorValues() throws Exception {
        print = true;

        Node node = session().getRootNode().addNode("my node", "nt:unstructured");
        node.addMixin("mix:versionable");
        session().save();
        checkin(node);

        print("Version history after initial checkin");
        printVersionHistory(node);

        checkout(node);
        node.setProperty("extraProperty", "hello");
        session().save();
        checkin(node);

        print("Version history after second checkin");
        printVersionHistory(node);

    }

    @Test
    public void shouldRestoreWithReplacingNodeWithCopyOpvAndNonReferenceableChildren() throws Exception {
        loadNodeTypes("version-test-nodetypes.cnd");
        verifyNodeTypeExists("modetest:versionTest");

        Session session = session();

        Node node = session.getRootNode().addNode("checkInTest", "modetest:versionTest");
        session.save();

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:title");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:title"));

        Version version = checkin(copyNode);

        printSubgraph(version);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        printVersionHistory(copyNode);

        restore(copyNode, version, true);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:title"));
    }

    @Test
    public void shouldRestoreWithoutReplacingNodeWithCopyOpvAndNonReferenceableChildren() throws Exception {
        loadNodeTypes("version-test-nodetypes.cnd");
        verifyNodeTypeExists("modetest:versionTest");

        Session session = session();

        Node node = session.getRootNode().addNode("checkInTest", "modetest:versionTest");
        session.save();

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:title");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:title"));

        Version version = checkin(copyNode);

        printSubgraph(version);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        printVersionHistory(copyNode);

        restore(copyNode, version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:title"));
    }

    @Test
    public void shouldRestoreWithReplacingNodeWithCopyOpvAndReferenceableChildren() throws Exception {
        loadNodeTypes("version-test-nodetypes.cnd");
        verifyNodeTypeExists("modetest:versionTest");

        Session session = session();

        Node node = session.getRootNode().addNode("checkInTest", "modetest:versionTest");
        session.save();

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:referenceable");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
        String belowCopyNodeId = belowCopyNode.getIdentifier();

        Version version = checkin(copyNode);

        printSubgraph(version);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        printVersionHistory(copyNode);

        restore(copyNode, version, true);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        String belowCopyNode2Id = belowCopyNode.getIdentifier();
        assertThat(belowCopyNodeId, is(belowCopyNode2Id));
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
    }

    @Test
    public void shouldRestoreWithoutReplacingNodeWithCopyOpvAndReferenceableChildren() throws Exception {
        loadNodeTypes("version-test-nodetypes.cnd");
        verifyNodeTypeExists("modetest:versionTest");

        Session session = session();

        Node node = session.getRootNode().addNode("checkInTest", "modetest:versionTest");
        session.save();

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:referenceable");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
        String belowCopyNodeId = belowCopyNode.getIdentifier();

        Version version = checkin(copyNode);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        print = true;
        printSubgraph(copyNode);
        printVersionHistory(copyNode);

        restore(copyNode, version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        String belowCopyNode2Id = belowCopyNode.getIdentifier();
        assertThat(belowCopyNodeId, is(belowCopyNode2Id));
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:referenceable"));
    }

    @Test
    public void testShouldRestoreWithNoReplaceTheNonReferenceableCopiedChildNode() throws Exception {
        loadNodeTypes("version-test-nodetypes.cnd");
        verifyNodeTypeExists("modetest:versionTest");

        Session session = session();

        Node node = session.getRootNode().addNode("checkInTest", "modetest:versionTest");
        session.save();

        /*
         * Create /checkinTest/copyNode with copyNode being versionable. This should be able
         * to be checked in, as the ABORT status of abortNode is ignored when copyNode is checked in.
         */

        Node copyNode = node.addNode("copyNode", "modetest:versionTest");
        copyNode.addMixin("mix:versionable");
        copyNode.setProperty("copyProp", "copyPropValue");
        copyNode.setProperty("ignoreProp", "ignorePropValue");
        copyNode.setProperty("computeProp", "computePropValue");
        Node belowCopyNode = copyNode.addNode("copyNode", "nt:unstructured");
        belowCopyNode.addMixin("mix:title");
        belowCopyNode.setProperty("copyProp", "copyPropValue");
        belowCopyNode.setProperty("ignoreProp", "ignorePropValue");
        belowCopyNode.setProperty("computeProp", "computePropValue");
        belowCopyNode.setProperty("versionProp", "versionPropValue");
        session.save();

        assertThat(belowCopyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode.getMixinNodeTypes()[0].getName(), is("mix:title"));

        Version version = checkin(copyNode);

        /*
        * Make some changes
        */
        checkout(copyNode);
        copyNode.addMixin("mix:lockable");
        copyNode.setProperty("copyProp", "copyPropValueNew");
        copyNode.setProperty("ignoreProp", "ignorePropValueNew");
        copyNode.setProperty("versionProp", "versionPropValueNew");
        copyNode.setProperty("computeProp", "computePropValueNew");
        belowCopyNode.setProperty("versionProp", "versionPropValueNew");
        belowCopyNode.setProperty("computeProp", "computePropValueNew");
        session.save();
        checkin(copyNode);

        print = true;
        printSubgraph(copyNode);
        printVersionHistory(copyNode);

        restore(copyNode, version, false);

        assertThat(copyNode.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(copyNode.getProperty("ignoreProp").getString(), is("ignorePropValueNew"));
        assertThat(copyNode.getProperty("computeProp").getString(), is("computePropValueNew"));

        try {
            copyNode.getProperty("versionProp");
            fail("Property with OnParentVersionAction of VERSION added after version should be removed during restore");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        // Note that in the case of properties on copied subnodes of a restored node, they replace the nodes
        // in the workspace unless there is a node with the same identifier. See Section 15.7.6 for details.

        Node belowCopyNode2 = copyNode.getNode("copyNode");
        assertThat(belowCopyNode2.getProperty("copyProp").getString(), is("copyPropValue"));
        assertThat(belowCopyNode2.getProperty("ignoreProp").getString(), is("ignorePropValue"));
        assertThat(belowCopyNode2.getProperty("computeProp").getString(), is("computePropValue"));
        assertThat(belowCopyNode2.getProperty("versionProp").getString(), is("versionPropValue"));
        assertThat(belowCopyNode2.getMixinNodeTypes()[0].getName(), is("mix:title"));
    }

    @Test
    public void testRemovingVersionFromVersionHistory() throws Exception {
        print = false;

        Session session = session();
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node node = session().getRootNode().addNode("base version node", "nt:unstructured");
        session.save();

        Node outerNode = node.addNode("outerFolder");
        Node innerNode = outerNode.addNode("innerFolder");
        Node fileNode = innerNode.addNode("testFile.dat");
        fileNode.setProperty("jcr:mimeType", "text/plain");
        fileNode.setProperty("jcr:data", "Original content");
        session.save();

        fileNode.addMixin("mix:versionable");
        session.save();

        // Make several changes ...
        String path = fileNode.getPath();
        for (int i = 2; i != 7; ++i) {
            versionManager.checkout(path);
            fileNode.setProperty("jcr:data", "Original content " + i);
            session.save();
            versionManager.checkin(path);
        }

        // Get the version history ...
        VersionHistory history = versionManager.getVersionHistory(path);
        if (print) System.out.println("Before: \n" + history);
        assertThat(history, is(notNullValue()));
        assertThat(history.getAllLinearVersions().getSize(), is(6L));

        // Get the versions ...
        VersionIterator iter = history.getAllLinearVersions();
        Version v1 = iter.nextVersion();
        Version v2 = iter.nextVersion();
        Version v3 = iter.nextVersion();
        Version v4 = iter.nextVersion();
        Version v5 = iter.nextVersion();
        Version v6 = iter.nextVersion();
        assertThat(iter.hasNext(), is(false));
        String versionName = v3.getName();
        assertThat(v1, is(notNullValue()));
        assertThat(v2, is(notNullValue()));
        assertThat(v3, is(notNullValue()));
        assertThat(v4, is(notNullValue()));
        assertThat(v5, is(notNullValue()));
        assertThat(v6, is(notNullValue()));

        // Remove the 3rd version (that is, i=3) ...
        history.removeVersion(versionName);

        if (print) System.out.println("After (same history used to remove): \n" + history);
        assertThat(history.getAllLinearVersions().getSize(), is(5L));

        // Get the versions using the history node we already have ...
        iter = history.getAllLinearVersions();
        Version v1a = iter.nextVersion();
        Version v2a = iter.nextVersion();
        Version v4a = iter.nextVersion();
        Version v5a = iter.nextVersion();
        Version v6a = iter.nextVersion();
        assertThat(iter.hasNext(), is(false));
        assertThat(v1a.getName(), is(v1.getName()));
        assertThat(v2a.getName(), is(v2.getName()));
        assertThat(v4a.getName(), is(v4.getName()));
        assertThat(v5a.getName(), is(v5.getName()));
        assertThat(v6a.getName(), is(v6.getName()));

        // Get the versions using a fresh history node ...
        VersionHistory history2 = versionManager.getVersionHistory(path);
        if (print) System.out.println("After (fresh history): \n" + history2);
        assertThat(history.getAllLinearVersions().getSize(), is(5L));

        iter = history2.getAllLinearVersions();
        Version v1b = iter.nextVersion();
        Version v2b = iter.nextVersion();
        Version v4b = iter.nextVersion();
        Version v5b = iter.nextVersion();
        Version v6b = iter.nextVersion();
        assertThat(iter.hasNext(), is(false));
        assertThat(v1b.getName(), is(v1.getName()));
        assertThat(v2b.getName(), is(v2.getName()));
        assertThat(v4b.getName(), is(v4.getName()));
        assertThat(v5b.getName(), is(v5.getName()));
        assertThat(v6b.getName(), is(v6.getName()));
    }

    @Test
    public void testRemovingVersionFromVersionHistoryByRemovingVersionNode() throws Exception {
        print = false;

        Session session = session();
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node node = session().getRootNode().addNode("base version node", "nt:unstructured");
        session.save();

        Node outerNode = node.addNode("outerFolder");
        Node innerNode = outerNode.addNode("innerFolder");
        Node fileNode = innerNode.addNode("testFile.dat");
        fileNode.setProperty("jcr:mimeType", "text/plain");
        fileNode.setProperty("jcr:data", "Original content");
        session.save();

        fileNode.addMixin("mix:versionable");
        session.save();

        // Make several changes ...
        String path = fileNode.getPath();
        for (int i = 2; i != 7; ++i) {
            versionManager.checkout(path);
            fileNode.setProperty("jcr:data", "Original content " + i);
            session.save();
            versionManager.checkin(path);
        }

        // Get the version history ...
        VersionHistory history = versionManager.getVersionHistory(path);
        if (print) System.out.println("Before: \n" + history);
        assertThat(history, is(notNullValue()));
        assertThat(history.getAllLinearVersions().getSize(), is(6L));

        // Get the versions ...
        VersionIterator iter = history.getAllLinearVersions();
        Version v1 = iter.nextVersion();
        Version v2 = iter.nextVersion();
        Version v3 = iter.nextVersion();
        Version v4 = iter.nextVersion();
        Version v5 = iter.nextVersion();
        Version v6 = iter.nextVersion();
        assertThat(iter.hasNext(), is(false));
        assertThat(v1, is(notNullValue()));
        assertThat(v2, is(notNullValue()));
        assertThat(v3, is(notNullValue()));
        assertThat(v4, is(notNullValue()));
        assertThat(v5, is(notNullValue()));
        assertThat(v6, is(notNullValue()));

        // Remove the 3rd version (that is, i=3) ...
        try {
            v3.remove();
            fail("Should not allow removing a protected node");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

}
