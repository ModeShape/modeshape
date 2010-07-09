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
package org.modeshape.connector.jbosscache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.jboss.cache.Cache;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.Repository;

/**
 * The representation of an in-memory repository and its content.
 */
@ThreadSafe
public class JBossCacheRepository extends Repository<JBossCacheNode, JBossCacheWorkspace> {

    private final Cache<UUID, JBossCacheNode> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> predefinedWorkspaceNames;

    public JBossCacheRepository( JBossCacheSource source,
                                 Cache<UUID, JBossCacheNode> cache ) {
        super(source);
        this.cache = cache;
        assert this.cache != null;
        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            workspaceNames.add(workspaceName);
        }
        this.predefinedWorkspaceNames = Collections.unmodifiableSet(workspaceNames);
        initialize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#getWorkspaceNames()
     */
    @Override
    public Set<String> getWorkspaceNames() {
        Set<String> names = new HashSet<String>(super.getWorkspaceNames());
        names.addAll(predefinedWorkspaceNames);
        return Collections.unmodifiableSet(names);
    }

    /**
     * @return cache
     */
    public Cache<UUID, JBossCacheNode> getCache() {
        return cache;
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#startTransaction(org.modeshape.graph.ExecutionContext, boolean)
     */
    @Override
    public JBossCacheTransaction startTransaction( ExecutionContext context,
                                                   boolean readonly ) {
        final Lock lock = readonly ? this.lock.readLock() : this.lock.writeLock();
        lock.lock();
        return new JBossCacheTransaction(context, this, getRootNodeUuid(), lock);
    }
}
