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
package org.modeshape.connector.infinispan;

import java.util.UUID;
import org.infinispan.Cache;
import org.modeshape.graph.connector.base.StandardMapWorkspace;

/**
 * 
 */
public class InfinispanWorkspace extends StandardMapWorkspace<InfinispanNode> {

    private Cache<UUID, InfinispanNode> workspaceCache;

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceCache the Infinispan cache containing the workspace content
     * @param rootNode the root node for the workspace
     */
    public InfinispanWorkspace( String name,
                                Cache<UUID, InfinispanNode> workspaceCache,
                                InfinispanNode rootNode ) {
        super(name, workspaceCache, rootNode);
        this.workspaceCache = workspaceCache;
    }

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceCache the Infinispan cache containing the workspace content
     * @param originalToClone the workspace that is to be cloned
     */
    public InfinispanWorkspace( String name,
                                Cache<UUID, InfinispanNode> workspaceCache,
                                InfinispanWorkspace originalToClone ) {
        super(name, workspaceCache, originalToClone);
        this.workspaceCache = workspaceCache;
    }

    public void destroy() {
        this.workspaceCache.clear();
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
        this.workspaceCache.stop();
    }

}
