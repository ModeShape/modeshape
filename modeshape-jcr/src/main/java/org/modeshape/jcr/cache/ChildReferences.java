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
package org.modeshape.jcr.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.EmptyIterator;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path.Segment;

/**
 * An interface used to access the {@link ChildReference} instances owned by a parent node.
 */
@Immutable
public interface ChildReferences extends Iterable<ChildReference> {

    /**
     * Get the total number of child references for the node, including all subsequent blocks of ChildReferences.
     * 
     * @return the total number of children; never negative
     */
    long size();

    /**
     * Determine if there are no references in this container. This is equivalent to calling {@code size() == 0} but may be
     * faster.
     * 
     * @return true if there are no references in this container, or false if there are.
     */
    boolean isEmpty();

    /**
     * Return the number of nodes that have the supplied name. If there are no siblings with the same supplied name, this method
     * will return 1; otherwise it will return the number of same-name-siblings.
     * 
     * @param name the name
     * @return the number of siblings with the supplied name; never negative
     */
    int getChildCount( Name name );

    /**
     * Look for the child reference that has the given name and a SNS index of '1'.
     * 
     * @param name the name for the node
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( Name name );

    /**
     * Look for the child reference that has the given name and SNS index.
     * 
     * @param name the name for the node
     * @param snsIndex the same-name-sibling index; must be positive
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( Name name,
                             int snsIndex );

    /**
     * Look for the child reference that has the given name and SNS index.
     * 
     * @param name the name for the node
     * @param snsIndex the same-name-sibling index; must be positive
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( Name name,
                             int snsIndex,
                             Context context );

    /**
     * Look for the child reference that has the given name and SNS index.
     * 
     * @param segment the path segment, which defines the name and SNS index
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( Segment segment );

    /**
     * Determine if this contains a reference to the specified child.
     * 
     * @param key the node key of the child
     * @return true if there is a child reference, or false if there is none
     */
    boolean hasChild( NodeKey key );

    /**
     * Look for the child reference that has the node key.
     * 
     * @param key the node key of the child
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( NodeKey key );

    /**
     * Look for the child reference that has the node key.
     * 
     * @param key the node key of the child
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @return the child reference, or null if there is no such child
     */
    ChildReference getChild( NodeKey key,
                             Context context );

    /**
     * Get an iterator over all of the children that have same name matching the supplied value. This essentially returns an
     * iterator over all of the same-name-siblings.
     * 
     * @param name the name of the same-name-sibling nodes; may not be null
     * @return the iterator; never null
     */
    Iterator<ChildReference> iterator( Name name );

    /**
     * Get an iterator over all of the children that have same name matching the supplied value. This essentially returns an
     * iterator over all of the same-name-siblings.
     * 
     * @param name the name of the same-name-sibling nodes; may not be null
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @return the iterator; never null
     */
    Iterator<ChildReference> iterator( Name name,
                                       Context context );

    /**
     * Get an iterator over all of the children.
     * 
     * @return the iterator; never null
     */
    @Override
    Iterator<ChildReference> iterator();

    /**
     * Get an iterator over all of the children that have names matching at least one of the supplied patterns.
     * 
     * @param namePatterns the list of string literals or regex patterns describing the names
     * @param registry the namespace registry, used to convert names to a form compatible with the name patterns
     * @return the iterator; never null
     */
    Iterator<ChildReference> iterator( Collection<?> namePatterns,
                                       NamespaceRegistry registry );

    /**
     * Get an iterator over all child references in this collection, using the supplied context.
     * 
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @return the iterator over all references; never null
     */
    Iterator<ChildReference> iterator( Context context );

    /**
     * Get an iterator over all of the children that have names matching at least one of the supplied patterns, using the supplied
     * context. The resulting iterator is lazy where possible, but it may be an expensive call if there are large numbers of
     * children.
     * 
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @param namePatterns the list of string literals or regex patterns describing the names
     * @param registry the namespace registry, used to convert names to a form compatible with the name patterns
     * @return the iterator; never null
     */
    Iterator<ChildReference> iterator( Context context,
                                       Collection<?> namePatterns,
                                       NamespaceRegistry registry );

    /**
     * Get the keys for all of the children. The resulting iterator is lazy where possible, but it may be an expensive call if
     * there are large numbers of children.
     * 
     * @return the iterator over the keys; never null
     */
    Iterator<NodeKey> getAllKeys();

    /**
     * The context in which the names are evaluated.
     */
    interface Context {

        /**
         * Consume the next child with the supplied name and key.
         * 
         * @param name the name of the node; may not be null
         * @param key the key for the node; may not be null
         * @return the same-name-sibling index for this node; always positive
         */
        int consume( Name name,
                     NodeKey key );

        /**
         * Get the set of changes for this context.
         * 
         * @return the changes; never null
         */
        Changes changes();
    }

    /**
     * The representation of a set of changes for the child references.
     */
    interface Changes {

        /**
         * Get the references to the children with the supplied name that were inserted.
         * 
         * @param name the name; may not be null
         * @return the iterator over the insertions; never null but possibly empty
         */
        Iterator<ChildInsertions> insertions( Name name );

        /**
         * Get the child reference for the inserted node with the supplied key.
         * 
         * @param key the node key for the inserted node; may not be null
         * @return the child reference, or null if no node was inserted with the supplied key
         */
        ChildReference inserted( NodeKey key );

