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
package org.modeshape.jcr.cache.change;

import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;

/**
 * 
 */
public class NodeMoved extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final NodeKey oldParent;
    private final NodeKey newParent;
    private final Path oldPath;

    public NodeMoved( NodeKey key,
                      NodeKey oldParent,
                      NodeKey newParent,
                      Path newPath,
                      Path oldPath ) {
        super(key, newPath);
        this.oldParent = oldParent;
        this.newParent = newParent;
        this.oldPath = oldPath;
        assert this.oldParent != null;
        assert this.newParent != null;
        assert this.newParent != this.oldParent;
    }

    /**
     * Get the parent under which the node now appears.
     * 
     * @return the new parent; never null
     */
    public NodeKey getNewParent() {
        return newParent;
    }

    /**
     * Get the parent under which the node formerly appeared.
     * 
     * @return the old parent; never null
     */
    public NodeKey getOldParent() {
        return oldParent;
    }

    /**
     * Get the new path for the node, if it is known
     * 
     * @return the new path; may be null if it is not known
     */
    public Path getNewPath() {
        return path;
    }

    /**
     * Get the old path for the node, if it is known
     * 
     * @return the old path; may be null if it is not known
     */
    public Path getOldPath() {
        return oldPath;
    }

    @Override
    public String toString() {
        return "Moved node '" + this.getKey() + "' to \"" + path + "\" (under '" + newParent + "') from \"" + oldPath
               + "\" (under '" + oldParent + "')";
    }
}
