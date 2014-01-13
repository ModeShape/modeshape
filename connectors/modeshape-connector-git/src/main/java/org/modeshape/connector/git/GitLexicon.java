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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * Lexicon of names from the "<code>http://www.modeshape.org/git/1.0</code>" namespace used in the Git connector.
 */
@Immutable
public class GitLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/git/1.0";
        public static final String PREFIX = "git";
    }

    public static final Name ROOT = new BasicName(Namespace.URI, "root");
    public static final Name BRANCHES = new BasicName(Namespace.URI, "branches");
    public static final Name BRANCH = new BasicName(Namespace.URI, "branch");
    public static final Name TAGS = new BasicName(Namespace.URI, "tags");
    public static final Name TAG = new BasicName(Namespace.URI, "tag");
    public static final Name COMMITS = new BasicName(Namespace.URI, "commits");
    public static final Name COMMIT = new BasicName(Namespace.URI, "commit");
    public static final Name OBJECT = new BasicName(Namespace.URI, "object");
    public static final Name TREES = new BasicName(Namespace.URI, "trees");
    public static final Name FOLDER = new BasicName(Namespace.URI, "folder");
    public static final Name FILE = new BasicName(Namespace.URI, "file");
    public static final Name RESOURCE = new BasicName(Namespace.URI, "resource");
    public static final Name DETAILS = new BasicName(Namespace.URI, "details");
    public static final Name DETAILED_COMMIT = new BasicName(Namespace.URI, "detailedCommit");

    public static final Name OBJECT_ID = new BasicName(Namespace.URI, "objectId");
    public static final Name AUTHOR = new BasicName(Namespace.URI, "author");
    public static final Name COMMITTER = new BasicName(Namespace.URI, "committer");
    public static final Name COMMITTED = new BasicName(Namespace.URI, "committed");
    public static final Name TITLE = new BasicName(Namespace.URI, "title");
    public static final Name MESSAGE = new BasicName(Namespace.URI, "message");
    public static final Name PARENTS = new BasicName(Namespace.URI, "parents");

    /** Property names */
    public static final Name HISTORY = new BasicName(Namespace.URI, "history");
    public static final Name DIFF = new BasicName(Namespace.URI, "diff");
    public static final Name TREE = new BasicName(Namespace.URI, "tree");
    public static final Name DETAIL = new BasicName(Namespace.URI, "detail");
}
