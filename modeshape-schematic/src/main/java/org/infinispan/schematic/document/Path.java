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
package org.infinispan.schematic.document;

/**
 * The path to a field somewhere within a document.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
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
