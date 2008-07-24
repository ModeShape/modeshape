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
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicCopyNodeCommand extends BasicGraphCommand implements CopyNodeCommand {

    private final Path oldPath;
    private final Path newPath;

    /**
     * @param oldPath the path to the original; may not be null
     * @param newPath the path to the copy; may not be null
     */
    public BasicCopyNodeCommand( Path oldPath,
                                 Path newPath ) {
        super();
        assert oldPath != null;
        assert newPath != null;
        this.oldPath = oldPath;
        this.newPath = newPath;
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
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " from " + this.getPath() + " to " + this.getNewPath();
    }

}
