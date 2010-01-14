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

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.cache.Cacheable;
import org.modeshape.graph.property.DateTime;

/**
 * A request that contains results that may be cached.
 */
@ThreadSafe
public abstract class CacheableRequest extends Request implements Cacheable {

    private static final long serialVersionUID = 1L;

    private CachePolicy policy;
    private DateTime timeLoaded;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.cache.Cacheable#getCachePolicy()
     */
    public CachePolicy getCachePolicy() {
        return policy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.cache.Cacheable#getTimeLoaded()
     */
    public DateTime getTimeLoaded() {
        return timeLoaded;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException if the request is frozen
     * @see org.modeshape.graph.cache.Cacheable#setCachePolicy(org.modeshape.graph.cache.CachePolicy)
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        checkNotFrozen();
        policy = cachePolicy;
    }

    /**
     * @param timeLoaded Sets timeLoaded to the specified value.
     * @throws IllegalStateException if the request is frozen
     */
    public void setTimeLoaded( DateTime timeLoaded ) {
        checkNotFrozen();
        this.timeLoaded = timeLoaded;
    }
}
