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

import java.io.IOException;
import javax.jcr.RepositoryException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.PageWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.schematic.document.Document;

/**
 * A function that returns the file and directory structure within a particular commit. The structure of this area of the
 * repository is as follows:
 * 
 * <pre>
 *   /tree/{branchOrTagOrObjectId}/{filesAndFolders}/...
 * </pre>
 */
public class GitTree extends GitFunction implements PageableGitFunction {

    protected static final String JCR_CONTENT = "jcr:content";
    protected static final String JCR_CONTENT_SUFFIX = "/" + JCR_CONTENT;

    protected static final String NAME = "tree";
    protected static final String ID = "/tree";

    protected static Object referenceToTree( ObjectId commitId,
                                             String branchOrTagOrCommitId,
                                             Values values ) {
        return values.referenceTo(ID + DELIMITER + branchOrTagOrCommitId);
    }

    public GitTree( GitConnector connector ) {
        super(NAME, connector);
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             DocumentWriter writer,
                             Values values ) throws GitAPIException, IOException {
        if (spec.parameterCount() == 0) {
            // This is the top-level "/branches" node
            writer.setPrimaryType(GitLexicon.TREES);

            // Generate the child references to the branches and tags. Branches are likely used more often, so list them first...
            addBranchesAsChildren(git, spec, writer);
            addTagsAsChildren(git, spec, writer);
            addCommitsAsChildren(git, spec, writer, pageSize);

        } else if (spec.parameterCount() == 1) {
            // This is a particular branch/tag/commit node ...
            String branchOrTagOrObjectId = spec.parameter(0);
            ObjectId objId = resolveBranchOrTagOrCommitId(repository, branchOrTagOrObjectId);
            RevWalk walker = new RevWalk(repository);
            walker.setRetainBody(true); // we need to parse the commit for the top-level
            try {
                RevCommit commit = walker.parseCommit(objId);

                // could happen if not enough permissions, for example
                if (commit != null) {
                    // Add the properties for this node ...
                    String committer = commiterName(commit);
                    String author = authorName(commit);
                    DateTime committed = values.dateFrom(commit.getCommitTime());
                    writer.setPrimaryType(GitLexicon.FOLDER);
                    writer.addProperty(JcrLexicon.CREATED, committed);
                    writer.addProperty(JcrLexicon.CREATED_BY, committer);
                    writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                    writer.addProperty(GitLexicon.AUTHOR, author);
                    writer.addProperty(GitLexicon.COMMITTER, committer);
                    writer.addProperty(GitLexicon.COMMITTED, committed);
                    writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
                    writer.addProperty(GitLexicon.HISTORY, GitHistory.referenceToHistory(objId, branchOrTagOrObjectId, values));
                    writer.addProperty(GitLexicon.DETAIL, GitCommitDetails.referenceToCommit(objId, values));

                    // Add the top-level children of the directory ...
                    addInformationForPath(repository, writer, commit, "", spec, values);
                } else {
                    connector.getLogger().warn(GitI18n.cannotReadCommit, objId);

                }
            } finally {
                walker.dispose();
            }

        } else {
            // This is a folder or file within the directory structure ...
            String branchOrTagOrObjectId = spec.parameter(0);
            String path = spec.parametersAsPath(1);
            ObjectId objId = resolveBranchOrTagOrCommitId(repository, branchOrTagOrObjectId);
            RevWalk walker = new RevWalk(repository);
            walker.setRetainBody(true);
            try {
                // Get the commit information ...
                RevCommit commit = walker.parseCommit(objId);

                if (commit != null) {
                    // Add the top-level children of the directory ...
                    addInformationForPath(repository, writer, commit, path, spec, values);
                }
            } finally {
                walker.dispose();
            }
        }
        return writer.document();
    }

