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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.PageWriter;

/**
 * A {@link GitFunction} that returns the history information about the (latest) commits in a particular branch or tag. The
 * structure of this area of the repository is as follows:
 * 
 * <pre>
 *   /commits/{branchOrTagNameOrObjectId}/{objectId}
 * </pre>
 */
public class GitHistory extends GitFunction implements PageableGitFunction {

    protected static Object referenceToHistory( ObjectId id,
                                                String branchOrTagName,
                                                Values values ) {
        return values.referenceTo(ID + DELIMITER + branchOrTagName + DELIMITER + id.getName());
    }

    protected static final String NAME = "commits";
    protected static final String ID = "/commits";

    protected static int DEFAULT_PAGE_SIZE = 15;

    private int pageSize = DEFAULT_PAGE_SIZE;

    public GitHistory( GitConnector connector ) {
        super(NAME, connector);
    }

    @Override
    public boolean isPaged() {
        return true;
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             DocumentWriter writer,
                             Values values ) throws GitAPIException, IOException {
        if (spec.parameterCount() == 0) {
            // This is the top-level "/commits" node
            writer.setPrimaryType(GitLexicon.COMMITS);

            // Generate the child references to the branches, tags, and commits in the history ...
            addBranchesAsChildren(git, spec, writer);
            addTagsAsChildren(git, spec, writer);
            addCommitsAsChildren(git, spec, writer, pageSize);

        } else if (spec.parameterCount() == 1) {
            // This is the top-level "/commits/{branchOrTagNameOrObjectId}" node
            writer.setPrimaryType(GitLexicon.OBJECT);
            addCommitsAsChildren(git, spec, writer, pageSize);
        } else if (spec.parameterCount() == 2) {
            // This is a specific commit in the history, via "/commits/{branchOrTagNameOrObjectId}/{objectId}"
            writer.setPrimaryType(GitLexicon.COMMIT);

            // so we need to show the commit information ...
            RevWalk walker = new RevWalk(repository);
            try {
                String commitId = spec.parameter(1);
                ObjectId objId = repository.resolve(commitId);
                RevCommit commit = walker.parseCommit(objId);
                writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                writer.addProperty(GitLexicon.AUTHOR, authorName(commit));
                writer.addProperty(GitLexicon.COMMITTER, commiterName(commit));
                writer.addProperty(GitLexicon.COMMITTED, values.dateFrom(commit.getCommitTime()));
                writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
                writer.addProperty(GitLexicon.TREE, GitTree.referenceToTree(objId, objId.name(), values));
                writer.addProperty(GitLexicon.DETAIL, GitCommitDetails.referenceToCommit(objId, values));
                // And there are no children
            } finally {
                walker.dispose();
            }
        } else {
            return null;
        }

        return writer.document();
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             PageWriter writer,
                             Values values,
                             PageKey pageKey ) throws GitAPIException, IOException {
        // List the next page of commits ...
        addCommitsAsPageOfChildren(git, repository, spec, writer, pageKey);
        return writer.document();
    }
}
