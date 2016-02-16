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
package org.modeshape.schematic.internal.document;

import java.util.Collection;
import java.util.List;
import org.modeshape.schematic.document.Array;

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

    @Override
    MutableArray clone();
}
