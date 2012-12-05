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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.PageWriter;

/**
 * 
 */
public abstract class GitFunction {

    protected static final String DELIMITER = "/";
    protected static final String REMOTE_BRANCH_PREFIX = "refs/remotes/";
    protected static final String TAG_PREFIX = "refs/tags/";
    protected static final int DEFAULT_PAGE_SIZE = 15;

    protected static final Comparator<Ref> REVERSE_REF_COMPARATOR = new Comparator<Ref>() {
        @Override
        public int compare( Ref o1,
                            Ref o2 ) {
            return 0 - o1.getName().compareTo(o2.getName());
        }
    };

    protected final String name;
    protected final GitConnector connector;
    protected int pageSize = DEFAULT_PAGE_SIZE;

    protected GitFunction( String name,
                           GitConnector connector ) {
        this.name = name;
        this.connector = connector;
    }

    /**
     * Get the name of this function.
     * 
     * @return the name; never null
     */
    public String getName() {
        return name;
    }

    public boolean isPaged() {
        return false;
    }

    public abstract Document execute( Repository repository,
                                      Git git,
                                      CallSpecification spec,
                                      DocumentWriter writer,
                                      Values values ) throws GitAPIException, IOException;

    private String remoteBranchPrefix() {
        String remoteName = connector.remoteName();
        return REMOTE_BRANCH_PREFIX + remoteName + "/";
    }

    /**
     * Obtain the name of the branch reference
     * 
     * @param branchName
     * @return the branch ref name
     */
    protected String branchRefForName( String branchName ) {
        return remoteBranchPrefix() + branchName;
    }

    /**
     * Resolve the branch name, tag name, or commit ID into the appropriate ObjectId. Note that the branch names are assumed to be
     * from the {@link GitConnector#remoteName() remote}.
     * 
     * @param repository the Repository object; may not be null
     * @param branchOrTagOrCommitId the branch name, tag name, or commit ID; may not be null
     * @return the resolved ObjectId, or null if the supplied string does not resolve to an object ID
     * @throws IOException if there is a problem reading the Git repository
     */
    protected ObjectId resolveBranchOrTagOrCommitId( Repository repository,
                                                     String branchOrTagOrCommitId ) throws IOException {
        ObjectId objId = repository.resolve(branchOrTagOrCommitId);
        if (objId == null) {
            String branchRef = branchRefForName(branchOrTagOrCommitId);
            objId = repository.resolve(branchRef);
        }
        return objId;
    }

    /**
     * Add the names of the branches as children of the current node.
     * 
     * @param git the Git object; may not be null
     * @param spec the call specification; may not be null
     * @param writer the document writer for the current node; may not be null
     * @throws GitAPIException if there is a problem accessing the Git repository
     */
    protected void addBranchesAsChildren( Git git,
                                          CallSpecification spec,
                                          DocumentWriter writer ) throws GitAPIException {
        // Generate the child references to the branches, which will be sorted by name (by the command).
        ListBranchCommand command = git.branchList();
        command.setListMode(ListMode.REMOTE);
        String remoteBranchPrefix = remoteBranchPrefix();
        List<Ref> branches = command.call();
        // Reverse the sort of the branch names, since they might be version numbers ...
        Collections.sort(branches, REVERSE_REF_COMPARATOR);
        for (Ref ref : branches) {
            String name = ref.getName();
            // We only want the branch if it matches the remote ...
            if (name.startsWith(remoteBranchPrefix)) {
                // Remove the prefix ...
                name = name.replaceFirst(remoteBranchPrefix, "");
            } else {
                // Otherwise, it's a remote branch from a different remote that we don't want ...
                continue;
            }
            writer.addChild(spec.childId(name), name);
        }
    }

    /**
     * Add the names of the tags as children of the current node.
     * 
     * @param git the Git object; may not be null
     * @param spec the call specification; may not be null
     * @param writer the document writer for the current node; may not be null
     * @throws GitAPIException if there is a problem accessing the Git repository
     */
    protected void addTagsAsChildren( Git git,
                                      CallSpecification spec,
                                      DocumentWriter writer ) throws GitAPIException {
        // Generate the child references to the branches, which will be sorted by name (by the command).
        ListTagCommand command = git.tagList();
        List<Ref> tags = command.call();
        // Reverse the sort of the branch names, since they might be version numbers ...
        Collections.sort(tags, REVERSE_REF_COMPARATOR);
        for (Ref ref : tags) {
            String fullName = ref.getName();
            String name = fullName.replaceFirst(TAG_PREFIX, "");
            writer.addChild(spec.childId(name), name);
        }
    }

    /**
     * Add the first page of commits in the history names of the tags as children of the current node.
     * 
     * @param git the Git object; may not be null
     * @param spec the call specification; may not be null
     * @param writer the document writer for the current node; may not be null
     * @param pageSize the number of commits to include, and the number of commits that will be in the next page (if there are
     *        more commits)
     * @throws GitAPIException if there is a problem accessing the Git repository
     */
    protected void addCommitsAsChildren( Git git,
                                         CallSpecification spec,
                                         DocumentWriter writer,
                                         int pageSize ) throws GitAPIException {
        // Add commits in the log ...
        LogCommand command = git.log();
        command.setSkip(0);
        command.setMaxCount(pageSize);

        // Add the first set of commits ...
        int actual = 0;
        String commitId = null;
        for (RevCommit commit : command.call()) {
            commitId = commit.getName();
            writer.addChild(spec.childId(commitId), commitId);
            ++actual;
        }
        if (actual == pageSize) {
            // We wrote the maximum number of commits, so there's (probably) another page ...
            writer.addPage(spec.getId(), commitId, pageSize, PageWriter.UNKNOWN_TOTAL_SIZE);
        }
    }

    /**
     * Add an additional page of commits in the history names of the tags as children of the current node.
     * 
     * @param git the Git object; may not be null
     * @param repository the Repository object; may not be null
     * @param spec the call specification; may not be null
     * @param writer the page writer for the current node; may not be null
     * @param pageKey the page key for this page; may not be null
     * @throws GitAPIException if there is a problem accessing the Git repository
     * @throws IOException if there is a problem reading the Git repository
     */
    protected void addCommitsAsPageOfChildren( Git git,
                                               Repository repository,
                                               CallSpecification spec,
                                               PageWriter writer,
                                               PageKey pageKey ) throws GitAPIException, IOException {
        RevWalk walker = new RevWalk(repository);
        try {
            // The offset is the ID of the last commit we read, so we'll need to skip the first commit
            String lastCommitIdName = pageKey.getOffsetString();
            ObjectId lastCommitId = repository.resolve(lastCommitIdName);
            int pageSize = (int)pageKey.getBlockSize();
            // int offset = pageKey.getOffsetInt();
            // int maxCount = pageSize + offset;
            LogCommand command = git.log();
            command.add(lastCommitId);
            command.setMaxCount(pageSize + 1);
            // Add the first set of commits ...
            int actual = 0;
            String commitId = null;
            for (RevCommit commit : command.call()) {
                commitId = commit.getName();
                if (commitId.equals(lastCommitIdName)) continue;
                writer.addChild(spec.childId(commitId), commitId);
                ++actual;
            }
            if (actual == pageSize) {
                assert commitId != null;
                // We wrote the maximum number of commits, so there's (probably) another page ...
                writer.addPage(pageKey.getParentId(), commitId, pageSize, PageWriter.UNKNOWN_TOTAL_SIZE);
            }
        } finally {
            walker.dispose();
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
