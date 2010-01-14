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

import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;

/**
 * A basic mutable {@link CachePolicy} implementation.
 */
@NotThreadSafe
public class BasicCachePolicy implements CachePolicy {

    private static final long serialVersionUID = 1L;
    private long timeToLiveInMillis = 0L;

    public BasicCachePolicy() {
    }

    public BasicCachePolicy( long timeToCache,
                             TimeUnit unit ) {
        CheckArg.isNotNull(unit, "unit");
        this.timeToLiveInMillis = TimeUnit.MILLISECONDS.convert(timeToCache, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.cache.CachePolicy#getTimeToLive()
     */
    public long getTimeToLive() {
        return this.timeToLiveInMillis;
    }

    /**
     * Set the time for values and information to live in the cache.
     * 
     * @param timeToLive Sets timeToLive to the specified value.
     * @param unit the unit in which the time to live value is defined
     */
    public void setTimeToLive( long timeToLive,
                               TimeUnit unit ) {
        this.timeToLiveInMillis = TimeUnit.NANOSECONDS.convert(timeToLive, unit);
    }

    public boolean isEmpty() {
        return this.timeToLiveInMillis == 0;
    }

    public CachePolicy getUnmodifiable() {
        return new ImmutableCachePolicy(this.getTimeToLive());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CachePolicy) {
            CachePolicy that = (CachePolicy)obj;
            if (this.getTimeToLive() != that.getTimeToLive()) return false;
            if (obj instanceof BasicCachePolicy) return true;
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
        return "{ TTL=" + this.timeToLiveInMillis + " ms }";
    }

}
