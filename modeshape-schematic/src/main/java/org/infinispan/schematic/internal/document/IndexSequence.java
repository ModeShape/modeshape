/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.document;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * An {@link IndexSequence} is a lightweight ordered sequence of string representing array indexes. It is lightweight because it
 * shares and reuses the strings for indexes less than the {@link #MAXIMUM_KEY_COUNT maximum key count}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class IndexSequence implements Set<String> {
    protected static final String[] EMPTY_ARRAY = new String[0];
    protected static final String[] INDEX_VALUES;
    protected static final int MAXIMUM_KEY_COUNT = 250;
    static {
        String[] values = new String[MAXIMUM_KEY_COUNT];
        for (int i = 0; i != MAXIMUM_KEY_COUNT; ++i) {
            values[i] = Integer.toString(i);
        }
        INDEX_VALUES = values;
    }

    /**
     * Obtain an iterator for the indexes up to the specified size.
     * 
     * @param size the number of indexes
     * @return the iterator of string indexes; never null
     */
    public static Iterator<String> indexesTo( int size ) {
        return new IndexSequence(size).iterator();
    }

    /**
     * Obtain an iterator for the indexes up the {@link Integer#MAX_VALUE largest} integer. This is efficient, because it only
     * builds strings as needed.
     * 
     * @return the iterator of string indexes; never null
     */
    public static Iterator<String> infiniteSequence() {
        return new IndexSequence(Integer.MAX_VALUE).iterator();
    }

    protected final int size;

    public IndexSequence( int size ) {
        this.size = size;
    }

    @Override
    public boolean add( String e ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll( Collection<? extends String> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains( Object o ) {
        int index = Integer.parseInt(String.valueOf(o));
        return index >= 0 && index < size;
    }

    @Override
    public boolean containsAll( Collection<?> c ) {
        for (Object value : c) {
            if (!contains(value)) return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public String next() {
                return index < MAXIMUM_KEY_COUNT ? INDEX_VALUES[index++] : String.valueOf(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean remove( Object o ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] toArray() {
        if (size == 0) return EMPTY_ARRAY;
        String[] copy = new String[size];
        System.arraycopy(INDEX_VALUES, 0, copy, 0, size);
        return copy;
    }

    @Override
    public <T> T[] toArray( T[] a ) {
        throw new UnsupportedOperationException();
    }
}
