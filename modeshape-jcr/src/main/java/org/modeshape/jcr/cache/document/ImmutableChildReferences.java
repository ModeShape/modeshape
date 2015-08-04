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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.EmptyIterator;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentTranslator.ChildReferencesInfo;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 *
 */
public class ImmutableChildReferences {

    protected static final ChildReferences EMPTY_CHILD_REFERENCES = EmptyChildReferences.INSTANCE;
    protected static final Iterator<ChildReference> EMPTY_ITERATOR = new EmptyIterator<ChildReference>();
    protected static final Iterator<NodeKey> EMPTY_KEY_ITERATOR = new EmptyIterator<NodeKey>();

    public static ChildReferences create( DocumentTranslator documentTranslator,
                                          Document document,
                                          String childrenFieldName,
                                          boolean allowsSNS) {
        return new Medium(documentTranslator, document, childrenFieldName, allowsSNS);
    }

    public static ChildReferences union( ChildReferences first,
                                         ChildReferences second ) {
        long firstSize = first.size();
        long secondSize = second.size();
        if (firstSize == 0 && secondSize == 0) {
            return EMPTY_CHILD_REFERENCES;
        } else if (firstSize == 0) {
            return second;
        } else if (secondSize == 0) {
            return first;
        }
        return new ReferencesUnion(first, second);
    }

    public static ChildReferences create( ChildReferences first,
                                          ChildReferencesInfo firstSegmentingInfo,
                                          WorkspaceCache cache,
                                          boolean allowsSNS) {
        if (firstSegmentingInfo == null || firstSegmentingInfo.nextKey == null) return first;
        return new Segmented(cache, first, firstSegmentingInfo, allowsSNS);
    }

    public static ChildReferences create( ChildReferences first,
                                          ChildReferencesInfo firstSegmentingInfo,
                                          ChildReferences second,
                                          WorkspaceCache cache,
                                          boolean allowsSNS) {
        if (firstSegmentingInfo == null || firstSegmentingInfo.nextKey == null) return union(first, second);
        Segmented segmentedReferences = new Segmented(cache, first, firstSegmentingInfo, allowsSNS);
        return union(segmentedReferences, second);
    }

    @Immutable
    protected final static class EmptyChildReferences implements ChildReferences {

        public static final ChildReferences INSTANCE = new EmptyChildReferences();

        private EmptyChildReferences() {
        }

