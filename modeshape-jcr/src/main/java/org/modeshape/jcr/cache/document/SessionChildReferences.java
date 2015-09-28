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

import java.util.Iterator;
import java.util.Set;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;

/**
 * A {@link ChildReferences} implementation that projects a single, changeable view of the child references of a node, allowing
 * child references to be added to the end of the persisted state. This view always reflects the view's changes on top of the
 * current persisted list of child references, even when the persisted state is changed by other sessions.
 * <p>
 * For example, consider a node A that has two children, B and C. If a {@link SessionChildReferences} instance is created for A,
 * then it includes {@link ChildReference} for nodes B and C. If a new child node, D, is created and appended to the list, the
 * MutableChildReferences object will show three children: B, C and D. Before these changes are saved, however, another client
 * adds to node A a new node, E, and persists those changes. At that instant, the MutableChildReferences will see four children
 * for node A: the persisted children B, C and E, plus the transient node D that has not yet been saved.
 * </p>
 */
@ThreadSafe
public class SessionChildReferences extends AbstractChildReferences {

    private final ChildReferences persisted;
    private final MutableChildReferences appended;
    private final SessionNode.ChangedChildren changedChildren;
    private final boolean allowsSNS;

    public SessionChildReferences( ChildReferences persisted,
                                   MutableChildReferences appended,
                                   SessionNode.ChangedChildren changedChildren,
                                   boolean allowsSNS) {
        this.persisted = persisted != null ? persisted : ImmutableChildReferences.EMPTY_CHILD_REFERENCES;
        this.appended = appended;
        this.changedChildren = changedChildren;
        this.allowsSNS = allowsSNS;
    }

    @Override
    public long size() {
        return persisted.size() + (appended != null ? appended.size() : 0) - (changedChildren != null ? changedChildren.removalCount() : 0);
    }

    @Override
    public int getChildCount( Name name ) {
        int appendedChildren = appended != null ? appended.getChildCount(name) : 0;
        int persistedChildrenCount = 0;

        int removedChildren = 0;
        int renamedChildren = 0;
        int appendedThenReorderedChildren = 0;

        for (Iterator<ChildReference> childReferenceIterator = persisted.iterator(name); childReferenceIterator.hasNext(); ) {
            ChildReference persistedChild = childReferenceIterator.next();
            ++persistedChildrenCount;

            if (changedChildren != null) {
                if (changedChildren.isRemoved(persistedChild)) {
                    --removedChildren;
                    continue;
                }

                if (changedChildren.isRenamed(persistedChild)) {
                    --renamedChildren;
                }
            }
        }

        if (changedChildren != null) {
            appendedThenReorderedChildren = changedChildren.insertionCount(name);

            if (changedChildren.isRenamed(name)) {
                ++renamedChildren;
            }
        }
        return persistedChildrenCount + appendedChildren + appendedThenReorderedChildren + removedChildren + renamedChildren;
    }

    @Override
    public ChildReference getChild( Name name,
                                    int snsIndex,
                                    Context context ) {
        if (!allowsSNS && snsIndex > 1) {
            return null;
        }
        if (changedChildren != null && !changedChildren.isEmpty()) context = new WithChanges(context, changedChildren);
        // First look in the delegate references ...
        ChildReference ref = persisted.getChild(name, snsIndex, context);
        if (ref == null) {
            if (appended != null) {
                // Look in the added ...
                ref = appended.getChild(name, snsIndex, context);
                if (ref == null && changedChildren != null && changedChildren.insertionCount() > 0) {
                    //look in added and then reordered (which don't appear as appended)
                    Iterator<ChildInsertions> insertionsWithName = changedChildren.insertions(name);
                    if (insertionsWithName == null) {
                        return null;
                    }
                    while (insertionsWithName.hasNext()) {
                        for (ChildReference inserted : insertionsWithName.next().inserted()) {
                            if (inserted.getSnsIndex() == snsIndex) {
                                return inserted;
                            }
                        }
                    }
                }
            }
        }
        return ref;
    }

    @Override
    public boolean hasChild( NodeKey key ) {
        return persisted.hasChild(key) ||
               (appended != null && appended.hasChild(key)) ||
               (changedChildren != null && changedChildren.inserted(key) != null);
    }

    @Override
    public ChildReference getChild( NodeKey key ) {
        return getChild(key, defaultContext());
    }

