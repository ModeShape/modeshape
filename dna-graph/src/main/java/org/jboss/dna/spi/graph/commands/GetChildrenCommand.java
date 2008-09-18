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

import org.jboss.dna.spi.cache.Cacheable;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;

/**
 * A command to get the children of a single node given its path.
 * 
 * @author Randall Hauch
 */
public interface GetChildrenCommand extends GraphCommand, ActsOnPath, Cacheable {

    /**
     * Add the child to this node. This method does not affect existing children, so callers of this method should not add a child
     * with the same segment as an existing child (this is not checked by this method).
     * 
     * @param nameOfChild the name of the child; should not be the same as an existing child (not checked)
     * @param identityProperties the property/properties that are considered identity properties (other than the name) for the
     *        child
     */
    void addChild( Path.Segment nameOfChild,
                   Property... identityProperties );

    /**
     * Set that this node has no children. Any existing child references already set on this command will be removed.
     */
    void setNoChildren();
}
