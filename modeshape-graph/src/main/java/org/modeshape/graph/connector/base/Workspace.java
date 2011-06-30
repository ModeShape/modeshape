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
package org.modeshape.graph.connector.base;

import org.modeshape.graph.connector.base.cache.NoCachePolicy;

/**
 * The {@code MapWorkspace} stores state and other information about a workspace in a way that is independent of
 * {@link Transaction}s.
 */
public interface Workspace {

    /**
     * Returns the name of the workspace. There can only be one workspace with a given name per repository.
     * 
     * @return the name of the workspace
     */
    String getName();

    /**
     * Indicates whether this workspace has a node cache. This method should return if the implementation is wired for cache
     * support, even if it happens to be supporting a trivial implementation like {@link NoCachePolicy}.
     * 
     * @return true if this workspace has a node cache
     */
    boolean hasNodeCache();
}
