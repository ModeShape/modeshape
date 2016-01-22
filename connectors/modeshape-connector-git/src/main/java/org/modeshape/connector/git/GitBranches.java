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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;

/**
 * A {@link GitFunction} that returns the list of branches in this repository. The structure of this area of the repository is as
 * follows:
 * 
 * <pre>
 *   /branches/{branchName}
 * </pre>
 */
public class GitBranches extends GitFunction {

    protected static final String NAME = "branches";
    protected static final String ID = "/branches";

    public GitBranches( GitConnector connector ) {
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
            writer.setPrimaryType(GitLexicon.BRANCHES);

            // Generate the child references to the branches ...
            addBranchesAsChildren(git, spec, writer);

        } else if (spec.parameterCount() == 1) {
            // This is a particular branch node ...
            writer.setPrimaryType(GitLexicon.BRANCH);
            String branchName = spec.parameter(0);
            String branchRefName = branchRefForName(branchName);
            // Get the Ref, which doesn't directly know about the commit SHA1, so we have to parse the commit ...
            Ref ref = repository.getRef(branchRefName);
            if (ref == null) return null; // invalid branch
            RevWalk walker = new RevWalk(repository);
            try {
                RevCommit commit = walker.parseCommit(ref.getObjectId());
                // Construct the references to other nodes in this source ...
                ObjectId objId = commit.getId();
                writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                writer.addProperty(GitLexicon.TREE, GitTree.referenceToTree(objId, objId.name(), values));
                writer.addProperty(GitLexicon.HISTORY, GitHistory.referenceToHistory(objId, branchName, values));
                writer.addProperty(GitLexicon.DETAIL, GitCommitDetails.referenceToCommit(objId, values));
            } finally {
                walker.dispose();
            }
        } else {
            return null;
        }

        return writer.document();
    }
}
