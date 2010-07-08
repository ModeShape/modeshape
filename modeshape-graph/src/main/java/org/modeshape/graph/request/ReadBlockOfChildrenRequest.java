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
package org.modeshape.graph.request;

import java.util.LinkedList;
import java.util.List;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Instruction to read a block of the children of a node, where the block is dictated by the {@link #startingAtIndex() starting
 * index} and the {@link #count() maximum number of children} to include in the block. This command is useful when paging through
 * a large number of children.
 * 
 * @see ReadNextBlockOfChildrenRequest
 */
public class ReadBlockOfChildrenRequest extends CacheableRequest {

    public static final int INDEX_NOT_USED = -1;

    private static final long serialVersionUID = 1L;

    private final Location of;
    private final String workspaceName;
    private final List<Location> children = new LinkedList<Location>();
    private final int startingAtIndex;
    private final int count;
    private Location actualLocation;

    /**
     * Create a request to read a block of the children of a node at the supplied location. The block is defined by the starting
     * index of the first child and the number of children to include. Note that this index is <i>not</i> the
     * {@link Path.Segment#getIndex() same-name-sibiling index}, but rather is the index of the child as if the children were in
     * an array.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace containing the parent
     * @param startingIndex the zero-based index of the first child to be included in the block
     * @param count the maximum number of children that should be included in the block
     * @throws IllegalArgumentException if the location or workspace name is null, if <code>startingIndex</code> is negative, or
     *         if <code>count</count> is less than 1.
     */
    public ReadBlockOfChildrenRequest( Location of,
                                       String workspaceName,
                                       int startingIndex,
                                       int count ) {
        CheckArg.isNotNull(of, "of");
        CheckArg.isNonNegative(startingIndex, "startingIndex");
        CheckArg.isPositive(count, "count");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.of = of;
        this.startingAtIndex = startingIndex;
        this.count = count;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Get the location defining the node whose children are to be read.
     * 
     * @return the location of the parent node; never null
     */
    public Location of() {
        return of;
    }

    /**
     * Get the name of the workspace in which the parent and children exist.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the maximum number of children that may be returned in the block.
     * 
     * @return the block's maximum count
     * @see #startingAtIndex()
     * @see #endingBefore()
     */
    public int count() {
        return this.count;
    }

    /**
     * Get the starting index of the block, which is the index of the first child to include. This index corresponds to the index
     * of all children in the list, not the {@link Path.Segment#getIndex() same-name-sibiling index}.
     * 
     * @return the (zero-based) child index at which this block starts; never negative and always less than
     *         {@link #endingBefore()}
     * @see #endingBefore()
     * @see #count()
     */
    public int startingAtIndex() {
        return this.startingAtIndex;
    }

    /**
     * Get the index past the last child that is to be included in the block. This index corresponds to the index of all children
     * in the list, not the {@link Path.Segment#getIndex() same-name-sibiling index}.
     * 
     * @return the index just past the last child included in the block; always positive and always greater than
     *         {@link #startingAtIndex()}.
     * @see #startingAtIndex()
     * @see #count()
     */
    public int endingBefore() {
        return this.startingAtIndex + this.count;
    }

    /**
     * Get the children that were read from the {@link RepositoryConnection} after the request was processed. Each child is
     * represented by a location.
     * 
     * @return the children that were read; never null
     */
    public List<Location> getChildren() {
        return children;
    }

    /**
     * Add to the list of children that has been read the supplied children with the given path and identification properties. The
     * children are added in order.
     * 
     * @param children the locations of the children that were read
     * @throws IllegalArgumentException if the parameter is null
     * @throws IllegalStateException if the request is frozen
     * @see #addChild(Location)
     * @see #addChild(Path, Property)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChildren( Iterable<Location> children ) {
        checkNotFrozen();
        CheckArg.isNotNull(children, "children");
        for (Location child : children) {
            if (child != null) this.children.add(child);
        }
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param child the location of the child that was read
     * @throws IllegalArgumentException if the location is null
     * @throws IllegalStateException if the request is frozen
     * @see #addChild(Path, Property)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Location child ) {
        checkNotFrozen();
        CheckArg.isNotNull(child, "child");
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param firstIdProperty the first identification property of the child that was just read
     * @param remainingIdProperties the remaining identification properties of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @throws IllegalStateException if the request is frozen
     * @see #addChild(Location)
     * @see #addChild(Path, Property)
     */
    public void addChild( Path pathToChild,
                          Property firstIdProperty,
                          Property... remainingIdProperties ) {
        checkNotFrozen();
        Location child = Location.create(pathToChild, firstIdProperty, remainingIdProperties);
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification property. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param idProperty the identification property of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @throws IllegalStateException if the request is frozen
     * @see #addChild(Location)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Path pathToChild,
                          Property idProperty ) {
        checkNotFrozen();
        Location child = Location.create(pathToChild, idProperty);
        this.children.add(child);
    }

    /**
     * Sets the actual and complete location of the node whose children have been read. This method must be called when processing
     * the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being read, or null if the {@link #of() current location} should be used
     * @throws IllegalArgumentException if the actual location is null or does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
    }

    /**
     * Get the actual location of the node whose children were read.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocation = null;
        this.children.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(of, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            ReadBlockOfChildrenRequest that = (ReadBlockOfChildrenRequest)obj;
            if (!this.of().isSame(that.of())) return false;
            if (this.startingAtIndex() != that.startingAtIndex()) return false;
            if (this.count() != that.count()) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        Inflector inflector = Inflector.getInstance();
        if (count() == 1) {
            return "read " + inflector.ordinalize(startingAtIndex()) + " thru " + inflector.ordinalize(endingBefore() - 1)
                   + " children of " + of() + " in the \"" + workspaceName + "\" workspace";
        }
        return "read " + inflector.ordinalize(startingAtIndex()) + " child of " + of() + " in the \"" + workspaceName
               + "\" workspace";
    }

    @Override
    public RequestType getType() {
        return RequestType.READ_BLOCK_OF_CHILDREN;
    }
}
