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
package org.modeshape.connector.git;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;

/**
 * Unit test for {@link org.modeshape.connector.git.GitConnector}
 */
public class GitConnectorTest extends MultiUseAbstractTest {

    private Node testRoot;

    @BeforeClass
    public static void beforeAll() throws Exception {
        loadGitRepositoryData();

        RepositoryConfiguration config = RepositoryConfiguration.read("config/repo-config-git-federation.json");
        startRepository(config);

        Session session = getSession();
        Node testRoot = session.getRootNode().addNode("repos");
        session.save();

        FederationManager fedMgr = session.getWorkspace().getFederationManager();
        fedMgr.createProjection(testRoot.getPath(), "remote-git-repo", "/", "git-modeshape-remote");
        fedMgr.createProjection(testRoot.getPath(), "local-git-repo", "/", "git-modeshape-local");
    }

    private static void loadGitRepositoryData() throws Exception {
        try (Git git = Git.open(new File("../.."))) {
            // the tests expect a series of remote branches and tags from origin, so if they're not present in the clone 
            // where the test is running, we need to load them...
            List<RefSpec> refsToFetch = new ArrayList<>();
            List<String> tagNames = git.tagList().call().stream()
                                       .map(ref -> ref.getName().replace("refs/tags/", ""))
                                       .collect(Collectors.toList());

            Arrays.stream(expectedTagNames())
                  .filter(tagName -> !tagNames.contains(tagName))
                  .map(tagName -> new RefSpec("+refs/tags/" + tagName + ":refs/remotes/origin/" + tagName))
                  .forEach(refsToFetch::add);

            List<String> branchNames = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
                                          .stream()
                                          .map(ref -> ref.getName()
                                                         .replace("refs/heads/", "")
                                                         .replace("refs/remotes/origin/", ""))
                                          .collect(Collectors.toList());
            Arrays.stream(expectedRemoteBranchNames())
                  .filter(branchName -> !branchNames.contains(branchName))
                  .map(branchName -> new RefSpec("+refs/heads/" + branchName + ":refs/remotes/origin/" + branchName))
                  .forEach(refsToFetch::add);

            if (!refsToFetch.isEmpty()) {
                // next fetch all the remote refs which we need for the test
                git.fetch().setRefSpecs(refsToFetch).call();
            }
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Before
    public void before() throws Exception {
        testRoot = getSession().getRootNode().getNode("repos");
    }

    protected Node gitRemoteNode() throws Exception {
        return testRoot.getNode("git-modeshape-remote");
    }

    protected Node gitLocalNode() throws Exception {
        return testRoot.getNode("git-modeshape-local");
    }

    @Test
    public void shouldReadFederatedNodeInProjection() throws Exception {
        Node git = gitRemoteNode();
        assertThat(git, is(notNullValue()));
        assertThat(git.getParent(), is(sameInstance(testRoot)));
        assertThat(git.getPath(), is(testRoot.getPath() + "/git-modeshape-remote"));
        assertThat(git.getName(), is("git-modeshape-remote"));
    }

    @Test
    public void shouldReadTags() throws Exception {
        Node git = gitRemoteNode();
        Node tags = git.getNode("tags");
        assertChildrenInclude("Make sure you run <git fetch --tags>", tags, expectedTagNames());
    }

    @Test
    public void shouldReadRemoteBranches() throws Exception {
        Node git = gitRemoteNode();
        Node branches = git.getNode("branches");
        assertChildrenInclude(branches, expectedRemoteBranchNames());
    }

    @Test
    @FixFor( "MODE-2426" )
    public void shouldReadLocalBranches() throws Exception {
        Node git = gitLocalNode();
        Node branches = git.getNode("branches");
        assertChildrenInclude(branches, "master");
    }

    @Test
    public void shouldReadTreeSubgraph() throws Exception {
        Node git = gitRemoteNode();
        Node tree = git.getNode("tree");
        navigate(tree, false, 100, 2);
    }

    @Test
    public void shouldReadCommitSubgraph() throws Exception {
        Node git = gitRemoteNode();
        Node commit = git.getNode("commit");
        navigate(commit, false, 100, 2);
    }

    @FixFor( "MODE-1732" )
    @Test
    public void shouldFollowReferenceFromRecentTagToCommit() throws Exception {
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
        String commitId = node.getProperty("git:objectId").getString();
        return git.getPath() + "/tree/" + commitId;
    }

    @Test
    public void shouldFollowReferenceFromOldTagToCommit() throws Exception {
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
        Node tree = git.getNode("tree");
        assertThat(tree.getPrimaryNodeType().getName(), is("git:trees"));
        assertChildrenInclude(tree, expectedRemoteBranchNames());
        assertChildrenInclude("Make sure you run <git fetch --tags>", tree, expectedTagNames());
    }

    @Test
    public void shouldFindMasterBranchAsPrimaryItemUnderBranchNode() throws Exception {
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
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
        Node git = gitRemoteNode();
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
    @FixFor( "MODE-2352" )
    public void shouldReadTreeObjectProperties() throws Exception {
        Node tree = session.getNode("/repos/git-modeshape-remote/tree/72ea74be3b3a50345a1b2f543f78fd6be00caa35");
        assertNotNull(tree);
        PropertyIterator propertyIterator = tree.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.nextProperty();
            assertNotNull(property.getName());
            assertNotNull(property.getValue());
        }
    }
    @Test
    @FixFor( "MODE-2352" )
    public void shouldReadBranchObjectProperties() throws Exception {
        Node branch = session.getNode("/repos/git-modeshape-remote/branches/master");
        assertNotNull(branch);
        PropertyIterator propertyIterator = branch.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.nextProperty();
            assertNotNull(property.getName());
            assertNotNull(property.getValue());
        }
    }

    @Test
    @FixFor( "MODE-2352" )
    public void shouldNavigateCommitWithMultiplePages() throws Exception {
        Node commit = session.getNode("/repos/git-modeshape-remote/commits/d1f7daf32bd67edded7545221cd5c79d94813310");
        assertNotNull(commit);
        NodeIterator childrenIterator = commit.getNodes();
        while (childrenIterator.hasNext()) {
            childrenIterator.nextNode();
        }
    }
    
    @Test
    @FixFor( "MODE-2643")
    public void shouldReadBinaryNodeAsLargeFile() throws Exception {
        //use some JGit API magic to reconfigure the default threshold which is around 50MB
        //so that when we attempt to read a larger binary, it will be seen as a large file by JGit
        WindowCacheConfig newConfig = new WindowCacheConfig();
        newConfig.setStreamFileThreshold(2 * WindowCacheConfig.MB);
        newConfig.install();
        try {
            readLargeBinary();
        } finally {
            newConfig.setStreamFileThreshold(PackConfig.DEFAULT_BIG_FILE_THRESHOLD);
            newConfig.install();
        }
    }

    @Test
    @FixFor( "MODE-2643")
    public void shouldReadBinaryNodeAsRegularFile() throws Exception {
        readLargeBinary();
    }
    
    private void readLargeBinary() throws Exception {
        Node commit = session.getNode("/repos/git-modeshape-remote/tree/master/modeshape-jcr/src/test/resources/docs/postgresql-8.4.1-US.pdf");
        assertNotNull(commit);
        Binary data = commit.getNode("jcr:content").getProperty("jcr:data").getBinary();
        long size = data.getSize();
        assertTrue(size > 0);
        //simply read the stream to make sure it's valid
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        IoUtil.write(data.getStream(), bos);
        assertEquals("invalid binary stream", size, baos.toByteArray().length);
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
    protected static String[] expectedTopLevelFileAndFolderNames() {
        return new String[]{"modeshape-parent", "pom.xml"};
    }

    /**
     * The <i>minimal</i> names of the branches that are expected to exist. Additional branch names will be acceptable.
     * Note that if any of these branches do not exist at startup, the tests will attempt to retrieve them from remote/origin
     *
     * @return the branch names; never null
     */
    protected static String[] expectedRemoteBranchNames() {
        return new String[]{"master", "2.x", "3.x", "4.x"};
    }

    /**
     * The <i>minimal</i> names of the tags that are expected to exist. Additional tag names will be acceptable.
     * Note that if any of these tags do not exist at startup, the tests will attempt to retrieve them from remote/origin
     *
     * @return the tag names; never null
     */
    protected static String[] expectedTagNames() {
        return new String[]{"modeshape-3.0.0.Final", "dna-0.2"};
    }
}
