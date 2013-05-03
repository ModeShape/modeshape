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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.query.Query;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;


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
        fedMgr.createProjection(testRoot.getPath(), "local-git-repo", "/", "git-modeshape");
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
        assertChildrenInclude("Make sure you run <git fetch --tags>", tags, expectedTagNames());
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
        navigate(tree, false, 100, 2);
    }

    @Test
    public void shouldReadCommitSubgraph() throws Exception {
        Node git = gitNode();
        Node commit = git.getNode("commit");
        navigate(commit, false, 100, 2);
    }

    @FixFor( "MODE-1732" )
    @Test
    public void shouldFollowReferenceFromRecentTagToCommit() throws Exception {
        Node git = gitNode();
        Node tag = git.getNode("tags/modeshape-3.0.0.Final");
        assertThat(tag.getProperty("git:objectId").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:tree").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:history").getString(), is(notNullValue()));
        Node tagTree = tag.getProperty("git:tree").getNode();
        assertThat(tagTree.getPath(), is(treePathFor(tag)));
        assertChildrenInclude(tagTree, expectedTopLevelFileAndFolderNames());

        // Load some of the child nodes ...
        Node pomFile = tagTree.getNode("pom.xml");
        assertThat(pomFile.getPrimaryNodeType().getName(), is("git:file"));
        assertNodeHasObjectIdProperty(pomFile);
        assertNodeHasCommittedProperties(pomFile);
        Node pomContent = pomFile.getNode("jcr:content");
        assertNodeHasCommittedProperties(pomContent);
        assertThat(pomContent.getProperty("jcr:data").getString(), is(notNullValue()));

        Node readmeFile = tagTree.getNode("README.md");
        assertThat(readmeFile.getPrimaryNodeType().getName(), is("git:file"));
        assertNodeHasObjectIdProperty(readmeFile);
        assertNodeHasCommittedProperties(readmeFile);
        Node readmeContent = readmeFile.getNode("jcr:content");
        assertNodeHasCommittedProperties(readmeContent);
        assertThat(readmeContent.getProperty("jcr:data").getString(), is(notNullValue()));

        Node parentModule = tagTree.getNode("modeshape-parent");
        assertThat(parentModule.getPrimaryNodeType().getName(), is("git:folder"));
        assertNodeHasObjectIdProperty(parentModule);
        assertNodeHasCommittedProperties(parentModule);
    }

    protected String treePathFor( Node node ) throws Exception {
        Node git = gitNode();
        String commitId = node.getProperty("git:objectId").getString();
        return git.getPath() + "/tree/" + commitId;
    }

    @Test
    public void shouldFollowReferenceFromOldTagToCommit() throws Exception {
        Node git = gitNode();
        Node tag = git.getNode("tags/dna-0.2");
        assertThat(tag.getProperty("git:objectId").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:tree").getString(), is(notNullValue()));
        assertThat(tag.getProperty("git:history").getString(), is(notNullValue()));
        Node tagTree = tag.getProperty("git:tree").getNode();
        assertThat(tagTree.getPath(), is(treePathFor(tag)));
        assertChildrenInclude(tagTree, "pom.xml", "dna-jcr", "dna-common", ".project");
    }

    @Test
    public void shouldContainTagsAndBranchNamesAndCommitsUnderTreeNode() throws Exception {
        Node git = gitNode();
        Node tree = git.getNode("tree");
        assertThat(tree.getPrimaryNodeType().getName(), is("git:trees"));
        assertChildrenInclude(tree, expectedBranchNames());
        assertChildrenInclude("Make sure you run <git fetch --tags>", tree, expectedTagNames());
    }

    @Test
    public void shouldFindMasterBranchAsPrimaryItemUnderBranchNode() throws Exception {
        Node git = gitNode();
        Node branches = git.getNode("branches");
        Item primaryItem = branches.getPrimaryItem();
        assertThat(primaryItem, is(notNullValue()));
        assertThat(primaryItem, is(instanceOf(Node.class)));
        Node primaryNode = (Node)primaryItem;
        assertThat(primaryNode.getName(), is("master"));
        assertThat(primaryNode.getParent(), is(sameInstance(branches)));
        assertThat(primaryNode, is(sameInstance(branches.getNode("master"))));
    }

    @Test
    public void shouldFindMasterBranchAsPrimaryItemUnderTreeNode() throws Exception {
        Node git = gitNode();
        Node tree = git.getNode("tree");
        Item primaryItem = tree.getPrimaryItem();
        assertThat(primaryItem, is(notNullValue()));
        assertThat(primaryItem, is(instanceOf(Node.class)));
        Node primaryNode = (Node)primaryItem;
        assertThat(primaryNode.getName(), is("master"));
        assertThat(primaryNode.getParent(), is(sameInstance(tree)));
        assertThat(primaryNode, is(sameInstance(tree.getNode("master"))));
    }

    @Test
    public void shouldFindTreeBranchAsPrimaryItemUnderGitRoot() throws Exception {
        Node git = gitNode();
        Node tree = git.getNode("tree");
        assertThat(tree, is(notNullValue()));
        Item primaryItem = git.getPrimaryItem();
        assertThat(primaryItem, is(notNullValue()));
        assertThat(primaryItem, is(instanceOf(Node.class)));
        Node primaryNode = (Node)primaryItem;
        assertThat(primaryNode.getName(), is(tree.getName()));
        assertThat(primaryNode.getParent(), is(sameInstance(git)));
        assertThat(primaryNode, is(sameInstance(tree)));
    }

    @Test
    public void shouldFindLatestCommitInMasterBranch() throws Exception {
        Node git = gitNode();
        Node commits = git.getNode("commits");
        Node master = commits.getNode("master");
        Node commit = master.getNodes().nextNode(); // the first commit in the history of the 'master' branch ...
        // print = true;
        printDetails(commit);
        assertNodeHasObjectIdProperty(commit, commit.getName());
        assertNodeHasCommittedProperties(commit);
        assertThat(commit.getProperty("git:title").getString(), is(notNullValue()));
        assertThat(commit.getProperty("git:tree").getNode().getPath(), is(git.getPath() + "/tree/" + commit.getName()));
        assertThat(commit.getProperty("git:detail").getNode().getPath(), is(git.getPath() + "/commit/" + commit.getName()));
    }

    @Test
    public void shouldFindLatestCommitDetailsInMasterBranch() throws Exception {
        Node git = gitNode();
        Node commits = git.getNode("commit");
        Node commit = commits.getNodes().nextNode(); // the first commit ...
        // print = true;
        printDetails(commit);
        assertNodeHasObjectIdProperty(commit);
        assertNodeHasCommittedProperties(commit);
        assertThat(commit.getProperty("git:parents").isMultiple(), is(true));
        for (Value parentValue : commit.getProperty("git:parents").getValues()) {
            String identifier = parentValue.getString();
            Node parent = getSession().getNodeByIdentifier(identifier);
            assertThat(parent, is(notNullValue()));
        }
        assertThat(commit.getProperty("git:diff").getString(), is(notNullValue()));
        assertThat(commit.getProperty("git:tree").getNode().getPath(), is(treePathFor(commit)));
    }

    @Test
    public void shouldIndexQueryableBranches() throws Exception {
        Node git = gitNode();
        Workspace workspace = session.getWorkspace();

        //force reindexing of tags and check that they haven't been indexed
        workspace.reindex(git.getPath() + "/tags");
        Query query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tags/%'", Query.JCR_SQL2);
        assertEquals(0, query.execute().getNodes().getSize());

        //force reindexing of branches and check that they haven't been indexed
        workspace.reindex(git.getPath() + "/branches");
        query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/branches/%'", Query.JCR_SQL2);
        assertEquals(0, query.execute().getNodes().getSize());

        //force reindexing of a file under master/tree and check that it has been indexed
        //(indexing everything under /tree/master is way too expensive)
        workspace.reindex(git.getPath() + "/tree/master/.gitignore");
        query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/master/%'", Query.JCR_SQL2);
        assertTrue(query.execute().getNodes().getSize() > 0);

        //force reindexing of a file under another configured branch and check that it has been indexed
        workspace.reindex(git.getPath() + "/tree/2.x/.gitignore");
        query = workspace.getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [jcr:path] LIKE '%/tree/2.x/%'", Query.JCR_SQL2);
        assertTrue(query.execute().getNodes().getSize() > 0);
    }

    protected void assertNodeHasObjectIdProperty( Node node ) throws Exception {
        assertThat(node.getProperty("git:objectId").getString(), is(notNullValue()));
    }

    protected void assertNodeHasObjectIdProperty( Node node,
                                                  String commitId ) throws Exception {
        assertThat(node.getProperty("git:objectId").getString(), is(commitId));
    }

    protected void assertNodeHasCommittedProperties( Node node ) throws Exception {
        assertThat(node.getProperty("git:author").getString(), is(notNullValue()));
        assertThat(node.getProperty("git:committer").getString(), is(notNullValue()));
        assertThat(node.getProperty("git:committed").getDate(), is(notNullValue()));
        assertThat(node.getProperty("git:title").getString(), is(notNullValue()));
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