    protected void addInformationForPath( Repository repository,
                                          DocumentWriter writer,
                                          RevCommit commit,
                                          String path,
                                          CallSpecification spec,
                                          Values values ) throws GitAPIException, IOException {
        // Make sure the path is in the canonical form we need ...
        if (path.startsWith("/")) {
            if (path.length() == 1) path = "";
            else path = path.substring(1);
        }

        // Now see if we're actually referring to the "jcr:content" node ...
        boolean isContentNode = false;
        if (path.endsWith(JCR_CONTENT_SUFFIX)) {
            isContentNode = true;
            path = path.substring(0, path.length() - JCR_CONTENT_SUFFIX.length());
        }

        // Create the TreeWalk that we'll use to navigate the files/directories ...
        final TreeWalk tw = new TreeWalk(repository);
        tw.addTree(commit.getTree());
        if ("".equals(path)) {
            // This is the top-level directory, so we don't need to pre-walk to find anything ...
            tw.setRecursive(false);
            while (tw.next()) {
                String childName = tw.getNameString();
                String childId = spec.childId(childName);
                writer.addChild(childId, childName);
            }
        } else {
            // We need to first find our path *before* we can walk the children ...
            PathFilter filter = PathFilter.create(path);
            tw.setFilter(filter);
            while (tw.next()) {
                if (filter.isDone(tw)) {
                    break;
                } else if (tw.isSubtree()) {
                    tw.enterSubtree();
                }
            }
            // Now that the TreeWalk is the in right location given by the 'path', we can get the
            if (tw.isSubtree()) {
                // The object at the 'path' is a directory, so go into it ...
                tw.enterSubtree();

                // Find the commit in which this folder was last modified ...
                // This may not be terribly efficient, but it seems to work faster on subsequent runs ...
                writer.setPrimaryType(GitLexicon.FOLDER);

                // Add folder-related properties ...
                String committer = commiterName(commit);
                String author = authorName(commit);
                DateTime committed = values.dateFrom(commit.getCommitTime());
                writer.addProperty(JcrLexicon.CREATED, committed);
                writer.addProperty(JcrLexicon.CREATED_BY, committer);
                writer.addProperty(GitLexicon.OBJECT_ID, commit.getId().name());
                writer.addProperty(GitLexicon.AUTHOR, author);
                writer.addProperty(GitLexicon.COMMITTER, committer);
                writer.addProperty(GitLexicon.COMMITTED, committed);
                writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
              
                // And now walk the contents of the directory ...
                while (tw.next()) {
                    String childName = tw.getNameString();
                    String childId = spec.childId(childName);
                    writer.addChild(childId, childName);
                }
            } else {
                // The path specifies a file (or a content node) ...
                
                if (isContentNode) {
                    writer.setPrimaryType(GitLexicon.RESOURCE);

                    // Add file-related properties ...
                    String committer = commiterName(commit);
                    String author = authorName(commit);
                    DateTime committed = values.dateFrom(commit.getCommitTime());

                    writer.addProperty(JcrLexicon.LAST_MODIFIED, committed);
                    writer.addProperty(JcrLexicon.LAST_MODIFIED_BY, committer);
                    writer.addProperty(GitLexicon.OBJECT_ID, commit.getId().name());
                    writer.addProperty(GitLexicon.AUTHOR, author);
                    writer.addProperty(GitLexicon.COMMITTER, committer);
                    writer.addProperty(GitLexicon.COMMITTED, committed);
                    writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
                    // Create the BinaryValue ...
                    ObjectId fileObjectId = tw.getObjectId(0);
                    ObjectLoader fileLoader = repository.open(fileObjectId);
                    // we'll always create an external binary which will be resolved by the connector when required
                    BinaryValue value = new GitBinaryValue(fileObjectId, fileLoader, connector.getSourceName(), name,
                                                           connector.getMimeTypeDetector());
                    writer.addProperty(JcrLexicon.DATA, value);
                    if (connector.includeMimeType()) {
                        try {
                            String filename = spec.parameter(spec.parameterCount() - 1); // the last is 'jcr:content'
                            String mimeType = value.getMimeType(filename);
                            if (mimeType != null) writer.addProperty(JcrLexicon.MIMETYPE, mimeType);
                        } catch (RepositoryException | IOException e) {
                            // do nothing
                            connector.getLogger().debug("cannot determine mime-type information for objectID '{0}'", fileObjectId);
                        }
                    }
                } else {
                    writer.setPrimaryType(GitLexicon.FILE);
                  
                    // Add file-related properties ...
                    String committer = commiterName(commit);
                    String author = authorName(commit);
                    DateTime committed = values.dateFrom(commit.getCommitTime());

                    writer.addProperty(JcrLexicon.CREATED, committed);
                    writer.addProperty(JcrLexicon.CREATED_BY, committer);
                    writer.addProperty(GitLexicon.OBJECT_ID, commit.getId().name());
                    writer.addProperty(GitLexicon.AUTHOR, author);
                    writer.addProperty(GitLexicon.COMMITTER, committer);
                    writer.addProperty(GitLexicon.COMMITTED, committed);
                    writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());

                    // Add the "jcr:content" child node ...
                    String childId = spec.childId(JCR_CONTENT);
                    writer.addChild(childId, JCR_CONTENT);
                }
            }
        }
    }

    @Override
    public boolean isPaged() {
        return true;
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             PageWriter writer,
                             Values values,
                             PageKey pageKey ) throws GitAPIException, IOException {
        if (spec.parameterCount() != 0) return null;
        addCommitsAsPageOfChildren(git, repository, spec, writer, pageKey);
        return writer.document();
    }
}