        @Override
        public boolean supportsGetChildReferenceByKey() {
            return true;
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int getChildCount( Name name ) {
            return 0;
        }

        @Override
        public ChildReference getChild( Name name ) {
            return null;
        }

        @Override
        public ChildReference getChild( Name name,
                                        int snsIndex ) {
            return null;
        }

        @Override
        public ChildReference getChild( Name name,
                                        int snsIndex,
                                        Context context ) {
            return null;
        }

        @Override
        public ChildReference getChild( org.modeshape.jcr.value.Path.Segment segment ) {
            return null;
        }

        @Override
        public ChildReference getChild( NodeKey key ) {
            return null;
        }

        @Override
        public ChildReference getChild( NodeKey key,
                                        Context context ) {
            return null;
        }

        @Override
        public boolean hasChild( NodeKey key ) {
            return false;
        }

        @Override
        public Iterator<ChildReference> iterator() {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<ChildReference> iterator( Name name ) {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<ChildReference> iterator( Name name,
                                                  Context context ) {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<ChildReference> iterator( Context context ) {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<ChildReference> iterator( Collection<?> namePatterns,
                                                  NamespaceRegistry registry ) {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<ChildReference> iterator( Context context,
                                                  Collection<?> namePatterns,
                                                  NamespaceRegistry registry ) {
            return EMPTY_ITERATOR;
        }

        @Override
        public Iterator<NodeKey> getAllKeys() {
            return EMPTY_KEY_ITERATOR;
        }

        @Override
        public boolean allowsSNS() {
            return false;
        }
    }

    @Immutable
    protected static final class Medium extends AbstractChildReferences {

        private final Map<NodeKey, ChildReference> childReferencesByKey;
        private final Map<Name, List<NodeKey>> childKeysByName;
        private final boolean allowsSNS;

        protected Medium( DocumentTranslator documentTranslator,
                          Document document,
                          String childrenFieldName,
                          boolean allowsSNS) {
            this.allowsSNS = allowsSNS;
            this.childKeysByName = new HashMap<>();
            this.childReferencesByKey = new LinkedHashMap<>();

            final List<?> documentArray = document.getArray(childrenFieldName);
            if (documentArray == null || documentArray.isEmpty()) {
                return;
            }
            for (Object value : documentArray) {
                ChildReference ref = documentTranslator.childReferenceFrom(value);
                if (ref == null) {
                    continue;
                }
                Name refName = ref.getName();
                NodeKey refKey = ref.getKey();
                ChildReference old = this.childReferencesByKey.get(refKey);
                if (old != null && old.getName().equals(ref.getName())) {
                    // We already have this key/name pair, so we don't need to add it again ...
                    continue;
                }
                // We've not seen this NodeKey/Name combination yet, so it is okay. In fact, we should not see any
                // node key more than once, since that is clearly an unexpected condition (as a child may not appear
                // more than once in its parent's list of child nodes). See MODE-2120.

                // we can precompute the SNS index up front
                List<NodeKey> keysWithName = this.childKeysByName.get(refName);
                if (allowsSNS) {
                    int currentSNS = keysWithName != null ? this.childKeysByName.get(refName).size() : 0;
                    ChildReference refWithSNS = ref.with(currentSNS + 1);
                    this.childReferencesByKey.put(refKey, refWithSNS);
                } else {
                    this.childReferencesByKey.put(refKey, ref);
                }
               
                if (keysWithName == null) {
                    keysWithName = new ArrayList<>();
                    this.childKeysByName.put(refName, keysWithName);
                }
                keysWithName.add(refKey);
            }
           
        }

        @Override
        public long size() {
            return childReferencesByKey.size();
        }

        @Override
        public int getChildCount( Name name ) {
            List<NodeKey> nodeKeys = childKeysByName.get(name);
            return nodeKeys != null ? nodeKeys.size() : 0;
        }

        @Override
        public ChildReference getChild( Name name,
                                        int snsIndex,
                                        Context context ) {
            if (!allowsSNS && snsIndex > 1) {
                return null;
            }
            Changes changes = null;
            Iterator<ChildInsertions> insertions = null;
            boolean includeRenames = false;
            if (context == null) {
                context = new SingleNameContext();
            } else {
                changes = context.changes(); // may still be null
                if (changes != null) {
                    insertions = changes.insertions(name);
                    includeRenames = changes.isRenamed(name);
                }
            }

            List<NodeKey> childrenWithSameName = this.childKeysByName.get(name);
            if (changes == null) {
                if (childrenWithSameName == null) {
                    return null;
                }
                // there are no changes, so we can take advantage of the fact that we precomputed the SNS indexes up front...
                if (snsIndex > childrenWithSameName.size()) {
                    return null;
                }
                NodeKey childKey = childrenWithSameName.get(snsIndex - 1);
                return childReferencesByKey.get(childKey);
            }
            // there are changes, so there is some extra processing to be done...

            if (childrenWithSameName == null && !includeRenames) {
                // This segment contains no nodes with the supplied name ...
                if (insertions == null) {
                    // and no nodes with this name were inserted ...
                    return null;
                }
                // But there is at least one inserted node with this name ...
                while (insertions.hasNext()) {
                    ChildInsertions inserted = insertions.next();
                    Iterator<ChildReference> iter = inserted.inserted().iterator();
                    while (iter.hasNext()) {
                        ChildReference result = iter.next();
                        int index = context.consume(result.getName(), result.getKey());
                        if (index == snsIndex) return result.with(index);
                    }
                }
                return null;
            }

            // This collection contains at least one node with the same name ...
            if (insertions != null || includeRenames) {
                // The logic for this would involve iteration to find the indexes, so we may as well just iterate ...
                Iterator<ChildReference> iter = iterator(context, name);
                while (iter.hasNext()) {
                    ChildReference ref = iter.next();
                    if (ref.getSnsIndex() == snsIndex) return ref;
                }
                return null;
            }

            // We have at least one SNS in this list (and still potentially some removals) ...
            for (NodeKey key : childrenWithSameName) {
                ChildReference childWithSameName = childReferencesByKey.get(key);
                if (changes.isRemoved(childWithSameName)) {
                    continue;
                }
                if (changes.isRenamed(childWithSameName)) {
                    continue;
                }
                // we've already precomputed the SNS index
                if (snsIndex == childWithSameName.getSnsIndex()) {
                    return childWithSameName;
                }
            }
            return null;
        }

        @Override
        public ChildReference getChild( NodeKey key,
                                        Context context ) {
            ChildReference ref = childReferencesByKey.get(key);
            if (ref == null) {
                // Not in our list, so check the context for changes ...
                if (context != null) {
                    Changes changes = context.changes();
                    if (changes != null) {
                        ref = changes.inserted(key);
                        if (ref != null) {
                            // The requested node was inserted, so figure out the SNS index.
                            // Unfortunately, this would require iteration (to figure out the indexes), so the
                            // easiest/fastest way is (surprisingly) to just iterate ...
                            Iterator<ChildReference> iter = iterator(context, ref.getName());
                            while (iter.hasNext()) {
                                ChildReference child = iter.next();
                                if (child.getKey().equals(key)) return child;
                            }
                            // Shouldn't really happen ...
                        }
                    }
                    // Not in list and no changes, so return ...
                }
            } else {
                // It's in our list, but if there are changes we need to use the context ...
                if (context != null) {
                    Changes changes = context.changes();
                    if (changes != null) {
                        boolean renamed = false;
                        if (changes.isRenamed(ref)) {
                            // The node was renamed, so get the new name ...
                            Name newName = changes.renamed(key);
                            ref = ref.with(newName, 1);
                            renamed = true;
                        }
                        Iterator<ChildInsertions> insertions = changes.insertions(ref.getName());
                        if (insertions != null || renamed) {
                            // We're looking for a node that was not inserted but has the same name as those that are
                            // (perhaps have renaming). As painful as it is, the easiest and most-efficient way to get
                            // this is to iterate ...
                            Iterator<ChildReference> iter = iterator(context, ref.getName());
                            while (iter.hasNext()) {
                                ChildReference child = iter.next();
                                if (child.getKey().equals(key)) return child;
                            }
                        }

                        // check if the node was removed only at the end to that insertions before can be processed first
                        if (changes.isRemoved(ref)) {
                            // The node was removed ...
                            return null;
                        }
                    }
                }
            }
            return ref;
        }

        @Override
        public ChildReference getChild( NodeKey key ) {
            // we should already have precomputed the correct SNS at the beginning
            return childReferencesByKey.get(key);
        }

        @Override
        public boolean hasChild( NodeKey key ) {
            return childReferencesByKey.containsKey(key);
        }

        @Override
        public Iterator<ChildReference> iterator( Name name ) {
            // the child references should already have the correct SNS precomputed
            List<NodeKey> childKeys = childKeysByName.get(name);
            if (childKeys == null || childKeys.isEmpty()) {
                return Collections.emptyIterator();
            }
            final Iterator<NodeKey> childKeysIterator = childKeys.iterator();
            return new Iterator<ChildReference>() {
                @Override
                public boolean hasNext() {
                    return childKeysIterator.hasNext();
                }

                @Override
                public ChildReference next() {
                    return childReferencesByKey.get(childKeysIterator.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Iterator<ChildReference> iterator() {
            // the child references should already have the correct SNS precomputed
            return childReferencesByKey.values().iterator();
        }

        @Override
        public Iterator<ChildReference> iterator( Name name,
                                                  Context context ) {
            if (context != null && context.changes() != null) {
                // we only want the context-sensitive behavior if there are changes. Otherwise we've already precomputed the SNS
                // index
                return super.iterator(name, context);
            }
            return iterator(name);
        }

        @Override
        public Iterator<ChildReference> iterator( Context context ) {
            if (context != null && context.changes() != null) {
                // we only want the context-sensitive behavior if there are changes. Otherwise we've already precomputed the SNS
                // index
                return super.iterator(context);
            }
            return iterator();
        }

        @Override
        public Iterator<NodeKey> getAllKeys() {
            return childReferencesByKey.keySet().iterator();
        }

        @Override
        public StringBuilder toString( StringBuilder sb ) {
            Iterator<ChildReference> iter = childReferencesByKey.values().iterator();
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

    @Immutable
    public static class Segmented extends AbstractChildReferences {

        protected final WorkspaceCache cache;
        protected final long totalSize;
        protected final boolean allowsSNS;
        private Segment firstSegment;

        public Segmented( WorkspaceCache cache,
                          ChildReferences firstSegment,
                          ChildReferencesInfo info,
                          boolean allowsSNS) {
            this.cache = cache;
            this.totalSize = info.totalSize;
            this.firstSegment = new Segment(firstSegment, info.nextKey, allowsSNS);
            this.allowsSNS = allowsSNS;
        }

        @Override
        public long size() {
            return totalSize;
        }

        @Override
        public boolean supportsGetChildReferenceByKey() {
            return size() != ChildReferences.UNKNOWN_SIZE;
        }

        @Override
        public int getChildCount( Name name ) {
            int result = 0;
            Segment segment = this.firstSegment;
            while (segment != null) {
                result += segment.getReferences().getChildCount(name);
                segment = segment.next(cache);
            }
            return result;
        }

        @Override
        public ChildReference getChild( Name name,
                                        int snsIndex,
                                        Context context ) {
            ChildReference result = null;
            Segment segment = this.firstSegment;
            while (segment != null) {
                result = segment.getReferences().getChild(name, snsIndex, context);
                if (result != null) {
                    return result;
                }
                segment = segment.next(cache);
            }
            return result;
        }

        @Override
        public boolean hasChild( NodeKey key ) {
            Segment segment = this.firstSegment;
            while (segment != null) {
                if (segment.getReferences().hasChild(key)) {
                    return true;
                }
                segment = segment.next(cache);
            }
            return false;
        }

        @Override
        public ChildReference getChild( NodeKey key ) {
            return getChild(key, defaultContext());
        }

        @Override
        public ChildReference getChild( NodeKey key,
                                        Context context ) {
            ChildReference result = null;
            Segment segment = this.firstSegment;
            while (segment != null) {
                result = segment.getReferences().getChild(key, context);
                if (result != null) {
                    return result;
                }
                segment = segment.next(cache);
            }
            return result;
        }

        @Override
        public Iterator<ChildReference> iterator( final Name name,
                                                  final Context context ) {
            final Segment firstSegment = this.firstSegment;
            return new Iterator<ChildReference>() {
                private Segment segment = firstSegment;
                private Iterator<ChildReference> iter = segment != null ? segment.getReferences().iterator(name, context) : ImmutableChildReferences.EMPTY_ITERATOR;
                private ChildReference next;

                @Override
                public boolean hasNext() {
                    if (next != null) {
                        // Called 'hasNext()' multiple times in a row ...
                        return true;
                    }
                    if (!iter.hasNext()) {
                        while (segment != null) {
                            segment = segment.next(cache);
                            if (segment != null) {
                                iter = segment.getReferences().iterator(name, context);
                                if (iter.hasNext()) {
                                    next = iter.next();
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                    next = iter.next();
                    return true;
                }

                @Override
                public ChildReference next() {
                    try {
                        if (next == null) {
                            if (hasNext()) return next;
                            throw new NoSuchElementException();
                        }
                        return next;
                    } finally {
                        next = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Iterator<ChildReference> iterator( final Context context ) {
            final Segment firstSegment = this.firstSegment;
            return new Iterator<ChildReference>() {
                private Segment segment = firstSegment;
                private Iterator<ChildReference> iter = segment != null ? segment.getReferences().iterator(context) : ImmutableChildReferences.EMPTY_ITERATOR;
                private ChildReference next;

                @Override
                public boolean hasNext() {
                    if (next != null) {
                        // Called 'hasNext()' multiple times in a row ...
                        return true;
                    }
                    if (!iter.hasNext()) {
                        while (segment != null) {
                            segment = segment.next(cache);
                            if (segment != null) {
                                iter = segment.getReferences().iterator(context);
                                if (iter.hasNext()) {
                                    next = iter.next();
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                    next = iter.next();
                    return true;
                }

                @Override
                public ChildReference next() {
                    try {
                        if (next == null) {
                            if (hasNext()) return next;
                            throw new NoSuchElementException();
                        }
                        return next;
                    } finally {
                        next = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Iterator<NodeKey> getAllKeys() {
            final Segment firstSegment = this.firstSegment;
            return new Iterator<NodeKey>() {
                private Segment segment = firstSegment;
                private Iterator<NodeKey> iter = segment != null ? segment.keys() : ImmutableChildReferences.EMPTY_KEY_ITERATOR;
                private NodeKey next;

                @Override
                public boolean hasNext() {
                    if (!iter.hasNext()) {
                        while (segment != null) {
                            segment = segment.next(cache);
                            if (segment != null) {
                                iter = segment.keys();
                                if (iter.hasNext()) {
                                    next = iter.next();
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                    next = iter.next();
                    return true;
                }

                @Override
                public NodeKey next() {
                    try {
                        if (next == null) {
                            if (hasNext()) return next;
                            throw new NoSuchElementException();
                        }
                        return next;
                    } finally {
                        next = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public StringBuilder toString( StringBuilder sb ) {
            Segment segment = firstSegment;
            while (segment != null) {
                segment.toString(sb);
                if (segment.next != null) {
                    // there is already a loaded segment ...
                    sb.append(", ");
                } else if (segment.nextKey != null) {
                    // there is a segment, but it hasn't yet been loaded ...
                    sb.append(", <segment=").append(segment.nextKey).append('>');
                }
                segment = segment.next;
            }
            return sb;
        }

        @Override
        public boolean allowsSNS() {
            return allowsSNS;
        }
    }

    public static class ReferencesUnion extends AbstractChildReferences {

        private final ChildReferences firstReferences;
        protected final ChildReferences secondReferences;

        ReferencesUnion( ChildReferences firstReferences,
                         ChildReferences secondReferences ) {
            this.firstReferences = firstReferences;
            this.secondReferences = secondReferences;
        }

        @Override
        public long size() {
            return secondReferences.size() + firstReferences.size();
        }

        @Override
        public int getChildCount( Name name ) {
            return secondReferences.getChildCount(name) + firstReferences.getChildCount(name);
        }

        @Override
        public ChildReference getChild( Name name,
                                        int snsIndex,
                                        Context context ) {
            ChildReference firstReferencesChild = firstReferences.getChild(name, snsIndex, context);
            if (firstReferencesChild != null) {
                return firstReferencesChild;
            }
            return secondReferences.getChild(name, snsIndex, context);
        }

        @Override
        public boolean hasChild( NodeKey key ) {
            return secondReferences.hasChild(key) || firstReferences.hasChild(key);
        }

        @Override
        public ChildReference getChild( NodeKey key ) {
            return getChild(key, defaultContext());
        }

        @Override
        public ChildReference getChild( NodeKey key,
                                        Context context ) {
            ChildReference firstReferencesChild = firstReferences.getChild(key, context);
            if (firstReferencesChild != null) {
                return firstReferencesChild;
            }
            return secondReferences.getChild(key, context);
        }

        @Override
        public Iterator<ChildReference> iterator() {
            return new UnionIterator<ChildReference>(firstReferences.iterator(), secondReferences);
        }

        @Override
        public Iterator<ChildReference> iterator( final Name name ) {
            Iterable<ChildReference> second = new Iterable<ChildReference>() {
                @Override
                public Iterator<ChildReference> iterator() {
                    return secondReferences.iterator(name);
                }
            };
            return new UnionIterator<ChildReference>(firstReferences.iterator(name), second);
        }

        @Override
        public Iterator<NodeKey> getAllKeys() {
            Set<NodeKey> externalKeys = new HashSet<NodeKey>();
            for (Iterator<NodeKey> externalKeysIterator = secondReferences.getAllKeys(); externalKeysIterator.hasNext();) {
                externalKeys.add(externalKeysIterator.next());
            }
            return new UnionIterator<NodeKey>(firstReferences.getAllKeys(), externalKeys);
        }

        @Override
        public StringBuilder toString( StringBuilder sb ) {
            sb.append("<second references=").append(secondReferences.toString());
            sb.append(">, <first references=").append(firstReferences.toString()).append(">");
            return sb;
        }

        @Override
        public boolean allowsSNS() {
            return firstReferences.allowsSNS() || secondReferences.allowsSNS();
        }
    }

    protected static class Segment {

        private final ChildReferences references;
        private final String nextKey;
        private final boolean allowsSNS;
        private Segment next;

        protected Segment( ChildReferences references,
                           String nextKey,
                           boolean allowsSNS) {
            this.nextKey = nextKey;
            this.references = references;
            this.allowsSNS = allowsSNS;
        }

        public ChildReferences getReferences() {
            return this.references;
        }

        public Segment next( WorkspaceCache cache ) {
            if (next == null && nextKey != null) {
                Document blockDoc = cache.blockFor(nextKey);
                if (blockDoc == null) {
                    throw new DocumentNotFoundException(nextKey);
                }
                // we only need the direct children of the block to avoid nesting
                ChildReferences refs = cache.translator().getChildReferencesFromBlock(blockDoc, allowsSNS);
                ChildReferencesInfo nextNextKey = cache.translator().getChildReferencesInfo(blockDoc);
                next = new Segment(refs, nextNextKey != null ? nextNextKey.nextKey : null, allowsSNS);
            }
            return next;
        }

        public Iterator<NodeKey> keys() {
            return references.getAllKeys();
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        public StringBuilder toString( StringBuilder sb ) {
            Iterator<ChildReference> iter = references.iterator();
            if (iter.hasNext()) {
                sb.append(iter.next());
                while (iter.hasNext()) {
                    sb.append(", ");
                    sb.append(iter.next());
                }
            }
            return sb;
        }
    }
}
