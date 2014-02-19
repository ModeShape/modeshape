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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * Unit test for versioning behaviour (see JSR_285#15)
 */
public class JcrVersioningTest extends SingleUseAbstractTest {

    private VersionManager versionManager;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        versionManager = session.getWorkspace().getVersionManager();
    }

    @Test
    @FixFor( "MODE-1302" )
    public void shouldHaveVersionHistoryWhenRefreshIsCalled() throws Exception {
        Node outerNode = session.getRootNode().addNode("outerFolder");
        Node innerNode = outerNode.addNode("innerFolder");
        Node fileNode = innerNode.addNode("testFile.dat");
        fileNode.setProperty("jcr:mimeType", "text/plain");
        fileNode.setProperty("jcr:data", "Original content");
        session.save();

        assertFalse(hasVersionHistory(fileNode));
        fileNode.addMixin("mix:versionable");
        // Version history is not created until save
        assertFalse(hasVersionHistory(fileNode));
        session.refresh(true);
        // Version history is not created until save
        assertFalse(hasVersionHistory(fileNode));
        session.save();
        assertTrue(hasVersionHistory(fileNode));
    }

    @Test
    @FixFor( "MODE-1401" )
    public void shouldAllowAddingUnderCheckedInNodeNewChildNodeWithOpvOfIgnore() throws Exception {
        registerNodeTypes(session, "cnd/versioning.cnd");

        // Set up parent node and check it in ...
        Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
        parent.setProperty("versionProp", "v");
        parent.setProperty("copyProp", "c");
        parent.setProperty("ignoreProp", "i");
        session.save();
        versionManager.checkin(parent.getPath());

        // Try to add child with OPV of ignore ...
        Node child = parent.addNode("nonVersionedIgnoredChild", "ver:nonVersionableChild");
        child.setProperty("copyProp", "c");
        child.setProperty("ignoreProp", "i");
        session.save();

        // Try to update the properties on the child with OPV of 'ignore'
        child.setProperty("copyProp", "c2");
        child.setProperty("ignoreProp", "i2");
        session.save();

        // Try to add versionable child with OPV of ignore ...
        Node child2 = parent.addNode("versionedIgnoredChild", "ver:versionableChild");
        child2.setProperty("copyProp", "c");
        child2.setProperty("ignoreProp", "i");
        session.save();

        // Try to update the properties on the child with OPV of 'ignore'
        child2.setProperty("copyProp", "c2");
        child2.setProperty("ignoreProp", "i2");
        session.save();
    }

    @Test
    @FixFor( "MODE-1401" )
    public void shouldNotAllowAddingUnderCheckedInNodeNewChildNodeWithOpvOfSomethingOtherThanIgnore() throws Exception {
        registerNodeTypes(session, "cnd/versioning.cnd");

        // Set up parent node and check it in ...
        Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
        parent.setProperty("versionProp", "v");
        parent.setProperty("copyProp", "c");
        parent.setProperty("ignoreProp", "i");
        session.save();
        versionManager.checkin(parent.getPath());

        // Try to add versionable child with OPV of not ignore ...
        try {
            parent.addNode("versionedChild", "ver:versionableChild");
            fail("should have failed");
        } catch (VersionException e) {
            // expected
        }

        // Try to add non-versionable child with OPV of not ignore ...
        try {
            parent.addNode("nonVersionedChild", "ver:nonVersionableChild");
            fail("should have failed");
        } catch (VersionException e) {
            // expected
        }
    }

    @Test
    @FixFor( "MODE-1401" )
    public void shouldAllowRemovingFromCheckedInNodeExistingChildNodeWithOpvOfIgnore() throws Exception {
        registerNodeTypes(session, "cnd/versioning.cnd");

        // Set up parent node and check it in ...
        Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
        parent.setProperty("versionProp", "v");
        parent.setProperty("copyProp", "c");
        parent.setProperty("ignoreProp", "i");

        Node child1 = parent.addNode("nonVersionedIgnoredChild", "ver:nonVersionableChild");
        child1.setProperty("copyProp", "c");
        child1.setProperty("ignoreProp", "i");

        Node child2 = parent.addNode("versionedIgnoredChild", "ver:versionableChild");
        child2.setProperty("copyProp", "c");
        child2.setProperty("ignoreProp", "i");

        session.save();
        versionManager.checkin(parent.getPath());

        // Should be able to change the properties on the ignored children
        child1.setProperty("copyProp", "c2");
        child1.setProperty("ignoreProp", "i2");
        child2.setProperty("copyProp", "c2");
        child2.setProperty("ignoreProp", "i2");
        session.save();

        // Try to remove the two child nodes that have an OPV of 'ignore' ...
        child1.remove();
        child2.remove();
        session.save();

        // Should be able to change the ignored properties on the checked-in parent ...
        parent.setProperty("ignoreProp", "i");

        // Should not be able to set any non-ignored properties on the checked in parent ...
        try {
            parent.setProperty("copyProp", "c2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }
        try {
            parent.setProperty("versionProp", "v2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }
    }

    @Test
    @FixFor( "MODE-1401" )
    public void shouldNotAllowRemovingFromCheckedInNodeExistingChildNodeWithOpvOfSomethingOtherThanIgnore() throws Exception {
        registerNodeTypes(session, "cnd/versioning.cnd");

        // Set up parent node and check it in ...
        Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
        parent.setProperty("versionProp", "v");
        parent.setProperty("copyProp", "c");
        parent.setProperty("ignoreProp", "i");

        Node child1 = parent.addNode("nonVersionedChild", "ver:nonVersionableChild");
        child1.setProperty("copyProp", "c");
        child1.setProperty("ignoreProp", "i");

        Node child2 = parent.addNode("versionedChild", "ver:versionableChild");
        child2.setProperty("copyProp", "c");
        child2.setProperty("ignoreProp", "i");

        session.save();
        versionManager.checkin(parent.getPath());
        versionManager.checkin(child2.getPath());

        // Should not be able to set any non-ignored properties on the checked in parent ...
        try {
            parent.setProperty("copyProp", "c2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }
        try {
            parent.setProperty("versionProp", "v2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }

        // Should not be able to set any non-ignored properties on the non-ignored children ...
        try {
            child2.setProperty("copyProp", "c2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }
        try {
            child1.setProperty("copyProp", "c2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }

        // Check out the versionable child node, and we should be able to edit it ...
        versionManager.checkout(child2.getPath());
        child2.setProperty("copyProp", "c3");
        session.save();
        versionManager.checkin(child2.getPath());

        // But we still cannot edit a property on the nonVersionable child node when the parent is still checked in ...
        try {
            child1.setProperty("copyProp", "c2");
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }

        // Check out the parent ...
        versionManager.checkout(parent.getPath());

        // Now we can change the properties on the non-versionable children ...
        child1.setProperty("copyProp", "c2");
        session.save();

        // And even remove it ...
        child1.remove();
        session.save();

        // Check in the parent ...
        versionManager.checkin(parent.getPath());

        // and we cannot remove the child versionable node ...
        try {
            child2.remove();
            fail("not allowed");
        } catch (VersionException e) {
            // expected
        }

        // But once the parent is checked out ...
        versionManager.checkout(parent.getPath());

        // We can remove the versionable child that is checked in (!), since the parent is checked out ...
        // See Section 15.2.2:
        // "Note that remove of a read-only node is possible, as long as its parent is not read-only,
        // since removal is an alteration of the parent node."
        assertThat(versionManager.isCheckedOut(child2.getPath()), is(false));
        child2.remove();
        session.save();
    }

    @FixFor( "MODE-1624" )
    @Test
    public void shouldAllowRemovingVersionFromVersionHistory() throws Exception {
        print = false;

        Node outerNode = session.getRootNode().addNode("outerFolder");
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

    @FixFor( "MODE-1624" )
    @Test
    public void shouldAllowRemovingVersionFromVersionHistoryByRemovingVersionNode() throws Exception {
        print = false;

        Node outerNode = session.getRootNode().addNode("outerFolder");
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
        // history.removeVersion(versionName);
        try {
            v3.remove();
            fail("Should not allow removing a protected node");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    @FixFor( "MODE-1775" )
    public void shouldFindVersionNodeByIdentifierAndByUuid() throws Exception {
        registerNodeTypes(session, "cnd/versioning.cnd");

        // Set up parent node and check it in ...
        Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
        parent.setProperty("versionProp", "v");
        parent.setProperty("copyProp", "c");
        parent.setProperty("ignoreProp", "i");
        session.save();
        Version version = versionManager.checkin(parent.getPath());

        // Now look for the version node by identifier, using the different ways to get an identifier ...
        assertThat(session.getNodeByIdentifier(version.getIdentifier()), is((Node)version));
        assertThat(session.getNodeByIdentifier(version.getProperty("jcr:uuid").getString()), is((Node)version));
        assertThat(session.getNodeByUUID(version.getProperty("jcr:uuid").getString()), is((Node)version));
    }

    @Test
    @FixFor( "MODE-1887" )
    public void shouldMergeWorkspaces() throws Exception {
        registerNodeTypes(session, "cnd/mode-1887.cnd");
        Session session = repository.login("default");

        session.getWorkspace().createWorkspace("original");
        session = repository.login("original");

        Node root = session.getRootNode();
        Node parent1 = root.addNode("parent1", "Parent");
        session.save();

        Node child1 = parent1.addNode("child1", "Child");
        assertThat(child1, is(notNullValue()));
        session.save();

        session.getWorkspace().createWorkspace("clone", "original");
        session = repository.login("clone");

        Node child2 = session.getNode("/parent1").addNode("child2", "Child");
        assertThat(child2, is(notNullValue()));
        session.save();

        session = repository.login("original");
        VersionManager vm = session.getWorkspace().getVersionManager();

        NodeIterator ni = vm.merge("/", "clone", true);
        session.save();

        if (print) {
            System.out.println("Failed nodes------------------");
            while (ni.hasNext()) {
                System.out.println(ni.nextNode());
            }
        }
        session.getNode("/parent1/child2");
    }

    @Test
    @FixFor( "MODE-1912" )
    public void shouldRemoveMixVersionablePropertiesWhenRemovingMixin() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
        session.save();

        // mix:referenceable
        assertNotNull(node.getProperty(JcrLexicon.UUID.getString()));
        // mix:simpleVersionable
        assertTrue(node.getProperty(JcrLexicon.IS_CHECKED_OUT.getString()).getBoolean());
        // mix:versionable
        assertNotNull(node.getProperty(JcrLexicon.BASE_VERSION.getString()));
        assertNotNull(node.getProperty(JcrLexicon.VERSION_HISTORY.getString()));
        assertNotNull(node.getProperty(JcrLexicon.PREDECESSORS.getString()));

        node.removeMixin(JcrMixLexicon.VERSIONABLE.getString());
        session.save();

        // mix:referenceable
        assertPropertyIsAbsent(node, JcrLexicon.UUID.getString());
        // mix:simpleVersionable
        assertPropertyIsAbsent(node, JcrLexicon.IS_CHECKED_OUT.getString());
        // mix:versionable
        assertPropertyIsAbsent(node, JcrLexicon.VERSION_HISTORY.getString());
        assertPropertyIsAbsent(node, JcrLexicon.BASE_VERSION.getString());
        assertPropertyIsAbsent(node, JcrLexicon.PREDECESSORS.getString());
    }

    @Test
    @FixFor( "MODE-1912" )
    public void shouldRelinkVersionablePropertiesWhenRemovingAndReaddingMixVersionable() throws Exception {
        JcrVersionManager jcrVersionManager = (JcrVersionManager)versionManager;

        Node node = session.getRootNode().addNode("testNode");
        node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
        session.save();
        // create a new version
        jcrVersionManager.checkin("/testNode");
        jcrVersionManager.checkout("/testNode");
        jcrVersionManager.checkin("/testNode");

        JcrVersionHistoryNode originalVersionHistory = jcrVersionManager.getVersionHistory("/testNode");
        Version originalBaseVersion = jcrVersionManager.getBaseVersion("/testNode");

        // remove the mixin
        jcrVersionManager.checkout("/testNode");
        node.removeMixin(JcrMixLexicon.VERSIONABLE.getString());
        session.save();

        // re-create the mixin and check the previous version history & versionable properties have been relinked.
        node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
        session.save();

        // mix:referenceable
        assertNotNull(node.getProperty(JcrLexicon.UUID.getString()));
        // mix:simpleVersionable
        assertTrue(node.getProperty(JcrLexicon.IS_CHECKED_OUT.getString()).getBoolean());
        // mix:versionable
        assertNotNull(node.getProperty(JcrLexicon.BASE_VERSION.getString()));
        assertNotNull(node.getProperty(JcrLexicon.VERSION_HISTORY.getString()));
        assertNotNull(node.getProperty(JcrLexicon.PREDECESSORS.getString()));

        JcrVersionHistoryNode versionHistory = jcrVersionManager.getVersionHistory("/testNode");
        Version baseVersion = jcrVersionManager.getBaseVersion("/testNode");

        // check the actual
        assertEquals(originalVersionHistory.key(), versionHistory.key());
        assertEquals(originalBaseVersion.getCreated(), baseVersion.getCreated());
        assertEquals(originalBaseVersion.getPath(), baseVersion.getPath());
    }

    @Test
    @FixFor( "MODE-2002" )
    public void shouldMergeEventualSuccessorVersions() throws Exception {
        // Create a "/record", make it versionable and check it in
        session.getRootNode().addNode("record").addMixin("mix:versionable");
        session.save();
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkin("/record");

        // Clone QA version of data
        session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
        Session sessionQa = repository.login("QA");
        VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

        // Change QA node first time
        versionManagerQa.checkout("/record");
        sessionQa.getNode("/record").setProperty("111", "111");
        sessionQa.save();
        versionManagerQa.checkin("/record");

        // Change QA node second time
        versionManagerQa.checkout("/record");
        sessionQa.getNode("/record").setProperty("222", "222");
        sessionQa.save();
        versionManagerQa.checkin("/record");

        // Checks before merge
        // Check basic node - should not have any properties
        assertFalse(session.getNode("/record").hasProperty("111"));
        assertFalse(session.getNode("/record").hasProperty("222"));
        // Check QA node - should have properties 111=111 and 222=222
        assertTrue(sessionQa.getNode("/record").hasProperty("111"));
        assertTrue(sessionQa.getNode("/record").hasProperty("222"));
        assertEquals("111", sessionQa.getNode("/record").getProperty("111").getString());
        assertEquals("222", sessionQa.getNode("/record").getProperty("222").getString());

        // Merge
        versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

        // Checks after merge - basic node should have properties 111=111 and 222=222
        assertTrue(session.getNode("/record").hasProperty("111"));
        assertTrue(session.getNode("/record").hasProperty("222"));
        assertEquals("111", session.getNode("/record").getProperty("111").getString());
        assertEquals("222", session.getNode("/record").getProperty("222").getString());

        sessionQa.logout();
    }

    @Test
    @FixFor( "MODE-2005" )
    public void shouldSetMergeFailedPropertyIfNodeIsCheckedIn() throws Exception {
        // Create a record, make it versionable and check it in
        session.getRootNode().addNode("record").addMixin("mix:versionable");
        session.save();
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkin("/record");

        // Clone QA version of data
        session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
        Session sessionQa = repository.login("QA");
        try {
            VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

            // Change QA node first time
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("111", "111");
            sessionQa.save();
            versionManagerQa.checkin("/record");

            // Change QA node second time, store version
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("222", "222");
            sessionQa.save();
            Version offendingVersion = versionManagerQa.checkin("/record");

            // Change original node one time to make versions in this workspace and other workspace be on
            // divergent branches, causing merge() to fail
            versionManager.checkout("/record");
            session.getNode("/record").setProperty("333", "333");
            session.save();
            versionManager.checkin("/record");

            // Try to merge
            NodeIterator nodeIterator = versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);
            assertTrue(nodeIterator.hasNext());

            while (nodeIterator.hasNext()) {
                Node record = nodeIterator.nextNode();
                Version mergeFailedVersion = (Version)session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                                                                                        .getValues()[0].getString());
                assertEquals(offendingVersion.getIdentifier(), mergeFailedVersion.getIdentifier());
                versionManager.cancelMerge("/record", mergeFailedVersion);
                assertFalse(record.hasProperty("jcr:mergeFailed"));
            }
        } finally {
            sessionQa.logout();
        }
    }

    @Test
    @FixFor( "MODE-2005" )
    public void shouldSetMergeFailedPropertyIfNodeIsCheckedIn2() throws Exception {
        // Create a record, make it versionable and check it in
        session.getRootNode().addNode("record").addMixin("mix:versionable");
        session.save();
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkin("/record");

        // Clone QA version of data
        session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
        Session sessionQa = repository.login("QA");
        try {
            VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

            // Change QA node first time, store version
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("111", "111");
            sessionQa.save();
            Version offendingVersion1 = versionManagerQa.checkin("/record");

            // Change original node one time to make versions in this workspace and other workspace be on
            // divergent branches, causing merge() to fail
            versionManager.checkout("/record");
            session.getNode("/record").setProperty("333", "333");
            session.save();
            versionManager.checkin("/record");

            // Try to merge with offendingVersion1
            // This should create a new jcr:mergeFailed property
            versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

            // Change QA node second time, store version
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("222", "222");
            sessionQa.save();
            Version offendingVersion2 = versionManagerQa.checkin("/record");

            // Try to merge with offendingVersion2
            // This should add to existing jcr:mergeFailed property
            NodeIterator nodeIterator = versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

            assertTrue(nodeIterator.hasNext());
            while (nodeIterator.hasNext()) {
                Node record = nodeIterator.nextNode();
                Version mergeFailedVersion1 = (Version)session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                                                                                         .getValues()[0].getString());
                assertEquals(offendingVersion1.getIdentifier(), mergeFailedVersion1.getIdentifier());
                Version mergeFailedVersion2 = (Version)session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                                                                                         .getValues()[1].getString());
                assertEquals(offendingVersion2.getIdentifier(), mergeFailedVersion2.getIdentifier());
                versionManager.cancelMerge("/record", mergeFailedVersion1);
                versionManager.cancelMerge("/record", mergeFailedVersion2);
                assertFalse(record.hasProperty("jcr:mergeFailed"));
            }
        } finally {
            sessionQa.logout();
        }
    }

    @Test
    @FixFor( "MODE-2006" )
    public void shouldMergeNodesWithSameNamesById() throws Exception {
        // Create a parent and two children, make them versionable and check them in
        Node parent = session.getRootNode().addNode("parent");
        parent.addMixin("mix:versionable");
        Node child1 = parent.addNode("child");
        child1.addMixin("mix:versionable");
        child1.setProperty("myproperty", "CHANGEME");
        Node child2 = parent.addNode("child");
        child2.addMixin("mix:versionable");
        child2.setProperty("myproperty", "222");
        session.save();
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkin(parent.getPath());
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());

        // Clone QA version of data
        session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
        Session sessionQa = repository.login("QA");
        VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

        try {
            // QA: change child1's property
            Node qaParent = sessionQa.getNode("/parent");
            versionManagerQa.checkout(qaParent.getPath());
            Node qaChild1 = sessionQa.getNodeByIdentifier(child1.getIdentifier());
            versionManagerQa.checkout(qaChild1.getPath());
            qaChild1.setProperty("myproperty", "111");

            // QA: Add three new children with same name/path to parent
            Node qaChild3 = qaParent.addNode("child");
            qaChild3.addMixin("mix:versionable");
            qaChild3.setProperty("myproperty", "333");

            Node qaChild4 = qaParent.addNode("child");
            qaChild4.addMixin("mix:versionable");
            qaChild4.setProperty("myproperty", "444");

            Node qaChild5 = qaParent.addNode("child");
            qaChild5.addMixin("mix:versionable");
            qaChild5.setProperty("myproperty", "555");

            // QA: drop child2
            Node qaChild2 = sessionQa.getNodeByIdentifier(child2.getIdentifier());
            qaChild2.remove();

            sessionQa.save();
            Version qaChild1Version = versionManagerQa.checkin(qaChild1.getPath());
            Version qaChild3Version = versionManagerQa.checkin(qaChild3.getPath());
            Version qaChild4Version = versionManagerQa.checkin(qaChild4.getPath());
            Version qaChild5Version = versionManagerQa.checkin(qaChild5.getPath());
            Version qaParentVersion = versionManagerQa.checkin(qaParent.getPath());

            // Merge
            NodeIterator nodeIterator = versionManager.merge("/parent", sessionQa.getWorkspace().getName(), true);

            parent = session.getNodeByIdentifier(parent.getIdentifier());
            child1 = session.getNodeByIdentifier(qaChild1.getIdentifier());
            try {
                session.getNodeByIdentifier(child2.getIdentifier()); // this one got removed
                fail("Deleted child should not be retrieved");
            } catch (ItemNotFoundException e) {
                //continue
            }
            Node child3 = session.getNodeByIdentifier(qaChild3.getIdentifier());
            Node child4 = session.getNodeByIdentifier(qaChild4.getIdentifier());
            Node child5 = session.getNodeByIdentifier(qaChild5.getIdentifier());

            // Run some checks using default workspace's versionManager and session
            assertFalse(nodeIterator.hasNext());

            assertEquals(qaParentVersion.getIdentifier(), versionManager.getBaseVersion(qaParent.getPath()).getIdentifier());
            assertEquals(qaChild1Version.getIdentifier(), versionManager.getBaseVersion(child1.getPath()).getIdentifier());
            assertEquals(qaChild3Version.getIdentifier(), versionManager.getBaseVersion(child3.getPath()).getIdentifier());
            assertEquals(qaChild4Version.getIdentifier(), versionManager.getBaseVersion(child4.getPath()).getIdentifier());
            assertEquals(qaChild5Version.getIdentifier(), versionManager.getBaseVersion(child5.getPath()).getIdentifier());

            // Check that parent no longer has child2
            for (NodeIterator childIterator = parent.getNodes(); childIterator.hasNext();) {
                Node child = childIterator.nextNode();
                assertFalse(child.getIdentifier().equals(child2.getIdentifier()));
            }
            assertEquals(1, child1.getIndex());
            assertEquals(2, child3.getIndex());
            assertEquals(3, child4.getIndex());
            assertEquals(4, child5.getIndex());

            assertEquals("111", child1.getProperty("myproperty").getString());
            assertEquals("333", child3.getProperty("myproperty").getString());
            assertEquals("444", child4.getProperty("myproperty").getString());
            assertEquals("555", child5.getProperty("myproperty").getString());
        } finally {
            sessionQa.logout();
        }
    }

    @Test
    @FixFor( "MODE-2034" )
    public void shouldRestoreNodeWithVersionedChildrenUsingCheckpoints() throws Exception {
        // Create a parent and two children, make them versionable and check them in
        Node parent = session.getRootNode().addNode("parent");
        parent.addMixin("mix:versionable");
        Node child1 = parent.addNode("child1");
        child1.addMixin("mix:versionable");
        child1.setProperty("myproperty", "v1_1");
        Node child2 = parent.addNode("child2");
        child2.addMixin("mix:versionable");
        child2.setProperty("myproperty", "v2_1");
        session.save();

        Version v1 = versionManager.checkpoint(parent.getPath());
        assertEquals("1.0", v1.getName());

        child1.setProperty("myproperty", "v1_2");
        child2.setProperty("myproperty", "v2_2");
        session.save();
        Version v2 = versionManager.checkpoint(parent.getPath());

        assertEquals("1.1", v2.getName());

        versionManager.restore(parent.getPath(), "1.0", true);
        parent = session.getNode("/parent");
        assertEquals("v1_2", parent.getNode("child1").getProperty("myproperty").getString());
        assertEquals("v2_2", parent.getNode("child2").getProperty("myproperty").getString());
    }

    @Test
    @FixFor( "MODE-2034" )
    public void shouldRestoreNodeWithoutVersionedChildrenUsingCheckpoints() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node node = session.getRootNode().addNode("revert", "jj:page");
        node.addNode("child1", "jj:content");
        node.addNode("child2", "jj:content");
        session.save();
        //create two versions
        versionManager.checkpoint(node.getPath());
        versionManager.checkpoint(node.getPath());
        versionManager.restore(node.getPath(), "1.0", true);
    }

    @Test
    @FixFor( "MODE-2055" )
    public void shouldNotReturnTheVersionHistoryNode() throws Exception {
        Node node = session.getRootNode().addNode("outerFolder");
        node.setProperty("jcr:mimeType", "text/plain");
        node.addMixin("mix:versionable");
        session.save();
        versionManager.checkpoint("/outerFolder");

        node.remove();
        session.save();

        try {
            session.getNodeByIdentifier(node.getIdentifier());
            fail("Removed versionable node should not be retrieved ");
        } catch (ItemNotFoundException e) {
            //expected
        }
    }

    @Test
    @FixFor( "MODE-2089" )
    public void shouldKeepOrderWhenRestoring() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node parent = session.getRootNode().addNode("parent", "jj:page");
        parent.addNode("child1", "jj:content");
        parent.addNode("child2", "jj:content");
        parent.addNode("child3", "jj:content");
        parent.addNode("child4", "jj:content");
        session.save();
        versionManager.checkpoint(parent.getPath());

        parent = session.getNode("/parent");
        parent.orderBefore("child4", "child3");
        parent.orderBefore("child3", "child2");
        parent.orderBefore("child2", "child1");
        session.save();

        Version version = versionManager.getBaseVersion(parent.getPath());
        versionManager.restore(version, true);

        parent = session.getNode("/parent");
        List<String> children = new ArrayList<String>();
        NodeIterator nodeIterator = parent.getNodes();
        while (nodeIterator.hasNext()) {
            children.add(nodeIterator.nextNode().getPath());
        }
        assertEquals(Arrays.asList("/parent/child1", "/parent/child2", "/parent/child3", "/parent/child4"), children);
    }

    @Test
    @FixFor( "MODE-2096" )
    public void shouldRestoreAfterRemovingAndReaddingNodesWithSameName() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node parent = session.getRootNode().addNode("parent", "jj:page");
        Node child = parent.addNode("child", "jj:content");
        child.addNode("descendant", "jj:content");
        child.addNode("descendant", "jj:content");
        session.save();
        versionManager.checkpoint(parent.getPath());

        child = session.getNode("/parent/child");
        NodeIterator childNodes = child.getNodes();
        while (childNodes.hasNext()) {
            childNodes.nextNode().remove();
        }
        child.addNode("descendant", "jj:content");
        child.addNode("descendant", "jj:content");
        session.save();

        Version version = versionManager.getBaseVersion(parent.getPath());
        versionManager.restore(version, true);

        parent = session.getNode("/parent");
        assertEquals(Arrays.asList("/parent/child", "/parent/child/descendant", "/parent/child/descendant[2]"), allChildrenPaths(parent));
    }

    @Test
    @FixFor( "MODE-2104" )
    public void shouldRestoreMovedNode() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node parent = session.getRootNode().addNode("parent", "jj:page");
        parent.addNode("child", "jj:content");
        session.save();

        versionManager.checkpoint(parent.getPath());

        parent.addNode("_context", "jj:context");
        // move to a location under a new parent
        session.move("/parent/child", "/parent/_context/child");
        session.save();

        // restore
        versionManager.restore(parent.getPath(), "1.0", true);
        //the default OPV is COPY, so we expect the restore to have removed _context
        assertNoNode("/parent/_context");
        assertNode("/parent/child");
    }

    @Test
    @FixFor( "MODE-2096" )
    public void shouldRestoreToMultipleVersionsWhenEachVersionHasDifferentChild() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        // Add a page node with one child then make a version 1.0
        Node node = session.getRootNode().addNode("page", "jj:page");
        node.addNode("child1", "jj:content");
        session.save();
        versionManager.checkpoint(node.getPath());

        // add second child then make version 1.1
        node.addNode("child2", "jj:content");
        session.save();
        versionManager.checkpoint(node.getPath());
        // restore to 1.0
        versionManager.restore(node.getPath(), "1.0", true);
        assertNode("/page/child1");
        assertNoNode("/page/child2");
        // then restore to 1.1, it will throw the NullPointException
        versionManager.restore(node.getPath(), "1.1", true);
        assertNode("/page/child1");
        assertNode("/page/child2");
    }

    @Test
    @FixFor( "MODE-2112" )
    public void shouldRestoreMovedNode2() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node parent = session.getRootNode().addNode("parent", "jj:page");
        parent.addNode("_context", "jj:context");
        parent.addNode("child", "jj:content");
        session.save();

        versionManager.checkpoint(parent.getPath());

        // move to a location under a new parent
        session.move("/parent/child", "/parent/_context/child");
        session.save();

        // restore
        versionManager.restore(parent.getPath(), "1.0", true);
        assertNoNode("/parent/_context/child");
        assertNode("/parent/child");
    }

    @Test
    @FixFor( "MODE-2112" )
    public void shouldRestoreMovedNode3() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node parent = session.getRootNode().addNode("parent", "jj:page");
        parent.addNode("child1", "jj:content");
        parent.addNode("_context", "jj:context");
        parent.addNode("child2", "jj:content");
        session.save();

        versionManager.checkpoint(parent.getPath());

        // move to a location under a new parent
        session.move("/parent/child1", "/parent/_context/child1");
        session.save();

        // restore
        versionManager.restore(parent.getPath(), "1.0", true);
        assertNoNode("/parent/_context/child1");
        assertNoNode("/parent/_context/child2");
        assertNode("/parent/child1");
        assertNode("/parent/child2");
        assertNode("/parent/_context");
    }

    @Test
    @FixFor( "MODE-2152" )
    public void shouldRemoveVersionInVersionGraphWithBranches2() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node node = session.getRootNode().addNode("node", "jj:page");
        session.save();
        String nodePath = node.getPath();

        //create two versions
        versionManager.checkpoint(nodePath);    // version 1.0
        versionManager.checkpoint(nodePath);    // version 1.1
        versionManager.restore(nodePath, "1.0", true);
        versionManager.checkout(nodePath);
        versionManager.checkpoint(nodePath);    // version 1.1.0

        versionManager.getVersionHistory(nodePath).removeVersion("1.1");
    }

    @Test
    @FixFor( "MODE-2153" )
    public void shouldRemoveVersionWhichWasRestoredAtSomePoint() throws Exception {
        registerNodeTypes("cnd/jj.cnd");

        Node node = session.getRootNode().addNode("node", "jj:page");
        session.save();

        Version v1 = versionManager.checkpoint(node.getPath());
        assertEquals("1.0", v1.getName());
        assertEquals(2, versionManager.getVersionHistory("/node").getAllVersions().getSize());

        Version v2 = versionManager.checkin("/node");
        assertEquals("1.1", v2.getName());
        assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

        versionManager.restore(v1, true);
        assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

        Version baseVersion = versionManager.checkpoint(node.getPath());
        assertEquals("1.0", baseVersion.getName());
        assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

        Version v4 = versionManager.checkin("/node");
        assertEquals("1.1.0", v4.getName());
        assertEquals(4, versionManager.getVersionHistory("/node").getAllVersions().getSize());

        versionManager.getVersionHistory("/node").removeVersion(v1.getName());
    }

    private List<String> allChildrenPaths( Node root ) throws Exception {
        List<String> paths = new ArrayList<String>();
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            paths.add(child.getPath());
            paths.addAll(allChildrenPaths(child));
        }
        return paths;
    }

    private void assertPropertyIsAbsent( Node node,
                                         String propertyName ) throws Exception {
        try {
            node.getProperty(propertyName);
            fail("Property: " + propertyName + " was expected to be missing on node:" + node);
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    private void registerNodeTypes( Session session,
                                    String resourcePathToCnd ) throws Exception {
        NodeTypeManager nodeTypes = (NodeTypeManager)session.getWorkspace().getNodeTypeManager();
        URL url = getClass().getClassLoader().getResource(resourcePathToCnd);
        assertThat(url, is(notNullValue()));
        nodeTypes.registerNodeTypes(url, true);
    }

    private boolean hasVersionHistory( Node node ) throws RepositoryException {
        try {
            VersionHistory history = versionManager.getVersionHistory(node.getPath());
            assertNotNull(history);
            return true;
        } catch (UnsupportedRepositoryOperationException e) {
            return false;
        }
    }
}
