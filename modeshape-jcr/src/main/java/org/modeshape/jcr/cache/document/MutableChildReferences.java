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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.LinkedListMultimap;
import org.modeshape.common.collection.ListMultimap;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;

/**
 * 
 */
@ThreadSafe
public class MutableChildReferences extends AbstractChildReferences {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ListMultimap<Name, ChildReference> childReferences;
    private final Map<NodeKey, ChildReference> childReferencesByKey;

    protected MutableChildReferences() {
        this.childReferences = LinkedListMultimap.create();
        this.childReferencesByKey = new HashMap<NodeKey, ChildReference>();
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
            return childReferences.get(name).size();
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
            List<ChildReference> childrenWithSameName = this.childReferences.get(name);
            if (childrenWithSameName.isEmpty()) {
                // This segment contains no nodes with the supplied name ...
                return null;
            }

            // We have at least one SNS in this list ...
            for (ChildReference childWithSameName : childrenWithSameName) {
                int index = context.consume(childWithSameName.getName(), childWithSameName.getKey());
                if (index == snsIndex) return childWithSameName.with(index);
            }
            return null;
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
                // It's in this list but there are no changes ...
                List<ChildReference> childrenWithSameName = this.childReferences.get(ref.getName());
                assert childrenWithSameName != null;
                assert childrenWithSameName.size() != 0;
                // Consume the child references until we find the reference ...
                for (ChildReference child : childrenWithSameName) {
                    int index = context.consume(child.getName(), child.getKey());
                    if (key.equals(child.getKey())) return child.with(index);
                }
            }
            return ref;
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
            // TODO: should this be a copy?
            return childReferences.values().iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<ChildReference> iterator( Name name ) {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            // TODO: should this be a copy?
            return childReferences.get(name).iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<NodeKey> getAllKeys() {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return new HashSet<NodeKey>(childReferencesByKey.keySet()).iterator();
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
            ChildReference old = this.childReferencesByKey.put(reference.getKey(), reference);
            if (old != null && old.getName().equals(name)) {
                // We already have this key/name pair, so we don't need to add it again ...
                return;
            }
            // We've not seen this node key yet, so it is okay. In fact, we should not see any
            // node key more than once, since that is clearly an unexpected condition (as a child
            // may not appear more than once in its parent's list of child nodes). See MODE-2120.
            this.childReferences.put(reference.getName(), reference);
        } finally {
            lock.unlock();
        }
    }

    public void append( Iterable<ChildReference> references ) {
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            for (ChildReference reference : references) {
                reference = reference.with(1);
                ChildReference old = this.childReferencesByKey.put(reference.getKey(), reference);
                if (old != null && old.getName().equals(reference.getName())) {
                    // We already have this key/name pair, so we don't need to add it again ...
                    continue;
                }
                // We've not seen this node key yet, so it is okay. In fact, we should not see any
                // node key more than once, since that is clearly an unexpected condition (as a child
                // may not appear more than once in its parent's list of child nodes). See MODE-2120.
                this.childReferences.put(reference.getName(), reference);
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
                this.childReferences.remove(existing.getName(), existing);
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
            Iterator<ChildReference> iter = childReferences.values().iterator();
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
}
