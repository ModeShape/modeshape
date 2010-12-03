/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.graph.io;

import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Interface used internally as the destination for the requests. This is used to abstract whether the requests should be
 * submitted immediately or in a single batch.
 */
@NotThreadSafe
public interface Destination {

    /**
     * Obtain the execution context of the destination.
     * 
     * @return the destination's execution context
     */
    public ExecutionContext getExecutionContext();

    /**
     * Create a node at the supplied path and with the supplied attributes. The path will be absolute.
     * 
     * @param path the absolute path of the node
     * @param properties the properties for the node; never null, but may be empty if there are no properties
     */
    public void create( Path path,
                        Iterable<Property> properties );

    /**
     * Create a node at the supplied path and with the supplied attributes. The path will be absolute.
     * 
     * @param path the absolute path of the node
     * @param firstProperty the first property
     * @param additionalProperties the remaining properties for the node
     */
    public void create( Path path,
                        Property firstProperty,
                        Property... additionalProperties );

    /**
     * Sets the given properties on the node at the supplied path. The path will be absolute.
     * 
     * @param path the absolute path of the node
     * @param properties the remaining properties for the node
     */
    public void setProperties( Path path,
                               Property... properties );

    /**
     * Signal to this destination that any enqueued create requests should be submitted. Usually this happens at the end of the
     * document parsing, but an implementer must allow for it to be called multiple times and anytime during parsing.
     */
    public void submit();
}
