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
package org.modeshape.jcr.cache.document;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path.Segment;

/**
 * An partial {@link ChildReferences} implementation that can serve as a base class to provide default implementations of some
 * methods to simplify other implementations.
 */
public abstract class AbstractChildReferences implements ChildReferences {

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public ChildReference getChild( Name name ) {
        return getChild(name, 1, new SingleNameContext());
    }

    @Override
    public ChildReference getChild( Segment segment ) {
        return getChild(segment.getName(), segment.getIndex(), new SingleNameContext());
    }

    @Override
    public ChildReference getChild( Name name,
                                    int snsIndex ) {
        return getChild(name, snsIndex, new SingleNameContext());
    }

    @Override
    public Iterator<ChildReference> iterator( Name name ) {
        return iterator(name, new BasicContext());
    }

    @Override
    public Iterator<ChildReference> iterator( Name name,
                                              Context context ) {
        return contextSensitiveIterator(iterator(name), context);
    }

    /**
     * Get an iterator over all child references in this collection, but base the SNS indexes upon those already consumed.
     * 
     * @param context the context in which the child should be evaluated; may be null if there is no context
     * @return the iterator over all references; never null
     */
    @Override
    public Iterator<ChildReference> iterator( final Context context ) {
        return contextSensitiveIterator(iterator(), context);
    }

    protected Iterator<ChildReference> contextSensitiveIterator( final Iterator<ChildReference> original,
                                                                 final Context context ) {
        if (context == null) return original;
        final Changes changes = context.changes();
        if (changes == null || changes.isEmpty()) {
            // There are no changes, so the iterator can be a bit more specialized and direct ...
            return new Iterator<ChildReference>() {

                @Override
                public boolean hasNext() {
                    return original.hasNext();
                }

                @Override
                public ChildReference next() {
                    ChildReference ref = original.next();
                    return ref.with(context.consume(ref.getName(), ref.getKey()));
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        // There ARE changes, so the iterator needs to take these into account ...
        return new Iterator<ChildReference>() {
            private Iterator<ChildReference> delegate = original;
            private Iterator<ChildReference> iter = delegate;
            private ChildReference next;
            private ChildReference nextAfterIter;

            @Override
            public boolean hasNext() {
                while (true) {
                    while (iter.hasNext()) {
                        // Verify the next reference is valid ...
                        ChildReference next = iter.next();

                        // See if there are any nodes inserted before this node ...
                        ChildInsertions insertions = changes.insertionsBefore(next);
                        if (insertions != null) {
                            nextAfterIter = next;
                            iter = insertions.inserted().iterator();
                            continue;
                        }

                        // See if this child has been removed ...
                        if (changes.isRemoved(next)) continue;
                        this.next = next;
                    }
                    if (iter != delegate) {
                        // This was an insertion iterator, so switch back to the delegate iterator ...
                        try {
                            iter = delegate;
                            // But the next ref will actually be the 'next' we found before the inserted ...
                            next = nextAfterIter;
                            return true;
                        } finally {
                            nextAfterIter = null;
                        }
                    }
                    return false;
                }
            }

            @Override
            public ChildReference next() {
                try {
                    if (next == null) {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                    }
                    return next.with(context.consume(next.getName(), next.getKey()));
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
    public Iterator<ChildReference> iterator( Collection<?> namePatterns,
                                              final NamespaceRegistry registry ) {
        return new PatternIterator<ChildReference>(iterator(), namePatterns) {
            @Override
            protected String matchable( ChildReference value ) {
                return value.getString(registry);
            }
        };
    }

    @Override
    public Iterator<ChildReference> iterator( Context context,
                                              Collection<?> namePatterns,
                                              final NamespaceRegistry registry ) {
        return new PatternIterator<ChildReference>(iterator(context), namePatterns) {
            @Override
            protected String matchable( ChildReference value ) {
                return value.getString(registry);
            }
        };
    }

    public Iterator<ChildReference> iterator( Context context,
                                              final Name name ) {
        return new ChildReferenceWithNameIterator(iterator(context), name);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public abstract StringBuilder toString( StringBuilder sb );

    protected static class ChildReferenceWithNameIterator extends DelegatingIterator<ChildReference> {

        private final Name name;
        private ChildReference last;

        protected ChildReferenceWithNameIterator( Iterator<ChildReference> delegate,
                                                  Name name ) {
            super(delegate);
            this.name = name;
        }

        @Override
        public boolean hasNext() {
            while (super.hasNext()) {
                last = super.next();
                if (last.getName().equals(name)) return true;
            }
            return false;
        }

        @Override
        public ChildReference next() {
            if (last == null) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            if (last != null) {
                try {
                    return last;
                } finally {
                    last = null;
                }
            }
            throw new NoSuchElementException();
        }
    }
}
