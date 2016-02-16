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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.PageWriter;

/**
 * A {@link GitFunction} that returns the information about a particular commit. The structure of this area of the repository is
 * as follows:
 * 
 * <pre>
 *   /commit/{branchOrTagNameOrObjectId}
 * </pre>
 */
public class GitCommitDetails extends GitFunction implements PageableGitFunction {

    /**
     * The name of the character set that is used when building the patch difference for a commit.
     */
    public static final String DIFF_CHARSET_NAME = "UTF-8";

    protected static final String NAME = "commit";
    protected static final String ID = "/commit";

    protected static Object referenceToCommit( ObjectId id,
                                               Values values ) {
        return values.referenceTo(ID + DELIMITER + id.getName());
    }

    protected static Object[] referencesToCommits( ObjectId[] ids,
                                                   Values values ) {
        int size = ids.length;
        Object[] results = new Object[size];
        for (int i = 0; i != size; ++i) {
            results[i] = referenceToCommit(ids[i], values);
        }
        return results;
    }

    public GitCommitDetails( GitConnector connector ) {
        super(NAME, connector);
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             DocumentWriter writer,
                             Values values ) throws GitAPIException, IOException {
        if (spec.parameterCount() == 0) {
            // This is the top-level "/commit" node
            writer.setPrimaryType(GitLexicon.DETAILS);

            // Generate the child references to the branches, tags, and commits in the history ...
            addBranchesAsChildren(git, spec, writer);
            addTagsAsChildren(git, spec, writer);
            addCommitsAsChildren(git, spec, writer, pageSize);

        } else if (spec.parameterCount() == 1) {
            // This is the top-level "/commit/{branchOrTagNameOrObjectId}" node
            writer.setPrimaryType(GitLexicon.DETAILED_COMMIT);

            // Add the properties describing this commit ...
            RevWalk walker = new RevWalk(repository);
            walker.setRetainBody(true);
            try {
                String branchOrTagOrCommitId = spec.parameter(0);
                ObjectId objId = resolveBranchOrTagOrCommitId(repository, branchOrTagOrCommitId);
                RevCommit commit = walker.parseCommit(objId);
                writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                writer.addProperty(GitLexicon.AUTHOR, authorName(commit));
                writer.addProperty(GitLexicon.COMMITTER, commiterName(commit));
                writer.addProperty(GitLexicon.COMMITTED, values.dateFrom(commit.getCommitTime()));
                writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
                writer.addProperty(GitLexicon.MESSAGE, commit.getFullMessage().trim());// removes trailing whitespace
                writer.addProperty(GitLexicon.PARENTS, GitCommitDetails.referencesToCommits(commit.getParents(), values));
                writer.addProperty(GitLexicon.TREE, GitTree.referenceToTree(objId, objId.name(), values));

                // Compute the difference between the commit and it's parent(s), and generate the diff/patch file ...
                List<DiffEntry> differences = computeDifferences(commit, walker, repository);
                String patchFile = computePatch(differences, repository);
                writer.addProperty(GitLexicon.DIFF, patchFile);

            } finally {
                walker.dispose();
            }
        } else {
            return null;
        }

        return writer.document();
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

    protected List<DiffEntry> computeDifferences( RevCommit commit,
                                                  RevWalk walker,
                                                  Repository repository )
        throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
        // Set up the tree walk to obtain the difference between the commit and it's parent(s) ...
        TreeWalk tw = new TreeWalk(repository);
        tw.setRecursive(true);
        tw.addTree(commit.getTree());

        RevCommit[] parents = commit.getParents();

        for (RevCommit parent : parents) {
            RevCommit parentCommit = walker.parseCommit(parent);
            tw.addTree(parentCommit.getTree());
            //if there are multiple parents, we can't really have a multiple-way diff so we'll only look at the first parent
            if (parents.length > 1) {
                connector.getLogger().warn(GitI18n.commitWithMultipleParents, commit.getName(), parentCommit.getName());
                break;
            }
        }

        if (tw.getTreeCount() == 1) {
            connector.getLogger().warn(GitI18n.commitWithSingleParent, commit.getName(), tw.getObjectId(0).name());
            return Collections.emptyList();
        }

        // Now process the diff of each file ...
        return DiffEntry.scan(tw);
    }

    protected String computePatch( Iterable<DiffEntry> entries,
                                   Repository repository ) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DiffFormatter formatter = new DiffFormatter(output);
        formatter.setRepository(repository);
        for (DiffEntry entry : entries) {
            formatter.format(entry);
        }
        return output.toString(DIFF_CHARSET_NAME);
    }

}
