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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.PageWriter;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.ReadOnlyConnector;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * A read-only {@link Connector} that accesses the content in a local Git repository that is a clone of a remote repository.
 * <p>
 * This connector has several properties that must be configured via the {@link RepositoryConfiguration}:
 * <ul>
 * <li><strong><code>directoryPath</code></strong> - The path to the folder that is or contains the <code>.git</code> data
 * structure is to be accessed by this connector.</li>
 * <li><strong><code>remoteName</code></strong> - The alias used by the local Git repository for the remote repository. The
 * default is the "<code>origin</code>". If the value contains commas, the value contains an ordered list of remote aliases that
 * should be searched; the first one to match an existing remote will be used.</li>
 * <li><strong><code>queryableBranches</code></strong> - An array with the names of the branches that should be queryable by the
 * repository. By default, only the master branch is queryable.</li>
 * </ul>
 * </p>
 * <p>
 * The connector results in the following structure:
 * </p>
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Path</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>/branches/{branchName}</code></td>
 * <td>The list of branches.</td>
 * </tr>
 * <tr>
 * <td><code>/tags/{tagName}</code></td>
 * <td>The list of tags.</td>
 * </tr>
 * <tr>
 * <td><code>/commits/{branchOrTagNameOrCommit}/{objectId}</code></td>
 * <td>The history of commits on the branch, tag or object ID name "<code>{branchOrTagNameOrCommit}</code>", where "
 * <code>{objectId}</code>" is the object ID of the commit.</td>
 * </tr>
 * <tr>
 * <td><code>/commit/{branchOrTagNameOrCommit}</code></td>
 * <td>The information about a particular branch, tag or commit "<code>{branchOrTagNameOrCommit}</code>".</td>
 * </tr>
 * <tr>
 * <td><code>/tree/{branchOrTagOrObjectId}/{filesAndFolders}/...</code></td>
 * <td>The structure of the directories and files in the specified branch, tag or commit "<code>{branchOrTagNameOrCommit}</code>".
 * </td>
 * </tr>
 * </table>
 */
public class GitConnector extends ReadOnlyConnector implements Pageable {

    private static final boolean DEFAULT_INCLUDE_MIME_TYPE = false;
    private static final String DEFAULT_REMOTE_NAME = "origin";
    private static final String GIT_DIRECTORY_NAME = ".git";
    private static final List<String> DEFAULT_QUERYABLE_BRANCHES = Arrays.asList("master");

    private static final String GIT_CND_PATH = "org/modeshape/connector/git/git.cnd";

    /**
     * The string path for a {@link File} object that represents the top-level directory of the local Git repository. This is set
     * via reflection and is required for this connector.
     */
    private String directoryPath;

    /**
     * The optional string value representing the name of the remote that serves as the primary remote repository. By default this
     * is "origin". This is set via reflection.
     */
    private String remoteName = DEFAULT_REMOTE_NAME;

    /**
     * The optional string value representing the name of the remote that serves as the primary remote repository. By default this
     * is "origin". This is set via reflection.
     */
    private List<String> parsedRemoteNames;

    /**
     * The optional boolean value specifying whether the connector should set the "jcr:mimeType" property on the "jcr:content"
     * child node under each "git:file" node. By default this is '{@value GitConnector#DEFAULT_INCLUDE_MIME_TYPE}'. This is set
     * via reflection.
     */
    private boolean includeMimeType = DEFAULT_INCLUDE_MIME_TYPE;

    /**
     * The optional list of branch names (under /tree) which should be indexed by the repository and therefore queryable.
     */
    private List<String> queryableBranches = DEFAULT_QUERYABLE_BRANCHES;

