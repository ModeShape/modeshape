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
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * 
 */
public abstract class GitFunction {

    protected static final String DELIMITER = "/";
    protected static final String BRANCH_PREFIX = "refs/heads/";
    protected static final String TAG_PREFIX = "refs/tags/";

    protected final String name;
    protected final GitConnector connector;

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

    protected void addBranchesAsChildren( Git git,
                                          CallSpecification spec,
                                          DocumentWriter writer ) throws GitAPIException {
        // Generate the child references to the branches, which will be sorted by name (by the command).
        ListBranchCommand command = git.branchList();
        command.setListMode(null);
        for (Ref ref : command.call()) {
            String fullName = ref.getName();
            String name = fullName.replaceFirst(BRANCH_PREFIX, "");
            writer.addChild(spec.childId(name), name);
        }
    }

    protected void addTagsAsChildren( Git git,
                                      CallSpecification spec,
                                      DocumentWriter writer ) throws GitAPIException {
        // Generate the child references to the branches, which will be sorted by name (by the command).
        ListTagCommand command = git.tagList();
        for (Ref ref : command.call()) {
            String fullName = ref.getName();
            String name = fullName.replaceFirst(TAG_PREFIX, "");
            writer.addChild(spec.childId(name), name);
        }
    }

}
