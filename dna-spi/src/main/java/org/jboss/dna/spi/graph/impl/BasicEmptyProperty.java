/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.spi.graph.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.spi.graph.Name;

/**
 * @author Randall Hauch
 */
@Immutable
public class BasicEmptyProperty extends BasicProperty {

    /**
     * @param name
     */
    public BasicEmptyProperty( Name name ) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingle() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Object> iterator() {
        return new EmptyIterator<Object>();
    }

    protected class EmptyIterator<T> implements Iterator<T> {

        protected EmptyIterator() {
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
