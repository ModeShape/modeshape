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
package org.jboss.dna.spi.cache;

/**
 * @author Randall Hauch
 */
public class BasicCachePolicy implements CachePolicy {

    private static final long serialVersionUID = 1L;
    private long timeToCache;
    private long timeToExpire;

    public BasicCachePolicy() {
    }

    public BasicCachePolicy( long timeToCache,
                             long timeToExpire ) {
        this.timeToCache = timeToCache;
        this.timeToExpire = timeToExpire;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.cache.CachePolicy#getTimeToCache()
     */
    public long getTimeToCache() {
        return this.timeToCache;
    }

    /**
     * @param timeToCache Sets timeToCache to the specified value.
     */
    public void setTimeToCache( long timeToCache ) {
        this.timeToCache = timeToCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.cache.CachePolicy#getTimeToExpire()
     */
    public long getTimeToExpire() {
        return this.timeToExpire;
    }

    /**
     * @param timeToExpire Sets timeToExpire to the specified value.
     */
    public void setTimeToExpire( long timeToExpire ) {
        this.timeToExpire = timeToExpire;
    }

    public boolean isEmpty() {
        return this.timeToCache == 0 && this.timeToExpire == 0;
    }

    public CachePolicy getUnmodifiable() {
        return new ImmutableCachePolicy(this.getTimeToCache(), this.getTimeToExpire());
    }

}
