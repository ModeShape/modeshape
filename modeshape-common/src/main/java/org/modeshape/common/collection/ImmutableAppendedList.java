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
package org.modeshape.common.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * An immutable {@link List} that consists of a single element appended to another existing {@link List}. The result is a list
 * that contains all of the elements in the parent list as well as the last appended element, but while reusing the existing
 * parent list and without having to create a copy of the parent list.
 * 
 * @param <T> the type of element
 */
@Immutable
public class ImmutableAppendedList<T> implements List<T> {

    private final List<T> parent;
    private final T element;
    private final int size;
    private transient int hc;

    /**
     * Create an instance using the supplied parent list and an element to be virtually appended to the parent. Note that the
     * parent must be immutable (though this is not checked).
     * 
     * @param parent the parent list
     * @param element the child element (may be null)
     * @throws IllegalArgumentException if the reference to the parent list is null
     */
    public ImmutableAppendedList( List<T> parent,
                                  T element ) {
        CheckArg.isNotNull(parent, "parent");
        this.parent = parent;
        this.element = element;
        this.size = parent.size() + 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#contains(java.lang.Object)
     */
    @Override
    public boolean contains( Object o ) {
        return element == o || (element != null && element.equals(o)) || parent.contains(o);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll( Collection<?> c ) {
        Iterator<?> e = c.iterator();
        while (e.hasNext()) {
            if (!contains(e.next())) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#get(int)
     */
    @Override
    public T get( int index ) {
        if (index == (size - 1)) return element;
        return parent.get(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf( Object o ) {
        int index = parent.indexOf(o);
        if (index == -1) {
            return (element == o || (element != null && element.equals(o))) ? (size - 1) : -1;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#iterator()
     */
    @Override
    @SuppressWarnings( "synthetic-access" )
    public Iterator<T> iterator() {
        final Iterator<T> parentIterator = parent.iterator();
        return new Iterator<T>() {
            boolean finished = false;

            @Override
            public boolean hasNext() {
                return parentIterator.hasNext() || !finished;
            }

            @Override
            public T next() {
                if (parentIterator.hasNext()) return parentIterator.next();
                if (finished) throw new NoSuchElementException();
                finished = true;
                return element;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf( Object o ) {
        if (element == o || (element != null && element.equals(o))) return size - 1;
        return parent.lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#listIterator(int)
     */
    @Override
    @SuppressWarnings( "synthetic-access" )
    public ListIterator<T> listIterator( final int index ) {
        return new ListIterator<T>() {
            int cursor = index;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                try {
                    T next = get(cursor);
                    cursor++;
                    return next;
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public boolean hasPrevious() {
                return cursor != 0;
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public T previous() {
                try {
                    int i = cursor - 1;
                    T previous = get(i);
                    cursor = i;
                    return previous;
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void set( T o ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add( T o ) {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#size()
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List<T> subList( int fromIndex,
                            int toIndex ) {
        if (fromIndex == 0 && toIndex == size) {
            // The bounds are the same as this list, so just return this list ...
            return this;
        }
        if (toIndex == size && fromIndex == (size - 1)) {
            // The only list is the last element ...
            return Collections.singletonList(element);
        }
        if (toIndex < size) {
            // It is all within the range of the parent's list, so simply delegate
            return parent.subList(fromIndex, toIndex);
        }
        // Otherwise, the sublist starts within the parent list and ends with the last element.
        // So, create a sublist starting at the 'fromIndex' until the end of the parent list ...
        List<T> sublist = parent.subList(fromIndex, toIndex - 1); // will catch out-of-bounds errors
        // And wrap with another immutable appended list to add the last element ...
        return new ImmutableAppendedList<T>(sublist, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#toArray()
     */
    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (T e : parent) {
            result[i++] = e;
        }
        result[i] = element;
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#toArray(T[])
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public <X> X[] toArray( X[] a ) {
        if (a.length < size) a = (X[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        a = parent.toArray(a);
        a[size - 1] = (X)element;
        return a;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (hc == 0) {
            int hashCode = 1;
            for (T element : this) {
                hashCode = 31 * hashCode + (element == null ? 0 : element.hashCode());
            }
            hc = hashCode;
        }
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof List<?>) {
            List<?> that = (List<?>)obj;
            if (this.size() != that.size()) return false;
            Iterator<?> thisIter = this.iterator();
            Iterator<?> thatIter = that.iterator();
            while (thisIter.hasNext()) {
                Object thisValue = thisIter.next();
                Object thatValue = thatIter.next();
                if (thisValue == null) {
                    if (thatValue != null) return false;
                    // assert thatValue == null;
                } else {
                    if (!thisValue.equals(thatValue)) return false;
                }
            }
            return true;
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");

        Iterator<T> i = iterator();
        boolean hasNext = i.hasNext();
        while (hasNext) {
            T o = i.next();
            buf.append(o == this ? "(this Collection)" : String.valueOf(o));
            hasNext = i.hasNext();
            if (hasNext) buf.append(", ");
        }

        buf.append("]");
        return buf.toString();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Methods that modify are not supported
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#add(int, Object)
     */
    @Override
    public void add( int index,
                     T element ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#add(Object)
     */
    @Override
    public boolean add( T o ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll( Collection<? extends T> c ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll( int index,
                           Collection<? extends T> c ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#clear()
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#remove(java.lang.Object)
     */
    @Override
    public boolean remove( Object o ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#remove(int)
     */
    @Override
    public T remove( int index ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll( Collection<?> c ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.List#set(int, java.lang.Object)
     */
    @Override
    public T set( int index,
                  T element ) {
        throw new UnsupportedOperationException();
    }

}
