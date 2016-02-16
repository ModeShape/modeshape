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


public interface Editor extends EditableDocument {

    /**
     * Get the changes that have been made to this document.
     * 
     * @return the changes; never null
     */
    Changes getChanges();

    /**
     * After making changes to another document, apply the same changes to this document. This allows a set of changes to be made,
     * serialized, and applied to a different document (that often represents a different instance of the same document).
     * 
     * @param changes the changes that are to be applied to this document; may not be null
     * @see #apply(Changes, Observer)
     */
    void apply( Changes changes );

    /**
     * After making changes to another document, apply the same changes to this document. This allows a set of changes to be made,
     * serialized, and applied to a different document (that often represents a different instance of the same document).
     * 
     * @param changes the changes that are to be applied to this document; may not be null
     * @param observer an observer that will be called as changes are undone; may be null
     * @see #apply(Changes)
     */
    void apply( Changes changes,
                Observer observer );

    /**
     * An interface that can be supplied to the {@link Editor#apply(Changes,Observer)} and {@link Editor#apply(Changes, Observer)}
     * methods to receive notifications of the changes that were applied or undone.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    public static interface Observer {

        /**
         * Set the supplied entry in the array at the given path.
         * 
         * @param path the path within the document of the array
         * @param entry the entry containing the new value and the index
         */
        void setArrayValue( Path path,
                            Array.Entry entry );

        /**
         * Insert the entry into the array at the given path.
         * 
         * @param path the path within the document of the array
         * @param entry the entry containing the new value and the index
         */
        void addArrayValue( Path path,
                            Array.Entry entry );

        /**
         * Remove the entry from the array at the given path.
         * 
         * @param path the path within the document of the array
         * @param entry the entry containing the new value and the index
         */
        void removeArrayValue( Path path,
                               Array.Entry entry );

        /**
         * Remove all fields from the document at the supplied path.
         * 
         * @param path the path to the document
         */
        void clear( Path path );

        /**
         * Set to the given value the field in the document at the supplied path.
         * 
         * @param parentPath the path to the parent document in which the field should be updated
         * @param field the name of the field to be updated
         * @param newValue the new value
         */
        void put( Path parentPath,
                  String field,
                  Object newValue );

        /**
         * Remove the field from the document at the supplied path.
         * 
         * @param parentPath the path to the parent document in which the field should be removed
         * @param field the name of the field to be removed
         */
        void remove( Path parentPath,
                     String field );

    }

}
