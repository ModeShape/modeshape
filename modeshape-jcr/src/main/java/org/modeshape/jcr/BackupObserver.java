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
package org.modeshape.jcr;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.BinaryValueUnused;
import org.modeshape.jcr.cache.change.BinaryValueUsed;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link ChangeSetListener} that captures the changes being made in a repository, forwarding the node and property changes to
 * the supplied queue, and capturing the set of binary keys that are marked as being used or unused.
 * 
 * @see BackupService
 */
@ThreadSafe
public class BackupObserver implements ChangeSetListener {
    private final Queue<NodeKey> changedNodes;
    private final Map<BinaryKey, Object> usedBinaryKeys = new ConcurrentHashMap<BinaryKey, Object>();
    private final Map<BinaryKey, Object> unusedBinaryKeys = new ConcurrentHashMap<BinaryKey, Object>();

    protected BackupObserver( Queue<NodeKey> changedNodes ) {
        this.changedNodes = changedNodes;
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null) return;
        // Add all of the changed nodes to the queue ...
        changedNodes.addAll(changeSet.changedNodes());

        // We only care about the binary changes; workspace changes will be handled by always writing out the
        // repository's metadata document, and node changes are handled above ...

        // Now process the binary changes ...
        for (Change change : changeSet) {
            // Look at property added/removed/changed events, and node add/removed/moved/renamed/reordered events ...
            if (change instanceof BinaryValueUnused) {
                BinaryValueUnused unused = (BinaryValueUnused)change;
                BinaryKey key = unused.getKey();
                if (usedBinaryKeys.containsKey(key)) {
                    // This change set had marked it as used, but now is unused again; removed it from the used.
                    usedBinaryKeys.remove(key);
                    break;
                }
                unusedBinaryKeys.put(key, null);
            } else if (change instanceof BinaryValueUsed) {
                BinaryValueUsed used = (BinaryValueUsed)change;
                BinaryKey key = used.getKey();
                if (unusedBinaryKeys.containsKey(key)) {
                    // This change set had marked it as unused, but now is used again; removed it from the unused.
                    unusedBinaryKeys.remove(key);
                    break;
                }
                usedBinaryKeys.put(key, null);
            }
        }
    }

    /**
     * Get the binary keys that have been marked as being unused during the time this observer was listening to the repository.
     * 
     * @return the unused binary keys; never null, but possibly empty
     */
    public Iterable<BinaryKey> getUnusedBinaryKeys() {
        return unusedBinaryKeys.keySet();
    }

    /**
     * Get the binary keys that have been marked as being used during the time this observer was listening to the repository.
     * 
     * @return the used binary keys; never null, but possibly empty
     */
    public Iterable<BinaryKey> getUsedBinaryKeys() {
        return usedBinaryKeys.keySet();
    }
}
