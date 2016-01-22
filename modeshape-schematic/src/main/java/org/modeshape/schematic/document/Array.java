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
package org.modeshape.schematic.document;

import java.util.List;

/**
 * Primary read-only interface for an in-memory representation of JSON/BSON arrays. Note that this interface extends
 * {@link Document}, where the field names are simply string representations of the array indices. This interface also extends the
 * standard Java {@link List} interface.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public interface Array extends Document, List<Object> {

    /**
     * Obtain a clone of this array.
     * 
     * @return the clone of this array; never null
     */
    @Override
    Array clone();

    /**
     * Get the entries in this array.
     * 
     * @return an iterable containing the array's entries; never null
     */
    Iterable<Entry> getEntries();

    /**
     * A representation of an entry within the array.
     */
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
