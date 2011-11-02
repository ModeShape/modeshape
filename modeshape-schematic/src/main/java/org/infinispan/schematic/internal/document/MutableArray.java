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
import java.util.List;
import org.infinispan.schematic.document.Array;

public interface MutableArray extends Array, MutableDocument {

    /**
     * Modifiable method that adds the supplied value if not already in the array. This method should <i>not</i> be called by
     * client code.
     * 
     * @param value the value to be added
     * @return true if the value was added, or false if the value was already in the array.
     */
    boolean addValueIfAbsent( Object value );

    /**
     * Modifiable method that adds the supplied value. This method should <i>not</i> be called by client code.
     * 
     * @param value the value to be added
     * @return the index at which the value was added, or -1 if the value could not be added
     */
    int addValue( Object value );

    /**
     * Modifiable method that adds the supplied value at the supplied index, shifting any existing values to the next higher index
     * value. This method should <i>not</i> be called by client code.
     * 
     * @param index the index
     * @param value the value to be added
     */
    void addValue( int index,
                   Object value );

    /**
     * Modifiable method that sets the supplied value at the given index. This method should <i>not</i> be called by client code.
     * 
     * @param index the index
     * @param value the value to be added
     * @return true if the value was added, or false if it could not be added
     */
    Object setValue( int index,
                     Object value );

    /**
     * Modifiable method that removes the supplied value. This method should <i>not</i> be called by client code.
     * 
     * @param value the value to be removed
     * @return true if the value was removed, or false if the value was not in the array
     */
    boolean removeValue( Object value );

    /**
     * Modifiable method that removes the value at the supplied index. This method should <i>not</i> be called by client code.
     * 
     * @param index the index of the value to be removed
     * @return the value that was at the index
     */
    Object removeValue( int index );

    /**
     * Modifiable method that adds the supplied values at the end of this array. This method should <i>not</i> be called by client
     * code.
     * 
     * @param values the values to be added
     * @return true if this array changed as a result of the operation
     */
    boolean addAllValues( Collection<?> values );

    /**
     * Modifiable method that adds the supplied values at the supplied index, shifting any existing values to the next higher
     * index value. This method should <i>not</i> be called by client code.
     * 
     * @param index the index at which the values are to be inserted
     * @param values the values to be added
     * @return true if this array changed as a result of the operation
     */
    boolean addAllValues( int index,
                          Collection<?> values );

    /**
     * Modifiable method that removes all of the values from this array. This method should <i>not</i> be called by client code.
     */
    @Override
    void removeAll();

    /**
     * Modifiable method that removes all of the supplied values from this array. This method should <i>not</i> be called by
     * client code.
     * 
     * @param values the values to be removed
     * @return the entries that were removed; never null but possibly empty if this array was not modified by this operation
     */
    List<Entry> removeAllValues( Collection<?> values );

    /**
     * Modifiable method that removes all of the values in this array except the supplied values. This method should <i>not</i> be
     * called by client code.
     * 
     * @param values the values to be kept, while all others are removed
     * @return the entries that were removed; never null but possibly empty if this array was not modified by this operation
     */
    List<Entry> retainAllValues( Collection<?> values );

    static interface Entry extends Comparable<Entry> {
        /**
         * Get the index for this entry.
         * 
         * @return the index
         */
        int getIndex();

        /**
         * Get the value for this entry.
         * 
         * @return the value
         */
        Object getValue();
    }

}
