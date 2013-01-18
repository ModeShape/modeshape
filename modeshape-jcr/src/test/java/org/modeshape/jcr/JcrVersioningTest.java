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

import java.net.URL;
import javax.jcr.Node;
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
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for versioning behaviour (see JSR_285#15)
 * 
 * @author Horia Chiorean
 */
public class JcrVersioningTest {

    private Session session;
    private VersionManager versionManager;

    @Before
    public void beforeEach() throws RepositoryException {
        String repositorySource = "store";
        String repositoryName = "r1";

        JcrConfiguration config = createInMemoryConfig(repositorySource, repositoryName);
        JcrEngine engine = startEngine(config);
        createTestSession(repositoryName, engine);

        versionManager = session.getWorkspace().getVersionManager();
    }

    private void createTestSession( String repositoryName,
                                    JcrEngine engine ) throws RepositoryException {
        JcrRepository repository = engine.getRepository(repositoryName);
        session = repository.login();
        assertNotNull(session);
    }

    private JcrEngine startEngine( JcrConfiguration config ) {
        JcrEngine engine = config.build();
        engine.start();
        assertThat(engine.getProblems().hasErrors(), is(false));
        return engine;
    }

    private JcrConfiguration createInMemoryConfig( String repositorySource,
                                                   String repositoryName ) {
        JcrConfiguration config = new JcrConfiguration();
        config.repositorySource("store")
              .usingClass(InMemoryRepositorySource.class)
              .setRetryLimit(100)
              .setProperty("defaultWorkspaceName", "ws1");
        config.repository(repositoryName)
              .setSource(repositorySource)
              .setOption(JcrRepository.Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        config.save();
        return config;
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
        session.refresh(true);
        assertTrue(hasVersionHistory(fileNode));
    }

    @Test
    @FixFor( "MODE-1401" )
    public void shouldAllowAddingUnderCheckedInNodeNewChildNodeWithOpvOfIgnore() throws Exception {
        registerNodeTypes(session, "versioning.cnd");

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
        registerNodeTypes(session, "versioning.cnd");

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
        registerNodeTypes(session, "versioning.cnd");

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
        registerNodeTypes(session, "versioning.cnd");

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
        boolean print = false;

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
        boolean print = false;

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
