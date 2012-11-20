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

import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.PageWriter;

/**
 * A {@link GitFunction} that returns the history information about the (latest) commits in a particular branch or tag. The
 * structure of this area of the repository is as follows:
 * 
 * <pre>
 *   /commits/{branchOrTagNameOrObjectId}/{objectId}
 * </pre>
 */
public class GitHistory extends GitFunction implements PageableGitFunction {

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

            // Generate the child references to the branches and tags ...
            addBranchesAsChildren(git, spec, writer);
            addTagsAsChildren(git, spec, writer);

        } else if (spec.parameterCount() == 1) {
            // This is the top-level "/commits/{branchOrTagNameOrObjectId}" node
            writer.setPrimaryType(GitLexicon.OBJECT);

            // Generate the child references to the (latest) commits on this branch/tag ...
            String branchOrTagNameOrObjectId = spec.parameter(1);
            ObjectId objId = repository.resolve(branchOrTagNameOrObjectId);
            RevWalk walker = new RevWalk(repository);
            try {
                RevCommit commit = walker.parseCommit(objId);
                LogCommand command = git.log();
                command.add(commit.getId());
                command.setMaxCount(pageSize);
                for (RevCommit rev : command.call()) {
                    String commitId = rev.getId().toString();
                    writer.addChild(spec.childId(commitId), commitId);
                }
                // Handle paging ...
                int totalNumberOfChildren = Integer.MAX_VALUE; // don't know how many (or at least it's very expensive)
                writer.addPage(spec.getParentId(), pageSize, pageSize, totalNumberOfChildren);
            } finally {
                walker.dispose();
            }

        } else if (spec.parameterCount() == 2) {
            // This is a specific commit in the history, via "/commits/{branchOrTagNameOrObjectId}/{objectId}"
            // so we need to show the commit information ...
            RevWalk walker = new RevWalk(repository);
            try {
                ObjectId objId = repository.resolve(spec.parameter(2));
                RevCommit commit = walker.parseCommit(objId);
                writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                writer.addProperty(GitLexicon.AUTHOR, commit.getAuthorIdent().getName());
                writer.addProperty(GitLexicon.COMMITTER, commit.getCommitterIdent().getName());
                writer.addProperty(GitLexicon.COMMITTED, values.dateFrom(commit.getCommitTime()));
                writer.addProperty(GitLexicon.TITLE, commit.getShortMessage());
                writer.addProperty(GitLexicon.MESSAGE, commit.getFullMessage().trim());
                writer.addProperty(GitLexicon.PARENTS, GitCommitDetails.referencesToCommits(commit.getParents(), values));
                writer.addProperty(GitLexicon.TREE, GitTree.referenceToTree(objId, values));
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
        String branchOrTagNameOrObjectId = spec.parameter(1);
        ObjectId objId = repository.resolve(branchOrTagNameOrObjectId);
        RevWalk walker = new RevWalk(repository);
        try {
            int offset = pageKey.getOffsetInt();
            RevCommit commit = walker.parseCommit(objId);
            LogCommand command = git.log();
            command.add(commit.getId());
            command.setSkip(offset);
            command.setMaxCount(pageSize);
            for (RevCommit rev : command.call()) {
                String commitId = rev.getId().toString();
                writer.addChild(spec.childId(commitId), commitId);
            }

            // Handle paging ...
            int totalNumberOfChildren = Integer.MAX_VALUE; // don't know how many (or at least it's very expensive)
            int nextOffset = offset + pageSize;
            writer.addPage(pageKey.getParentId(), nextOffset, pageSize, totalNumberOfChildren);
            return writer.document();
        } finally {
            walker.dispose();
        }
    }

    protected static String referenceToHistory( ObjectId id,
                                                String branchOrTagName,
                                                Values values ) {
        return ID + DELIMITER + branchOrTagName + DELIMITER + id.getName();
    }
}
