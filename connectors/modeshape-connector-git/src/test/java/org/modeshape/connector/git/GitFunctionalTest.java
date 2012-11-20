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
        values = new Values(context.getValueFactories());
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
        print = true;
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
        print = true;
        printTreeContent("modeshape-3.0.0.Final", "", true);
    }

    @Test
    public void shouldGetDirectoryContentsAtPathForCommit() throws Exception {
        print = true;
        printTreeContent("modeshape-3.0.0.Final", "modeshape-jcr/src", true);
    }

    @Test
    public void shouldGetFileInfoAtPathInContent() throws Exception {
        print = true;
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
                    print("commiter", lastCommit.getAuthorIdent().getName());
                }
            }
        } finally {
            walker.dispose();
        }
    }
}