        /**
         * Get the set of child references that were inserted before the node with the supplied key.
         * 
         * @param key the node key for the node before which the inserted nodes are to be returned; may not be null
         * @return the nodes that were inserted before the node with the supplied key
         */
        ChildInsertions insertionsBefore( ChildReference key );

        /**
         * Determine whether the supplied child reference was removed.
         * 
         * @param ref the reference; may not be null
         * @return true if the child reference was removed, or false otherwise
         */
        boolean isRemoved( ChildReference ref );

        /**
         * Determine whether the supplied child reference was renamed.
         * 
         * @param ref the reference; may not be null
         * @return true if the child reference was renamed, or false otherwise
         */
        boolean isRenamed( ChildReference ref );

        /**
         * Determine whether any of the child references were renamed to the supplied name.
         * 
         * @param newName the new name; may not be null
         * @return true if at least one child reference was renamed to the supplied name, or false otherwise
         */
        boolean isRenamed( Name newName );

        /**
         * Return the new name for the child node with the supplied key.
         * 
         * @param key the child node's key; may not be null
         * @return the new name, or null if the node is not a child or was not renamed
         */
        Name renamed( NodeKey key );

        /**
         * Determine if this set of changes is empty.
         * 
         * @return true if there are no effective changes, or false if there is at least one effective change
         */
        boolean isEmpty();

        /**
         * Get the number of child references that were removed.
         * 
         * @return the number of removed child references; never negative
         */
        int removalCount();

        /**
         * Get the number of child references that were inserted.
         * 
         * @return the number of inserted child references; never negative
         */
        int insertionCount();

        /**
         * Get the number of child references that were renamed.
         * 
         * @return the number of renamed child references; never negative
         */
        int renameCount();
    }

    /**
     * A representation of the child references that were inserted before some other node.
     */
    interface ChildInsertions {

        /**
         * The nodes that were inserted.
         * 
         * @return the iterator over the child references that were inserted; never null
         */
        Iterable<ChildReference> inserted();

        /**
         * The reference to the child before which the nodes are to be inserted.
         * 
         * @return the child reference before which the nodes are to be inserted; never null
         */
        ChildReference insertedBefore();
    }

    /**
     * A {@link ChildReferences.Changes} implementation that has no changes and that is useful when there are never any siblings
     * with the same names, since it always returns '1' for the SNS index.
     */
    public static final class NoContext implements Context {
        protected static final Context INSTANCE = new NoContext();

        private NoContext() {
        }

        @Override
        public int consume( Name name,
                            NodeKey key ) {
            return 1;
        }

        @Override
        public Changes changes() {
            return null;
        }
    }

    @Immutable
    public static final class NoChanges implements Changes {
        protected static final Iterator<ChildInsertions> NO_INSERTIONS_ITERATOR = new EmptyIterator<ChildInsertions>();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int insertionCount() {
            return 0;
        }

        @Override
        public int removalCount() {
            return 0;
        }

        @Override
        public int renameCount() {
            return 0;
        }

        @Override
        public Name renamed( NodeKey key ) {
            return null;
        }

        @Override
        public Iterator<ChildInsertions> insertions( Name name ) {
            return NO_INSERTIONS_ITERATOR;
        }

        @Override
        public ChildReference inserted( NodeKey key ) {
            return null;
        }

        @Override
        public ChildInsertions insertionsBefore( ChildReference key ) {
            return null;
        }

        @Override
        public boolean isRemoved( ChildReference key ) {
            return false;
        }

        @Override
        public boolean isRenamed( ChildReference ref ) {
            return false;
        }

        @Override
        public boolean isRenamed( Name newName ) {
            return false;
        }
    }

    /**
     * A {@link ChildReferences.Context} implementation that has no changes and that always returns '1' for the SNS index.
     */
    @ThreadSafe
    public static final class NoSnsIndexesContext implements Context {

        @Override
        public int consume( Name name,
                            NodeKey key ) {
            return 1;
        }

        @Override
        public Changes changes() {
            return null;
        }
    }

    /**
     * A {@link ChildReferences.Context} implementation that has no changes and can be used to find the SNS indexes for nodes
     * named a single name.
     */
    @ThreadSafe
    public static class SingleNameContext implements Context {
        private int index = 0;

        @Override
        public int consume( Name name,
                            NodeKey key ) {
            return ++index;
        }

        @Override
        public Changes changes() {
            return null;
        }
    }

    /**
     * A {@link ChildReferences.Context} implementation that has no changes but maintains the SNS indexes for nodes with any name.
     */
    @NotThreadSafe
    public static class BasicContext implements Context {
        private final Map<Name, AtomicInteger> indexes = new HashMap<Name, AtomicInteger>();

        @Override
        public int consume( Name name,
                            NodeKey key ) {
            AtomicInteger index = indexes.get(name);
            if (index == null) {
                index = new AtomicInteger(1);
                indexes.put(name, index);
                return 1;
            }
            return index.incrementAndGet();
        }

        @Override
        public Changes changes() {
            return null;
        }
    }

    /**
     * A {@link ChildReferences.Context} implementation that has changes and can be used to find the SNS indexes for nodes named a
     * single name.
     */
    @ThreadSafe
    public static class WithChanges implements Context {
        private final Context delegate;
        private final Changes changes;

        public WithChanges( Context delegate,
                            Changes changes ) {
            this.delegate = delegate;
            this.changes = changes;
        }

        @Override
        public int consume( Name name,
                            NodeKey key ) {
            return this.delegate.consume(name, key);
        }

        @Override
        public Changes changes() {
            return changes;
        }
    }
}
