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
package org.modeshape.graph.connector.base.lock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.ReadRequest;
import org.modeshape.graph.request.Request;

/**
 * A lock strategy that locks at the granularity of individual workspaces in the request. This strategy allows concurrent writes
 * from different transactions, as long as each transaction is writing to a disjoint set of workspaces within the
 * {@link Repository}.
 */
public class WorkspaceLockStrategy implements LockStrategy {

    private final LockProvider provider = new JvmLockProvider();

    /**
     * {@inheritDoc}
     * 
     * @see LockStrategy#lock(Request)
     */
    public HeldLocks lock( Request request ) {
        Set<String> readWorkspaceNames = Collections.emptySet();
        Set<String> writeWorkspaceNames = Collections.emptySet();

        // Parse the request to see which workspaces are being read or modified
        if (request instanceof CompositeRequest) {
            readWorkspaceNames = new HashSet<String>();
            writeWorkspaceNames = new HashSet<String>();

            for (Request subrequest : ((CompositeRequest)request).getRequests()) {
                if (subrequest instanceof ReadRequest) {
                    readWorkspaceNames.add(((ReadRequest)subrequest).readWorkspace());
                } else if (subrequest instanceof ChangeRequest) {
                    writeWorkspaceNames.add(((ChangeRequest)subrequest).changedWorkspace());
                }
            }
        } else if (request instanceof ReadRequest) {
            writeWorkspaceNames = Collections.emptySet();
            readWorkspaceNames = Collections.singleton(((ReadRequest)request).readWorkspace());
            
        } else if (request instanceof ChangeRequest) {
            writeWorkspaceNames = Collections.singleton(((ChangeRequest)request).changedWorkspace());
            readWorkspaceNames = Collections.emptySet();
        }

        // Sort the affected workspaces into a canonical order to help avoid deadlocks
        List<String> sortedWriteWorkspaceNames = new ArrayList<String>(writeWorkspaceNames);
        Collections.sort(sortedWriteWorkspaceNames);

        List<String> sortedReadWorkspaceNames = new ArrayList<String>(readWorkspaceNames);
        Collections.sort(sortedReadWorkspaceNames);

        // Acquire the locks
        Collection<ModeShapeLock> heldLocks = new LinkedList<ModeShapeLock>();
        for (String writeWorkspaceName : sortedWriteWorkspaceNames) {
            ModeShapeLock lock = provider.writeLockFor(writeWorkspaceName);

            lock.lock();
            heldLocks.add(lock);
        }

        for (String readWorkspaceName : sortedReadWorkspaceNames) {
            ModeShapeLock lock = provider.readLockFor(readWorkspaceName);
            lock.lock();
            heldLocks.add(lock);
        }

        return new WorkspaceHeldLocks(heldLocks);
    }

    class WorkspaceHeldLocks implements HeldLocks {

        private final Iterable<? extends ModeShapeLock> locks;

        public WorkspaceHeldLocks( Iterable<? extends ModeShapeLock> locks ) {
            this.locks = locks;
        }

        public void release() {
            for (ModeShapeLock lock : locks) {
                lock.unlock();
            }
        }

    }
}
