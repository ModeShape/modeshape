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
import java.io.File;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;

public class GitFunctionalTest {

    private static Git git;
    private static Repository repository;
    private static ExecutionContext context;
    private static Values values;

    private boolean print = false;

    @BeforeClass
    public static void beforeAll() throws Exception {
        File gitDir = new File("../../.git");

        RepositoryBuilder builder = new RepositoryBuilder();
        repository = builder.setGitDir(gitDir).readEnvironment().findGitDir().build();

        git = new Git(repository);

        context = new ExecutionContext();
        values = new Values(context.getValueFactories(), context.getBinaryStore());
    }

    @Before
    public void beforeEach() {
        print = false;
    }

    protected void print( String message ) {
        if (!print) return;
        System.out.println(message);
    }

    protected void print( String prefix,
                          Object obj ) {
        if (!print) return;
        System.out.println(prefix + obj.toString());
    }

    protected void print( String prefix,
                          Object... objects ) {
        if (!print) return;
        System.out.print(prefix + ": ");
        for (Object obj : objects) {
            System.out.print(obj.toString());
        }
        System.out.println();
    }

    protected void print( RevCommit commit ) {
        if (!print) return;
        System.out.println(commit.getId().name());
        PersonIdent committer = commit.getCommitterIdent();
        PersonIdent author = commit.getAuthorIdent();
        System.out.println("   Author    = " + author);
        System.out.println("   Committer = " + committer);
        System.out.println("   Committed = " + values.dateFrom(commit.getCommitTime()));
        System.out.println("   Title     = " + commit.getShortMessage());
        System.out.println("   Message   = " + commit.getFullMessage().trim());
        System.out.println("   Parents   = " + commit.getParents());
    }

    @Test
    public void shouldGetBranchesWithLocalMode() throws Exception {
        // print = true;
        ListMode mode = null;
        ListBranchCommand command = git.branchList();
        command.setListMode(mode);
        for (Ref ref : command.call()) {
            String fullName = ref.getName();
            String name = fullName.replaceFirst("refs/heads/", "");
            print(fullName + " \t--> " + name);
        }
    }

    @Test
    public void shouldGetBranchesWithAllMode() throws Exception {
        // print = true;
        ListMode mode = ListMode.ALL;
        ListBranchCommand command = git.branchList();
        command.setListMode(mode);
        for (Ref ref : command.call()) {
            print(ref.getName());
        }
    }

    @Test
    public void shouldGetTags() throws Exception {
        // print = true;
        ListTagCommand command = git.tagList();
        for (Ref ref : command.call()) {
            String fullName = ref.getName();
            String name = fullName.replaceFirst("refs/tags/", "");
            print(fullName + " \t--> " + name);
        }
    }

    @Test
    public void shouldGetFirstDozenCommitsInHistoryForTag() throws Exception {
        Ref ref = repository.getRef("modeshape-3.0.0.Final");
        ref = repository.peel(ref);
        RevWalk walker = new RevWalk(repository);
        walker.setRetainBody(true);
        try {
            RevCommit commit = walker.parseCommit(ref.getObjectId());
            LogCommand command = git.log();
            command.add(commit.getId());
            command.setMaxCount(12);
            for (RevCommit rev : command.call()) {
                commit = walker.parseCommit(rev);
                print(commit);
            }
        } finally {
            walker.dispose();
        }
    }

