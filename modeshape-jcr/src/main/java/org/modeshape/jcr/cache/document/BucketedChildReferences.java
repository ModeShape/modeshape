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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A child references implementation which uses separate buckets for groups of children, based on the SHA1 of the name of each 
 * child and the an internal field stored in the parent document which indicates the length (i.e. how many of the
 * first SHA1 chars) of the ID of each bucket.
 * See <a href='https://issues.jboss.org/browse/MODE-2109'>MODE-2109</a>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
@ThreadSafe
final class BucketedChildReferences extends AbstractChildReferences {
    
    private final long size;
    private final int bucketIdLength;
    private final String parentKey;
    private final DocumentTranslator translator;
    private final Map<BucketId, Bucket> rangeBucketsById;
    private final Set<BucketId> bucketIds; 
    
    protected BucketedChildReferences( Document parent, DocumentTranslator translator ) {
        // the power of 16 which indicates how many buckets
        this.bucketIdLength = parent.getInteger(DocumentConstants.BUCKET_ID_LENGTH);
        
        Long size = parent.getLong(DocumentConstants.SIZE);
        this.size = size != null ? size : 0;
        
        this.parentKey = translator.getKey(parent);
        this.translator = translator;
        
        List<?> bucketsArray = parent.getArray(DocumentConstants.BUCKETS);
        if (bucketsArray == null) {
            this.bucketIds = Collections.emptySet();
        } else {
            this.bucketIds = new HashSet<>(bucketsArray.size());
            for (Object bucketId : bucketsArray) {
                this.bucketIds.add(new BucketId(bucketId.toString()));
            }
        }
        this.rangeBucketsById = new LinkedHashMap<>(bucketIds.size());
    }

    @Override
    public long size() {
        return size;
    }
    
    @Override
    public int getChildCount( Name name ) {
        Bucket bucket = bucketFor(name);
        return bucket != null ? bucket.childCount(name) : 0;
    }

    @Override
    public ChildReference getChild( Name name, int snsIndex, Context context ) {
        if (snsIndex != 1) {
            // we don't support SNS
            return null;
        }
        Bucket bucket = bucketFor(name);
        if (context == null || context.changes() == null) {
            // there are no additional changes
            return bucket != null ? bucket.childReferenceFor(name) : null;
        }
        // there are changes
        Changes changes = context.changes();
        ChildReference ref = bucket != null ? bucket.childReferenceFor(name) : null;

        if (ref == null) {
            // we don't have a reference in our data, look at the changes
            Iterator<ChildInsertions> iterator = changes.insertions(name);
            if (iterator != null && iterator.hasNext()) {
                ChildInsertions inserted = iterator.next();
                Iterator<ChildReference> iter = inserted.inserted().iterator();
                // we don't support SNS, renames or reorderings so we'll always return the fist insertion
                return iter.hasNext() ? iter.next() : null;
            }
            return null;
            
        }
        // we do have a reference in our data, but we need to check it it hasn't been removed
        return changes.isRemoved(ref) ? null : ref;
    }

    @Override
    public boolean hasChild( NodeKey key ) {
        return getChild(key) != null;
    }

    @Override
    public ChildReference getChild( NodeKey key ) {
        return getChild(key, NoContext.INSTANCE);
    }

    @Override
    public ChildReference getChild( NodeKey key, Context context ) {
       
        ChildReference ref = null;
        for (BucketId bucketId : bucketIds) {
            Bucket bucket = loadBucket(bucketId);
            ref = bucket != null ? bucket.childReferenceFor(key) : null;
            if (ref != null) {
                break;
            }
        }
        
        Changes changes = context != null ? context.changes() : null;
        if (ref == null) {
            // we don't have the node key locally, but we still need to check the changes
            return changes != null ? changes.inserted(key) : null;
        }
        // we do have the child ref in one of our buckets
        return changes != null && changes.isRemoved(ref) ? null : ref;
    }

    @Override
    public boolean allowsSNS() {
        return false;
    }

    @Override
    public Iterator<NodeKey> getAllKeys() {
        final Iterator<BucketId> bucketIdsIterator = bucketIds.iterator();  
        return bucketsIterator(null, bucketIdsIterator, new ChildReferenceExtractor<NodeKey>() {
            @Override
            public NodeKey extractFrom( ChildReference ref ) {
                return ref.getKey();
            }
        });
    }

    @Override
    public Iterator<ChildReference> iterator( Name name, Context context ) {
        // since SNS are not support, simply check if there is a node with this name or not
        ChildReference ref = getChild(name, 1, context);
        return ref != null ? Collections.singletonList(ref).iterator() : Collections.<ChildReference>emptyListIterator();
    }

    @Override
    public Iterator<ChildReference> iterator( final Context context ) {
        return bucketsIterator(context, bucketIds.iterator(), new ChildReferenceExtractor<ChildReference>() {
            @Override
            public ChildReference extractFrom( ChildReference ref ) {
                return ref;
            }
        });
    }

