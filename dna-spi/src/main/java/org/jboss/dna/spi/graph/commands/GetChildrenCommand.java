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
package org.jboss.dna.spi.graph.commands;

import java.util.Iterator;
import org.jboss.dna.spi.cache.Cacheable;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;

/**
 * A command to get the children of a single node given its path.
 * 
 * @author Randall Hauch
 */
public interface GetChildrenCommand extends GraphCommand, ActsOnPath, Cacheable {

    /**
     * Set the children of this node using an iterator of names. Any existing child references already set on this command will be
     * replaced by those supplied to this method.
     * <p>
     * The indexes of the same-name siblings will be determined by the order in which they appear in the iterator.
     * </p>
     * <p>
     * The caller may supply a custom iterator implementation, which will be called on this same connection within the same
     * transaction when the node data is processed and consumed. This is useful, for example, if the iterator to transparently
     * page through the information without requiring all children to be pulled into memory.
     * </p>
     * 
     * @param namesOfChildren the iterator over the names of children; may be null if there are no children
     */
    void setChildren( Iterator<Path.Segment> namesOfChildren );

    /**
     * Set the children of this node using an iterator of names. Any existing child references already set on this command will be
     * replaced by those supplied to this method.
     * <p>
     * The indexes of the same-name siblings will be determined by the order in which they appear in the iterator.
     * </p>
     * <p>
     * The caller may supply a custom {@link Iterable} implementation, which will be called on this same connection within the
     * same transaction when the node data is processed and consumed. This is useful, for example, if the iterator to
     * transparently page through the information without requiring all children to be pulled into memory.
     * </p>
     * 
     * @param namesOfChildren the iterable names of children; may be null if there are no children
     */
    void setChildren( Iterable<Path.Segment> namesOfChildren );

    /**
     * Set the children of this node using the array of names. Any existing child references already set on this command will be
     * replaced by those supplied to this method.
     * <p>
     * The indexes of the same-name siblings will be determined by the order in which they appear in the iterator.
     * </p>
     * 
     * @param namesOfChildren the names of children; may be null if there are no children
     */
    void setChildren( Path.Segment... namesOfChildren );

    /**
     * Set the child of this node using the supplied name. Any existing child references already set on this command will be
     * replaced by those supplied to this method. Note that a {@link Path.Segment segment} is not required in this case because
     * there is only one child and (by definition) no index.
     * 
     * @param nameOfChild
     */
    void setChild( Name nameOfChild );

    /**
     * Set that this node has no children. Any existing child references already set on this command will be removed.
     */
    void setNoChildren();
}
