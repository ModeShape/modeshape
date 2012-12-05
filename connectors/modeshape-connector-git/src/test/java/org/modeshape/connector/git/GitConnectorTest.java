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
package org.modeshape.connector.git;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;

public class GitConnectorTest extends MultiUseAbstractTest {

    private Node testRoot;

    @BeforeClass
    public static void beforeAll() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("config/repo-config-git-federation.json");
        startRepository(config);

        // registerNodeTypes("cnd/flex.cnd");

        Session session = getSession();
        Node testRoot = session.getRootNode().addNode("repos");
        session.save();

        FederationManager fedMgr = session.getWorkspace().getFederationManager();
        fedMgr.createExternalProjection(testRoot.getPath(), "local-git-repo", "/", "git-modeshape");
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Before
    public void before() throws Exception {
        testRoot = getSession().getRootNode().getNode("repos");
    }

    protected Node gitNode() throws Exception {
        return testRoot.getNode("git-modeshape");
    }

    @Test
    public void shouldReadFederatedNodeInProjection() throws Exception {
        Node git = gitNode();
        assertThat(git, is(notNullValue()));
        assertThat(git.getParent(), is(sameInstance(testRoot)));
        assertThat(git.getPath(), is(testRoot.getPath() + "/git-modeshape"));
        assertThat(git.getName(), is("git-modeshape"));
    }

    @Test
    public void shouldReadTags() throws Exception {
        Node git = gitNode();
        Node tags = git.getNode("tags");
        assertChildrenInclude(tags, expectedTagNames());
    }

    @Test
    public void shouldReadBranches() throws Exception {
        Node git = gitNode();
        Node branches = git.getNode("branches");
        assertChildrenInclude(branches, expectedBranchNames());
    }

    @Test
    public void shouldReadTreeSubgraph() throws Exception {
        Node git = gitNode();
        Node tree = git.getNode("tree");
        print = true;
        print(tree, false, 100, 2);
    }

    @Test
    public void shouldReadCommitSubgraph() throws Exception {
        Node git = gitNode();
        Node commit = git.getNode("commit");
        print = true;
        print(commit, false, 100, 2);
    }

    @Test
    public void shouldFollowReferenceFromRecentTagToCommit() throws Exception {
        Node git = gitNode();
        Node tag = git.getNode("tags/modeshape-3.0.0.Final");
        assertThat(tag.getProperty("git:objectId").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:tree").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:history").getString(), is(notNullValue()));
        Node tagTree = tag.getProperty("git:tree").getNode();
        assertThat(tagTree.getPath().startsWith(git.getPath() + "/tree/modeshape-3.0.0.Final"), is(true));
        assertChildrenInclude(tagTree, expectedTopLevelFileAndFolderNames());
    }

    @Test
    public void shouldFollowReferenceFromOldTagToCommit() throws Exception {
        Node git = gitNode();
        Node tag = git.getNode("tags/dna-0.2");
        assertThat(tag.getProperty("git:objectId").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:tree").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:history").getString(), is(notNullValue()));
        Node tagTree = tag.getProperty("git:tree").getNode();
        assertThat(tagTree.getPath().startsWith(git.getPath() + "/tree/dna-0.2"), is(true));
        assertChildrenInclude(tagTree, "pom.xml", "dna-jcr", "dna-common", ".project");
    }

    @Test
    public void shouldContainTagsAndBranchNamesAndCommitsUnderTreeNode() throws Exception {
        Node git = gitNode();
        Node tree = git.getNode("tree");
        assertThat(tree.getPrimaryNodeType().getName(), is("git:trees"));
        assertChildrenInclude(tree, expectedBranchNames());
        assertChildrenInclude(tree, expectedTagNames());
    }

    /**
     * The <i>minimal</i> names of the files and/or folders that are expected to exist at the top-level of the Git repository.
     * Additional file and folder names will be acceptable.
     * 
     * @return the file and folder names; never null
     */
    protected String[] expectedTopLevelFileAndFolderNames() {
        return new String[] {"modeshape-parent", "pom.xml"};
    }

    /**
     * The <i>minimal</i> names of the branches that are expected to exist. Additional branch names will be acceptable.
     * 
     * @return the branch names; never null
     */
    protected String[] expectedBranchNames() {
        return new String[] {"master", "2.2.x", "2.5.x", "2.8.x", "2.x", "3.0.x"};
    }

    /**
     * The <i>minimal</i> names of the tags that are expected to exist. Additional tag names will be acceptable.
     * 
     * @return the tag names; never null
     */
    protected String[] expectedTagNames() {
        return new String[] {"modeshape-3.0.1.Final", "modeshape-3.0.0.Final", "modeshape-3.0.0.CR3", "modeshape-3.0.0.CR2",
            "modeshape-3.0.0.CR1", "modeshape-3.0.0.Beta4", "modeshape-3.0.0.Beta3", "modeshape-3.0.0.Beta2",
            "modeshape-3.0.0.Beta1", "modeshape-3.0.0.Alpha6", "modeshape-3.0.0.Alpha5", "modeshape-3.0.0.Alpha4",
            "modeshape-3.0.0.Alpha3", "modeshape-3.0.0.Alpha2", "modeshape-3.0.0.Alpha1", "modeshape-2.8.3.Final",
            "modeshape-2.8.2.Final", "modeshape-2.8.1.GA", "modeshape-2.8.1.Final", "modeshape-2.8.0.Final",
            "modeshape-2.7.0.Final", "modeshape-2.6.0.Final", "modeshape-2.6.0.Beta2", "modeshape-2.6.0.Beta1",
            "modeshape-2.5.0.Final", "modeshape-2.5.0.Beta2", "modeshape-2.5.0.Beta1", "modeshape-2.4.0.Final",
            "modeshape-2.3.0.Final", "modeshape-2.2.0.Final", "modeshape-2.1.0.Final", "modeshape-2.0.0.Final",
            "modeshape-1.2.0.Final", "modeshape-1.1.0.Final", "modeshape-1.0.0.Final", "modeshape-1.0.0.Beta1", "dna-0.7",
            "dna-0.6", "dna-0.5", "dna-0.4", "dna-0.3", "dna-0.2", "dna-0.1"};
    }

}
