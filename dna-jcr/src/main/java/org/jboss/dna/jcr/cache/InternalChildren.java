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

import java.util.UUID;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.PathFactory;

/**
 * An internal interface for the {@link Children} implementations. Methods on this interface are used internally and should not be
 * used by components or clients.
 */
interface InternalChildren extends Children {

    /**
     * Create another Children object that is equivalent to this node but with the supplied child added.
     * 
     * @param newChildName the name of the new child; may not be null
     * @param newChildUuid the UUID of the new child; may not be null
     * @param pathFactory the factory that can be used to create Path and/or Path.Segment instances.
     * @return the new Children object; never null
     */
    ChangedChildren with( Name newChildName,
                          UUID newChildUuid,
                          PathFactory pathFactory );

    /**
     * Create another Children object that is equivalent to this node but without the supplied child.
     * 
     * @param childUuid the UUID of the child to be removed; may not be null
     * @param pathFactory the factory that can be used to create Path and/or Path.Segment instances.
     * @return the new Children object; never null
     */
    ChangedChildren without( UUID childUuid,
                             PathFactory pathFactory );
}