    private <T> Iterator<T> bucketsIterator( final Context context,
                                             final Iterator<BucketId> bucketIdsIterator,
                                             final ChildReferenceExtractor<T> extractor ) {
        return new Iterator<T>() {
            private Iterator<ChildReference> activeIterator = null;
            
            @Override
            public boolean hasNext() {
                while ((activeIterator == null || !activeIterator.hasNext()) && bucketIdsIterator.hasNext()) {
                    BucketId bucketId = bucketIdsIterator.next();
                    Bucket bucket = loadBucket(bucketId);
                    if (bucket != null) {
                        activeIterator = bucket.iterator(context);
                    }
                }
                return activeIterator != null && activeIterator.hasNext();
            }

            @Override
            public T next() {
                T value = null;
                while (hasNext() && value == null) {
                    value = extractor.extractFrom(activeIterator.next());                    
                }
                if (value != null) {
                    return value;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
   
    @Override
    public Iterator<ChildReference> iterator( final Context context, 
                                              final Collection<?> namePatterns,
                                              final NamespaceRegistry registry ) {
        Set<BucketId> bucketsToSearch = new LinkedHashSet<>(namePatterns.size());
        // check to see if any wildcards are used in the search
        for (Object pattern : namePatterns) {
            if (pattern instanceof Pattern) {
                // at least one wildcard is being used somewhere, so there isn't any optimization that can be performed
                return super.iterator(context, namePatterns, registry);                 
            } else {
                // this is a simple string which we'll collect
                String name = pattern.toString();
                bucketsToSearch.add(new BucketId(name, bucketIdLength));
            }
        }
        // all the patterns are simple strings, without wildcards, so we can take advantage of that and find out which buckets
        // to look at
        return bucketsIterator(context, bucketsToSearch.iterator(), new ChildReferenceExtractor<ChildReference>() {
            @Override
            public ChildReference extractFrom( ChildReference ref ) {
                if (namePatterns.contains(ref.getName().getString(registry))) {
                    return ref;
                } else {
                    return null;
                }
            }
        });
    }
    
    interface ChildReferenceExtractor<T> {
        T extractFrom(ChildReference ref);
    }

    @Override
    public StringBuilder toString( StringBuilder sb ) {
        sb.append("BucketedChildReferences[");
        for (Bucket bucket : rangeBucketsById.values()) {
            sb.append(bucket).append(System.getProperty("line.separator"));
        }
        sb.append("]");
        return sb;
    }
    
    protected Bucket bucketFor(Name name) {
        return loadBucket(new BucketId(name, bucketIdLength));
    }

    protected Bucket loadBucket(BucketId bucketId) {
        Bucket bucket = rangeBucketsById.get(bucketId);
        if (bucket != null) {
            return bucket;
        }
        synchronized(this) {
            bucket = rangeBucketsById.get(bucketId);
            if (bucket != null) {
                return bucket;
            }
            bucket = translator.loadBucket(parentKey, bucketId);
            if (bucket != null) {
                rangeBucketsById.put(bucketId, bucket);
            }
            return bucket;
        }
    }

    protected static class Bucket implements Iterable<NodeKey> {
        private final BucketId id;
        private final Map<NodeKey, String> childNamesByKey;
        private final Map<String, NodeKey> childKeysByName;
        private final NameFactory nameFactory;
        private final ValueFactory<String> stringValueFactory;
       
        protected Bucket( BucketId id, Document bucketDoc, DocumentTranslator translator ) {
            assert id != null;
            assert bucketDoc != null;
            this.id = id;
            this.nameFactory = translator.getNameFactory();
            this.stringValueFactory = translator.getStringFactory();
            if (bucketDoc.isEmpty()) {
                this.childNamesByKey = Collections.emptyMap();
                this.childKeysByName = Collections.emptyMap();
            } else {
                int size = bucketDoc.size();
                this.childNamesByKey = new LinkedHashMap<>(size);
                this.childKeysByName = new HashMap<>(size);
                for (Map.Entry<String, ?> entry : bucketDoc.toMap().entrySet()) {
                    NodeKey nodeKey = new NodeKey(entry.getKey());
                    String name = entry.getValue().toString();
                    childNamesByKey.put(nodeKey, name);
                    childKeysByName.put(name, nodeKey);
                }
            }
        }
        
        protected int childCount(Name name) {
            return childKeysByName.containsKey(stringValueFactory.create(name)) ? 1 : 0;
        }

        protected ChildReference childReferenceFor(Name name) {
            NodeKey key = childKeysByName.get(stringValueFactory.create(name));
            return key == null ? null : new ChildReference(key, name, 1);
        }

        protected ChildReference childReferenceFor(NodeKey key) {
            String name = childNamesByKey.get(key);
            return name == null ? null : new ChildReference(key, nameFactory.create(name), 1);
        }

        @Override
        public Iterator<NodeKey> iterator() {
            return childNamesByKey.keySet().iterator();
        }
        
        protected Iterator<ChildReference> iterator(final Context context) {
            // the only transient part of the context these types of references care about is the removals since
            // reordering and renames are not supported
            final Changes changes = context != null ? context.changes() : null;
            final int removalCount = changes != null ? changes.removalCount() : 0; 
            final Iterator<NodeKey> internalIterator = childNamesByKey.keySet().iterator();
            return new Iterator<ChildReference>() {
                private ChildReference next = null;
                
                @Override
                public boolean hasNext() {
                    while (next == null && internalIterator.hasNext()) {
                        ChildReference internalRef = childReferenceFor(internalIterator.next());
                        if (removalCount > 0 && changes.isRemoved(internalRef)) {
                            // this is transient removed child, so ignore it
                            continue;
                        } else {
                            next = internalRef;
                            break;
                        }
                    }
                    return next != null;
                }

                @Override
                public ChildReference next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    ChildReference result = next;
                    next = null;
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };     
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("bucket[");
            sb.append("id='").append(id).append('\'');
            sb.append(", children=").append(childNamesByKey.values());
            sb.append(']');
            return sb.toString();
        }
    }
}