    private Repository repository;
    private Git git;
    private Map<String, GitFunction> functions;
    private Map<String, PageableGitFunction> pageableFunctions;
    private Values values;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        // Verify the local git repository exists ...
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RepositoryException(GitI18n.directoryDoesNotExist.text(dir.getAbsolutePath()));
        }
        if (!dir.canRead()) {
            throw new RepositoryException(GitI18n.directoryCannotBeRead.text(dir.getAbsolutePath()));
        }
        File gitDir = dir;
        if (!GIT_DIRECTORY_NAME.equals(gitDir.getName())) {
            gitDir = new File(dir, ".git");
            if (!gitDir.exists() || !gitDir.isDirectory()) {
                throw new RepositoryException(GitI18n.directoryDoesNotExist.text(gitDir.getAbsolutePath()));
            }
            if (!gitDir.canRead()) {
                throw new RepositoryException(GitI18n.directoryCannotBeRead.text(gitDir.getAbsolutePath()));
            }
        }

        values = new Values(factories(), getContext().getBinaryStore());

        // Set up the repository instance. We expect it to exist, and will use it as a "bare" repository (meaning
        // that no working directory will be used nor needs to exist) ...
        repository = new FileRepositoryBuilder().setGitDir(gitDir).setMustExist(true).setBare().build();
        git = new Git(repository);

        // Make sure the remote exists ...
        Set<String> remoteNames = repository.getConfig().getSubsections("remote");
        parsedRemoteNames = new ArrayList<String>();
        String remoteName = null;
        for (String desiredName : this.remoteName.split(",")) {
            if (remoteNames.contains(desiredName)) {
                remoteName = desiredName;
                parsedRemoteNames.add(desiredName);
                break;
            }
        }
        if (remoteName == null) {
            throw new RepositoryException(GitI18n.remoteDoesNotExist.text(this.remoteName, gitDir.getAbsolutePath()));
        }
        this.remoteName = remoteName;

        // Register the different functions ...
        functions = new HashMap<String, GitFunction>();
        pageableFunctions = new HashMap<String, PageableGitFunction>();
        register(new GitRoot(this),
                 new GitBranches(this),
                 new GitTags(this),
                 new GitHistory(this),
                 new GitCommitDetails(this),
                 new GitTree(this));

        // Register the Git-specific node types ...
        InputStream cndStream = getClass().getClassLoader().getResourceAsStream(GIT_CND_PATH);
        nodeTypeManager.registerNodeTypes(cndStream, true);

    }

    private void register( GitFunction... functions ) {
        for (GitFunction function : functions) {
            this.functions.put(function.getName(), function);
            if (function instanceof PageableGitFunction) {
                this.pageableFunctions.put(function.getName(), (PageableGitFunction)function);
            }
        }
    }

    protected DocumentWriter newDocumentWriter( String id ) {
        return super.newDocument(id);
    }

    protected boolean includeMimeType() {
        return includeMimeType;
    }

    @Override
    public void shutdown() {
        repository = null;
        git = null;
        functions = null;
    }

    @Override
    public Document getDocumentById( String id ) {
        CallSpecification callSpec = new CallSpecification(id);
        GitFunction function = functions.get(callSpec.getFunctionName());
        if (function == null) return null;
        try {
            // Set up the document writer ...
            DocumentWriter writer = newDocument(id);
            String parentId = callSpec.getParentId();
            assert parentId != null;
            writer.setParent(parentId);
            // check if the document should be indexed or not, based on the global connector setting and the specific function
            if (!this.isQueryable() || !function.isQueryable(callSpec)) {
                writer.setNotQueryable();
            }
            // Now call the function ...
            Document doc = function.execute(repository, git, callSpec, writer, values);
            // Log the result ...
            getLogger().trace("ID={0},result={1}", id, doc);
            return doc;
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public Document getChildren( PageKey pageKey ) {
        String id = pageKey.getParentId();
        CallSpecification callSpec = new CallSpecification(id);
        PageableGitFunction function = pageableFunctions.get(callSpec.getFunctionName());
        if (function == null) return null;
        try {
            // Set up the document writer ...
            PageWriter writer = newPageDocument(pageKey);
            // Now call the function ...
            return function.execute(repository, git, callSpec, writer, values, pageKey);
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public Document getChildReference( String parentKey,
                                       String childKey ) {
        // The child key always contains the path to the child, so therefore we can always use it to create the
        // child reference document ...
        CallSpecification callSpec = new CallSpecification(childKey);
        return newChildReference(childKey, callSpec.lastParameter());
    }

    @Override
    public String getDocumentId( String path ) {
        // Our paths are basically used as IDs ...
        return path;
    }

    @Override
    public Collection<String> getDocumentPathsById( String id ) {
        // Our paths are basically used as IDs, so the ID is the path ...
        return Collections.singletonList(id);
    }

    @Override
    public boolean hasDocument( String id ) {
        Document doc = getDocumentById(id);
        return doc != null;
    }

    @Override
    public ExternalBinaryValue getBinaryValue( String id ) {
        try {
            ObjectId fileObjectId = ObjectId.fromString(id);
            ObjectLoader fileLoader = repository.open(fileObjectId);
            return new GitBinaryValue(fileObjectId, fileLoader, getSourceName(), null, getMimeTypeDetector());
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    protected final String remoteName() {
        return remoteName;
    }

    protected final List<String> remoteNames() {
        return parsedRemoteNames;
    }

    protected List<String> getQueryableBranches() {
        return queryableBranches;
    }
}
