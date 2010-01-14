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
package org.modeshape.graph.cache;

import java.io.Serializable;
import org.modeshape.graph.property.DateTime;

/**
 * Interface defining an object that can be cached according to a {@link CachePolicy}.
 */
public interface Cacheable extends Serializable {

    /**
     * Get the time that this node data was originally loaded.
     * 
     * @return the system time (in milliseconds) that the node data was loaded
     */
    DateTime getTimeLoaded();

    /**
     * Get the caching policy to be used for this object.
     * <p>
     * Note that the values of the policy are relative to the {@link #getTimeLoaded() time the node was loaded}, so the same
     * instance can be used for many nodes.
     * </p>
     * 
     * @return cachePolicy the caching policy, which may not be null
     */
    public CachePolicy getCachePolicy();

    /**
     * Set the caching policy for this object.
     * 
     * @param cachePolicy the caching policy to use for this object
     * @throws IllegalArgumentException if the cachePolicy is null
     */
    public void setCachePolicy( CachePolicy cachePolicy );

}
