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
package org.jboss.dna.graph.requests;

import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.cache.Cacheable;
import org.jboss.dna.graph.properties.DateTime;

/**
 * A request that contains results that may be cached.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public abstract class CacheableRequest extends Request implements Cacheable {

    private static final long serialVersionUID = 1L;

    private CachePolicy policy;
    private DateTime timeLoaded;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#getCachePolicy()
     */
    public CachePolicy getCachePolicy() {
        return policy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#getTimeLoaded()
     */
    public DateTime getTimeLoaded() {
        return timeLoaded;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#setCachePolicy(org.jboss.dna.graph.cache.CachePolicy)
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        policy = cachePolicy;
    }

    /**
     * @param timeLoaded Sets timeLoaded to the specified value.
     */
    public void setTimeLoaded( DateTime timeLoaded ) {
        this.timeLoaded = timeLoaded;
    }
}
