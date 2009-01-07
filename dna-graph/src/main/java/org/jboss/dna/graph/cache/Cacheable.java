/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.cache;

import java.io.Serializable;
import org.jboss.dna.graph.properties.DateTime;

/**
 * Interface defining an object that can be cached according to a {@link CachePolicy}.
 * 
 * @author Randall Hauch
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
