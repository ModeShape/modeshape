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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;

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
            String branchName = spec.parameter(1);
            // Get the Ref, which doesn't directly know about the commit SHA1, so we have to parse the commit ...
            Ref ref = repository.getRef(branchName);
            if (ref == null) return null; // invalid branch
            RevWalk walker = new RevWalk(repository);
            try {
                RevCommit commit = walker.parseCommit(ref.getObjectId());
                // Construct the references to other nodes in this source ...
                ObjectId objId = commit.getId();
                writer.addProperty(GitLexicon.OBJECT_ID, objId.name());
                writer.addProperty(GitLexicon.TREE, GitTree.referenceToTree(objId, values));
                writer.addProperty(GitLexicon.HISTORY, GitHistory.referenceToHistory(objId, branchName, values));
            } finally {
                walker.dispose();
            }
        } else {
            return null;
        }

        return writer.document();
    }
}