    @Override
    public ChildReference getChild( NodeKey key,
                                    Context context ) {
        if (changedChildren != null && !changedChildren.isEmpty()) context = new WithChanges(context, changedChildren);
        // First look in the delegate references ...
        // Note that we don't know the name of the child yet, so we'll have to come back
        // to the persisted node after we know the name ...
        ChildReference ref = persisted.getChild(key, context);
        if (ref == null) {
            if (appended != null) {
                // Look in the added ...
                ref = appended.getChild(key, context);
                if (ref != null) {
                    // The node was appended, but we need to increment the SNS indexes
                    // by the number of same-named children in 'persisted' ...
                    int numSnsInPersisted = persisted.getChildCount(ref.getName());
                    if (numSnsInPersisted != 0) {
                        // There were some persisted with the same name, and we didn't find these
                        // when looking in the persisted node above. So adjust the SNS index ...

                        int numSnsInRemoved = 0;
                        if (changedChildren != null && !changedChildren.isEmpty()) {
                            Set<NodeKey> removals = changedChildren.getRemovals();
                            //we need to take into account that the same node might be removed (in case of an reorder to the end)
                            if (removals.contains(key)) {
                                numSnsInRemoved = 1;
                            } 
                            // for each of the existing (persisted) SNS we need to see if any of them were already removed
                            for (int i = 1; i <= numSnsInPersisted; i++) {
                                NodeKey persistedChildKey = persisted.getChild(ref.getName(), i).getKey();
                                if (!key.equals(persistedChildKey) && removals.contains(persistedChildKey)) {
                                    numSnsInRemoved++;
                                }
                            }
                        }
                        ref = ref.with(numSnsInPersisted + ref.getSnsIndex() - numSnsInRemoved);
                    }
                } else if (changedChildren != null && changedChildren.insertionCount() > 0) {
                    //it's not in the appended list, so check if there are any transient reordered nodes - appended and
                    //reordered at the same time - they would not appear in the "appended" list
                    ref = changedChildren.inserted(key);
                }
            }
        }
        return ref;
    }

    @Override
    public Iterator<ChildReference> iterator( Name name,
                                              Context context ) {
        if (changedChildren != null && !changedChildren.isEmpty()) context = new WithChanges(context, changedChildren);
        return createIterator(name, context);
    }

    protected Iterator<ChildReference> createIterator( final Name name,
                                                       final Context context ) {
        Iterator<ChildReference> firstIter = persisted.iterator(name, context);
        final MutableChildReferences appended = this.appended;
        Iterable<ChildReference> second = new Iterable<ChildReference>() {
            @Override
            public Iterator<ChildReference> iterator() {
                return appended.iterator(context, name);
            }
        };
        return appended == null ? firstIter : new UnionIterator<ChildReference>(firstIter, second);
    }

    @Override
    public Iterator<ChildReference> iterator( Context context ) {
        if (changedChildren != null && !changedChildren.isEmpty()) context = new WithChanges(context, changedChildren);
        return createIterator(context);
    }

    protected Iterator<ChildReference> createIterator( final Context context ) {
        Iterator<ChildReference> firstIter = persisted.iterator(context);
        final MutableChildReferences appended = this.appended;
        Iterable<ChildReference> second = new Iterable<ChildReference>() {
            @Override
            public Iterator<ChildReference> iterator() {
                return appended.iterator(context);
            }
        };
        return appended == null ? firstIter : new UnionIterator<ChildReference>(firstIter, second);
    }

    @Override
    public Iterator<NodeKey> getAllKeys() {
        Iterator<NodeKey> firstIter = persisted.getAllKeys();
        final MutableChildReferences appended = this.appended;
        Iterable<NodeKey> second = new Iterable<NodeKey>() {
            @Override
            public Iterator<NodeKey> iterator() {
                return appended.getAllKeys();
            }
        };
        return appended == null ? firstIter : new UnionIterator<NodeKey>(firstIter, second);
    }

    @Override
    public StringBuilder toString( StringBuilder sb ) {
        Iterator<ChildReference> iter = iterator();
        if (iter.hasNext()) {
            sb.append(iter.next());
            while (iter.hasNext()) {
                sb.append(", ");
                sb.append(iter.next());
            }
        }
        return sb;
    }

    @Override
    public boolean allowsSNS() {
        return allowsSNS;
    }
}
