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
package org.jboss.dna.spi.connector.commands;

import org.jboss.dna.spi.cache.Cacheable;
import org.jboss.dna.spi.graph.Name;

/**
 * Aspect interface for any repository command that acts upon or updates properties on a given node. This aspect also allows for
 * the recipient to {@link Cacheable#setCachePolicy(org.jboss.dna.spi.cache.CachePolicy) update the cache policy} for the updated
 * information.
 * @author Randall Hauch
 */
public interface ActsOnProperties extends ActsOnPath, Cacheable {

    /**
     * Set the values for the named property. Any existing property values, if previously set, will be overwritten. If there are
     * no property vlaues or if all of the property values are null, the property will be removed.
     * @param propertyName the name of the property
     * @param values the property values
     */
    void setProperty( Name propertyName, Object... values );

}
