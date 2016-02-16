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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;

/**
 * 
 */
public class GitRoot extends GitFunction {

    protected static final String ID = CallSpecification.DELIMITER_STR;
    protected static final String NAME = "";

    private final Document root;

    public GitRoot( GitConnector connector ) {
        super(NAME, connector);
        DocumentWriter writer = connector.newDocumentWriter(ID);
        writer.setPrimaryType(GitLexicon.ROOT);
        writer.addChild(GitBranches.ID, GitBranches.NAME);
        writer.addChild(GitTags.ID, GitTags.NAME);
        writer.addChild(GitHistory.ID, GitHistory.NAME);
        writer.addChild(GitCommitDetails.ID, GitCommitDetails.NAME);
        writer.addChild(GitTree.ID, GitTree.NAME);
        root = writer.document();
    }

    @Override
    public Document execute( Repository repository,
                             Git git,
                             CallSpecification spec,
                             DocumentWriter writer,
                             Values values ) {
        return root.clone(); // return a copy
    }
}
