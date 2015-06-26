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
package org.modeshape.jcr.cache.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;

/**
 * 
 */
@ThreadSafe
public class MutableChildReferences extends AbstractChildReferences {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Name, List<NodeKey>> childReferenceKeysByName;
    private final Map<NodeKey, ChildReference> childReferencesByKey;

    protected MutableChildReferences() {
        this.childReferenceKeysByName = new HashMap<>();
        this.childReferencesByKey = new LinkedHashMap<>();
    }

    @Override
    public long size() {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return childReferencesByKey.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getChildCount( Name name ) {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            List<NodeKey> nodeKeys = childReferenceKeysByName.get(name);
            return nodeKeys != null ? nodeKeys.size() : 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ChildReference getChild( Name name,
                                    int snsIndex,
                                    Context context ) {
        if (context == null) context = new SingleNameContext();
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            List<NodeKey> childrenKeysWithSameName = this.childReferenceKeysByName.get(name);
            if (childrenKeysWithSameName == null || childrenKeysWithSameName.isEmpty()) {
                // This segment contains no nodes with the supplied name ...
                return null;
            }

            // there are no changes, so we can optimize this lookup
            if (snsIndex > childrenKeysWithSameName.size()) {
                return null;
            } else {
                NodeKey nodeKey = childrenKeysWithSameName.get(snsIndex - 1);
                return this.childReferencesByKey.get(nodeKey).with(snsIndex);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ChildReference getChild( NodeKey key,
                                    Context context ) {
        if (context == null) context = new SingleNameContext();
        Lock lock = this.lock.readLock();
        try {
            lock.lock();

            ChildReference ref = childReferencesByKey.get(key);
            if (ref != null) {
                // It's in this list but there may be changes ...
                List<NodeKey> childrenKeysWithSameName = this.childReferenceKeysByName.get(ref.getName());
                assert childrenKeysWithSameName != null;
                assert childrenKeysWithSameName.size() != 0;
                int index = childrenKeysWithSameName.indexOf(key);
                return ref.with(index + 1);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ChildReference getChild( NodeKey key ) {
        return getChild(key, new BasicContext());
    }

    @Override
    public boolean hasChild( NodeKey key ) {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return childReferencesByKey.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<ChildReference> iterator() {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();   
            // we need to copy to be thread safe
            return new ArrayList<>(childReferencesByKey.values()).iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<ChildReference> iterator( Name name ) {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            final List<NodeKey> nodeKeys = childReferenceKeysByName.get(name);
            if (nodeKeys == null || nodeKeys.isEmpty()) {
                return Collections.emptyIterator();
            }
            List<ChildReference> childReferences = new ArrayList<>(nodeKeys.size());
            for (ChildReference childReference : childReferencesByKey.values()) {
                if (name.equals(childReference.getName())) {
                    childReferences.add(childReference);
                }
            }
            return childReferences.iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<NodeKey> getAllKeys() {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            //we need to copy to be thread safe
            return new ArrayList<>(childReferencesByKey.keySet()).iterator();
        } finally {
            lock.unlock();
        }
    }

    public void append( NodeKey key,
                        Name name ) {
        ChildReference reference = new ChildReference(key, name, 1);
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            ChildReference old = this.childReferencesByKey.put(key, reference);
            if (old != null && old.getName().equals(name)) {
                // We already have this key/name pair, so we don't need to add it again ...
                return;
            }
            // We've not seen this node key yet, so it is okay. In fact, we should not see any
            // node key more than once, since that is clearly an unexpected condition (as a child
            // may not appear more than once in its parent's list of child nodes). See MODE-2120.
            List<NodeKey> nodeKeysWithSameName = this.childReferenceKeysByName.get(name);
            if (nodeKeysWithSameName == null) {
                nodeKeysWithSameName = new ArrayList<>();
                this.childReferenceKeysByName.put(name, nodeKeysWithSameName);
            }
            nodeKeysWithSameName.add(key);
        } finally {
            lock.unlock();
        }
    }

    public void append( Iterable<ChildReference> references ) {
        Iterator<ChildReference> childReferenceIterator = references.iterator();
        if (!childReferenceIterator.hasNext()) {
            return;
        }
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            while (childReferenceIterator.hasNext()) {
                ChildReference reference = childReferenceIterator.next();
                reference = reference.with(1);
                ChildReference old = this.childReferencesByKey.put(reference.getKey(), reference);
                if (old != null && old.getName().equals(reference.getName())) {
                    // We already have this key/name pair, so we don't need to add it again ...
                    continue;
                }
                // We've not seen this node key yet, so it is okay. In fact, we should not see any
                // node key more than once, since that is clearly an unexpected condition (as a child
                // may not appear more than once in its parent's list of child nodes). See MODE-2120.
                Name name = reference.getName();
                List<NodeKey> nodeKeysWithSameName = this.childReferenceKeysByName.get(name);
                if (nodeKeysWithSameName == null) {
                    nodeKeysWithSameName = new ArrayList<>();
                    this.childReferenceKeysByName.put(name, nodeKeysWithSameName);
                }
                nodeKeysWithSameName.add(reference.getKey());
            }
        } finally {
            lock.unlock();
        }
    }

    public ChildReference remove( NodeKey key ) {
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            ChildReference existing = this.childReferencesByKey.remove(key);
            if (existing != null) {
                List<NodeKey> nodeKeys = this.childReferenceKeysByName.get(existing.getName());
                assert nodeKeys != null;
                nodeKeys.remove(key);
            }
            return existing;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public StringBuilder toString( StringBuilder sb ) {
        sb.append("appended: ");
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            Iterator<ChildReference> iter = this.childReferencesByKey.values().iterator();
            if (iter.hasNext()) {
                sb.append(iter.next());
                while (iter.hasNext()) {
                    sb.append(", ");
                    sb.append(iter.next());
                }
            }
        } finally {
            lock.unlock();
        }
        return sb;
    }

    @Override
    public boolean allowsSNS() {
        // we don't really have any way of knowing SNS information here, so assume true to cover all cases
        return true;
    }
}
