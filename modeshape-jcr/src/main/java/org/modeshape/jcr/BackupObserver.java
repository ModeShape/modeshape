/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