    @Test
    public void shouldComputeTheDiffOfACommit() throws Exception {
        // print = true;
        // Find the commit ...
        Ref ref = repository.getRef("modeshape-3.0.0.Final");
        ref = repository.peel(ref);
        RevWalk walker = new RevWalk(repository);
        walker.setRetainBody(true);
        try {
            RevCommit commit = walker.parseCommit(ref.getObjectId());

            // Set up the tree walk to obtain the difference between the commit and it's parent(s) ...
            TreeWalk tw = new TreeWalk(repository);
            tw.setRecursive(true);
            tw.addTree(commit.getTree());
            for (RevCommit parent : commit.getParents()) {
                RevCommit parentCommit = walker.parseCommit(parent);
                tw.addTree(parentCommit.getTree());
            }

            // Now process the diff of each file ...
            for (DiffEntry fileDiff : DiffEntry.scan(tw)) {
                ChangeType type = fileDiff.getChangeType();
                switch (type) {
                    case ADD:
                        String newPath = fileDiff.getNewPath();
                        print("ADD   ", newPath);
                        break;
                    case COPY:
                        newPath = fileDiff.getNewPath();
                        String origPath = fileDiff.getOldPath();
                        print("COPY   ", origPath, " -> ", newPath);
                        break;
                    case DELETE:
                        origPath = fileDiff.getOldPath();
                        print("DELETE ", origPath);
                        break;
                    case MODIFY:
                        newPath = fileDiff.getNewPath();
                        print("MODIFY ", newPath);
                        break;
                    case RENAME:
                        newPath = fileDiff.getNewPath();
                        origPath = fileDiff.getOldPath();
                        print("RENAME ", origPath, " -> ", newPath);
                        break;
                    default:
                        // skip
                        break;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                DiffFormatter formatter = new DiffFormatter(output);
                formatter.setRepository(repository);
                formatter.format(fileDiff);
                String diff = output.toString("UTF-8");
                print(diff);
            }
        } finally {
            walker.dispose();
        }
    }

    @Test
    public void shouldGetTopLevelDirectoryContentForCommit() throws Exception {
        printTreeContent("modeshape-3.0.0.Final", "", true);
    }

    @Test
    public void shouldGetDirectoryContentsAtPathForCommit() throws Exception {
        printTreeContent("modeshape-3.0.0.Final", "modeshape-jcr/src", true);
    }

    @Test
    public void shouldGetFileInfoAtPathInContent() throws Exception {
        printTreeContent("modeshape-3.0.0.Final", "modeshape-jcr/src/main/java/org/modeshape/jcr/XmlNodeTypeReader.java", false);
    }

    protected void printTreeContent( String tagOrBranchOrCommit,
                                     String parentPath,
                                     boolean showCommitInfo ) throws Exception {
        // Find the commit ...
        ObjectId objId = repository.resolve("modeshape-3.0.0.Final");
        RevWalk walker = new RevWalk(repository);
        if (showCommitInfo) {
            walker.setRetainBody(true);
        }
        try {
            RevCommit commit = walker.parseCommit(objId);
            if (showCommitInfo) print(commit);
            final TreeWalk tw = new TreeWalk(repository);
            tw.addTree(commit.getTree());
            if ("".equals(parentPath) || "/".equals(parentPath)) {
                // We're already at the top-level
                tw.setRecursive(false);
                print("Getting contents of path ...");
                while (tw.next()) {
                    print(tw.getPathString());
                }
            } else {
                PathFilter filter = PathFilter.create(parentPath);
                tw.setFilter(filter);
                print("Finding path ...");
                while (tw.next()) {
                    print(tw.getPathString());
                    if (filter.isDone(tw)) {
                        break;
                    } else if (tw.isSubtree()) {
                        tw.enterSubtree();
                    }
                }
                if (tw.isSubtree()) {
                    print("Getting contents of path ...");
                    tw.enterSubtree();
                    while (tw.next()) {
                        print(tw.getPathString());
                    }
                } else {
                    print("File: " + tw.getPathString());
                    // Find the commit that last modified this file ...
                    // Is this the most efficient way to do this, 'cuz it's expensive?
                    RevCommit lastCommit = git.log().addPath(parentPath).call().iterator().next();
                    print("commitMessage", lastCommit.getShortMessage());
                    PersonIdent authorIdent = lastCommit.getAuthorIdent();
                    if (authorIdent != null) {
                        print("commiter", authorIdent.getName());
                    }
                }
            }
        } finally {
            walker.dispose();
        }
    }
}
