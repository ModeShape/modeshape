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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;

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
