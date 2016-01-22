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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.PageWriter;

/**
 * 
 */
public abstract class GitFunction {

    protected static final String DELIMITER = "/";
    protected static final String REMOTE_BRANCH_PREFIX = "refs/remotes/";
    protected static final String LOCAL_BRANCH_PREFIX = "refs/heads/";
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

    private Set<String> remoteBranchPrefixes() {
        Set<String> prefixes = new HashSet<String>();
        for (String remoteName : connector.remoteNames()) {
            String prefix = remoteBranchPrefix(remoteName);
            prefixes.add(prefix);
        }
        return prefixes;
    }

    private String remoteBranchPrefix( String remoteName ) {
        return REMOTE_BRANCH_PREFIX + remoteName + "/";
    }

    /**
     * Obtain the name of the branch reference
     * 
     * @param branchName
     * @return the branch ref name
     */
    protected String branchRefForName( String branchName ) {
        String remoteName = connector.remoteName();
        return remoteName != null ? remoteBranchPrefix(remoteName) + branchName : LOCAL_BRANCH_PREFIX + branchName;
    }

    /**
     * Obtain the name of the branch reference
     * 
     * @param branchName
     * @param remoteName the name of the remote
     * @return the branch ref name
     */
    protected String branchRefForName( String branchName,
                                       String remoteName ) {
        return remoteBranchPrefix(remoteName) + branchName;
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
            for (String remoteName : connector.remoteNames()) {
                String branchRef = branchRefForName(branchOrTagOrCommitId, remoteName);
                objId = repository.resolve(branchRef);
                if (objId != null) break;
            }
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
        Set<String> remoteBranchPrefixes = remoteBranchPrefixes();
        if (remoteBranchPrefixes.isEmpty()) {
            // Generate the child references to the LOCAL branches, which will be sorted by name ...
            ListBranchCommand command = git.branchList();
            List<Ref> branches = command.call();
            // Reverse the sort of the branch names, since they might be version numbers ...
            Collections.sort(branches, REVERSE_REF_COMPARATOR);
            for (Ref ref : branches) {
                String name = ref.getName();
                name = name.replace(GitFunction.LOCAL_BRANCH_PREFIX, "");
                writer.addChild(spec.childId(name), name);
            }
            return;
        }
        // There is at least one REMOTE branch, so generate the child references to the REMOTE branches,
        // which will be sorted by name (by the command)...
        ListBranchCommand command = git.branchList();
        command.setListMode(ListMode.REMOTE);
        List<Ref> branches = command.call();
        // Reverse the sort of the branch names, since they might be version numbers ...
        Collections.sort(branches, REVERSE_REF_COMPARATOR);
        Set<String> uniqueNames = new HashSet<String>();
        for (Ref ref : branches) {
            String name = ref.getName();
            if (uniqueNames.contains(name)) continue;
            // We only want the branch if it matches one of the listed remotes ...
            boolean skip = false;
            for (String remoteBranchPrefix : remoteBranchPrefixes) {
                if (name.startsWith(remoteBranchPrefix)) {
                    // Remove the prefix ...
                    name = name.replaceFirst(remoteBranchPrefix, "");
                    break;
                }
                // Otherwise, it's a remote branch from a different remote that we don't want ...
                skip = true;
            }
            if (skip) continue;
            if (uniqueNames.add(name)) writer.addChild(spec.childId(name), name);
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

    protected boolean isQueryable( CallSpecification callSpec ) {
        // by default, a git function does not return queryable content
        return false;
    }

    protected String authorName( RevCommit commit ) {
        PersonIdent authorIdent = commit.getAuthorIdent();
        return authorIdent != null ? authorIdent.getName() : "<unknown>";
    }

    protected String commiterName( RevCommit commit ) {
        PersonIdent committerIdent = commit.getCommitterIdent();
        return committerIdent != null ? committerIdent.getName() : "<unknown>";
    }

    @Override
    public String toString() {
        return getName();
    }

}
