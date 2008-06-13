/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.commands.impl;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicMoveBranchCommand extends BasicGraphCommand implements MoveBranchCommand {

    private final Path oldPath;
    private final Path newPath;
    private final NodeConflictBehavior conflictBehavior;

    /**
     * @param oldPath the path to the original; may not be null
     * @param newPath the path to the new location; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     */
    public BasicMoveBranchCommand( Path oldPath,
                                   Path newPath,
                                   NodeConflictBehavior conflictBehavior ) {
        super();
        assert oldPath != null;
        assert newPath != null;
        assert conflictBehavior != null;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.conflictBehavior = conflictBehavior;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return oldPath;
    }

    /**
     * {@inheritDoc}
     */
    public Path getNewPath() {
        return newPath;
    }

    /**
     * {@inheritDoc}
     */
    public NodeConflictBehavior getConflictBehavior() {
        return conflictBehavior;
    }

}
