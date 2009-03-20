/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr.cache;

import java.util.Iterator;
import java.util.UUID;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;

/**
 * Class that maintains the ordered list of {@link ChildNode} instances yet allows fast access to the children with a specified
 * name.
 */
public interface Children extends Iterable<ChildNode> {

    /**
     * Get the number of children.
     * 
     * @return the number of children
     */
    int size();

    /**
     * The UUID of the parent node.
     * 
     * @return the parent node's UUID
     */
    UUID getParentUuid();

    /**
     * Get the child with the given UUID.
     * 
     * @param uuid the UUID of the child node
     * @return the child node, or null if there is no child with the supplied UUID
     */
    ChildNode getChild( UUID uuid );

    /**
     * Get the child given the path segment.
     * 
     * @param segment the path segment for the child, which includes the {@link Path.Segment#getName() name} and
     *        {@link Path.Segment#getIndex() one-based same-name-sibling index}; may not be null
     * @return the information for the child node, or null if no such child existed
     */
    ChildNode getChild( Path.Segment segment );

    /**
     * Get the same-name-sibling children that all share the supplied name, in order of increasing SNS index.
     * 
     * @param name the name for the children; may not be null
     * @return the children with the supplied name; never null
     */
    Iterator<ChildNode> getChildren( Name name );

    /**
     * Get the number of same-name-siblings that all share the supplied name.
     * 
     * @param name the name for the children; may not be null
     * @return the number of same-name-siblings with the supplied name
     */
    int getCountOfSameNameSiblingsWithName( Name name );

}
