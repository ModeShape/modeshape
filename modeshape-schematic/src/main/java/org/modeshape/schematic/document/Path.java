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

import org.modeshape.schematic.annotation.Immutable;

/**
 * The path to a field somewhere within a document.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
public interface Path extends Iterable<String>, Comparable<Path> {

    /**
     * Get a particular segment within this path.
     * 
     * @param segmentNumber the 0-based index of the segment in this path
     * @return the segment; never null
     * @throws IndexOutOfBoundsException if the segment number is negative, or greater than or equal to the {@link #size() size}.
     */
    String get( int segmentNumber );

    /**
     * Get the last segment in the path.
     * 
     * @return the field name in the last segment of this path, or null if this is the empty path
     */
    String getLast();

    /**
     * Get the first segment in the path.
     * 
     * @return the field name in the first segment of this path, or null if this is the empty path
     */
    String getFirst();

    /**
     * Get the number of segments in this path. An empty path with have a size of '0'.
     * 
     * @return the size of this path; never negative
     */
    int size();

    /**
     * Obtain a path that has this path as the parent and which has as the last segment the supplied field name.
     * 
     * @param fieldName the field name for the last segment in the new path; may be null
     * @return the new path, or this path if the <code>fieldName</code> parameter is null; never null
     */
    Path with( String fieldName );

    /**
     * Get the parent path, which may be an empty path.
     * 
     * @return the parent path; never null
     */
    Path parent();

    /**
     * Determine if the first segments of this path are equal to the segments in the supplied path. This method returns true if
     * the two paths are equal, or if the supplied path is an ancestor of this path.
     * 
     * @param other the other path; may not be null
     * @return true if the other path is equal to or an ancestor of this path, or false otherwise
     */
    boolean startsWith( Path other );

}
