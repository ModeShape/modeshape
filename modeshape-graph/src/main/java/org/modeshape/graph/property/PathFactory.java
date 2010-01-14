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
package org.modeshape.graph.property;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.text.TextDecoder;

/**
 * A factory for creating {@link Path paths}. This interface extends the {@link ValueFactory} generic interface and adds specific
 * methods for creating paths (and relative paths) from a series of names, segments, or combinations.
 */
@ThreadSafe
public interface PathFactory extends ValueFactory<Path> {

    /**
     * Create an absolute root path. Subsequent calls will always return the same instance.
     * 
     * @return the new path
     */
    Path createRootPath();

    /**
     * Create an absolute path with the supplied segment names, in order. If no segments are provided, the result will be the root
     * path.
     * 
     * @param segmentNames the names of the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment name is provided and if any of the supplied segment names are null
     */
    Path createAbsolutePath( Name... segmentNames );

    /**
     * Create an absolute path with the supplied segments, in order. If no segments are provided, the result will be the root
     * path.
     * 
     * @param segments the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment is provided and if any of the supplied segments are null
     */
    Path createAbsolutePath( Path.Segment... segments );

    /**
     * Create an absolute path with the supplied segments, in order. If no segments are provided, the result will be the root
     * path.
     * 
     * @param segments the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment is provided and if any of the supplied segments are null
     */
    Path createAbsolutePath( Iterable<Path.Segment> segments );

    /**
     * Create an empty relative path (i.e., equivalent to {@link #createRelativePath(Path.Segment...) createRelativePath}(
     * {@link Path#SELF_SEGMENT})). Subsequent calls will always return the same instance.
     * 
     * @return the new path
     */
    Path createRelativePath();

    /**
     * Create a relative path with the supplied segment names, in order. If no segments are provided, the result will be the root
     * path.
     * 
     * @param segmentNames the names of the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment name is provided and if any of the supplied segment names are null
     */
    Path createRelativePath( Name... segmentNames );

    /**
     * Create a relative path with the supplied segments, in order. If no segments are provided, the result will be the root path.
     * 
     * @param segments the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment is provided and if any of the supplied segments are null
     */
    Path createRelativePath( Path.Segment... segments );

    /**
     * Create a relative path with the supplied segments, in order. If no segments are provided, the result will be the root path.
     * 
     * @param segments the segments
     * @return the new path
     * @throws IllegalArgumentException if at least one segment is provided and if any of the supplied segments are null
     */
    Path createRelativePath( Iterable<Path.Segment> segments );

    /**
     * Create a path by appending the supplied relative path to the supplied parent path. The resulting path will be
     * {@link Path#isAbsolute() absolute} if the supplied parent path is absolute.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param childPath the path that should be appended to the parent path
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference or the child path reference is null
     */
    Path create( Path parentPath,
                 Path childPath );

    /**
     * Create a path by appending the supplied names to the parent path.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param segmentName the name of the segment to be appended to the parent path
     * @param index the index for the new segment
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference or the segment name is null, or if the index is invalid
     */
    Path create( Path parentPath,
                 Name segmentName,
                 int index );

    /**
     * Create a path by appending the supplied names to the parent path.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param segmentName the name of the segment to be appended to the parent path
     * @param index the index for the new segment
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference or the segment name is null, or if the index is invalid
     */
    Path create( Path parentPath,
                 String segmentName,
                 int index );

    /**
     * Create a path by appending the supplied names to the parent path. If no names are appended, the parent path is returned.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param segmentNames the names of the segments that are to be appended, in order, to the parent path
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference is null, or if at least one segment name is provided and if
     *         any of the supplied segment names are null
     */
    Path create( Path parentPath,
                 Name... segmentNames );

    /**
     * Create a path by appending the supplied names to the parent path. If no names are appended, the parent path is returned.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param segments the segments that are to be appended, in order, to the parent path
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference is null, or if at least one segment name is provided and if
     *         any of the supplied segment names are null
     */
    Path create( Path parentPath,
                 Path.Segment... segments );

    /**
     * Create a path by appending the supplied names to the parent path. If no names are appended, the parent path is returned.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param segments the segments that are to be appended, in order, to the parent path
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference is null, or if at least one segment name is provided and if
     *         any of the supplied segment names are null
     */
    Path create( Path parentPath,
                 Iterable<Path.Segment> segments );

    /**
     * Create a path by appending the supplied names to the parent path.
     * 
     * @param parentPath the path that is to provide the basis for the new path
     * @param subpath the subpath to be appended to the parent path, which must be in the form of a relative path
     * @return the new path
     * @throws IllegalArgumentException if the parent path reference or the segment name is null, or if the index is invalid
     */
    Path create( Path parentPath,
                 String subpath );

    /**
     * Create a path segment given the supplied segment name. The supplied string may contain a same-name-sibling index in the
     * form of "<code>[<i>n</i>]</code>" at the end of the name, where <i>n</i> is a positive integer. Note that the
     * same-name-sibling index is 1-based, not zero-based.
     * 
     * @param segmentName the name of the segment
     * @return the segment
     * @throws IllegalArgumentException if the segment name reference is <code>null</code> or the value could not be created from
     *         the supplied string
     * @throws ValueFormatException if the same-name-sibling index is not an integer, or if the supplied string is not a valid
     *         segment name
     */
    Path.Segment createSegment( String segmentName );

    /**
     * Create a path segment given the supplied segment name. The supplied string may contain a same-name-sibling index in the
     * form of "<code>[<i>n</i>]</code>" at the end of the name, where <i>n</i> is a positive integer. Note that the
     * same-name-sibling index is 1-based, not zero-based.
     * 
     * @param segmentName the name of the segment
     * @param decoder the decoder that should be used to decode the qualified name
     * @return the segment
     * @throws IllegalArgumentException if the segment name reference is <code>null</code> or the value could not be created from
     *         the supplied string
     * @throws ValueFormatException if the same-name-sibling index is not an integer, or if the supplied string is not a valid
     *         segment name
     */
    Path.Segment createSegment( String segmentName,
                                TextDecoder decoder );

    /**
     * Create a path segment given the supplied segment name and index.
     * 
     * @param segmentName the name of the new segment
     * @param index the index of the new segment
     * @return the segment
     * @throws IllegalArgumentException if the segment name reference is <code>null</code> or if the index is invalid
     * @throws ValueFormatException if the supplied string is not a valid segment name
     */
    Path.Segment createSegment( String segmentName,
                                int index );

    /**
     * Create a path segment given the supplied segment name. The resulting segment will have no index.
     * 
     * @param segmentName the name of the segment
     * @return the segment
     * @throws IllegalArgumentException if the segment name reference is null
     */
    Path.Segment createSegment( Name segmentName );

    /**
     * Create a path segment given the supplied segment name and index.
     * 
     * @param segmentName the name of the new segment
     * @param index the index of the new segment
     * @return the segment
     * @throws IllegalArgumentException if the segment name reference is null or if the index is invalid
     */
    Path.Segment createSegment( Name segmentName,
                                int index );

}
