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
package org.jboss.dna.connector.federation.executor;

import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;

/**
 * @author Randall Hauch
 */
public class ProjectedGetPropertiesCommand extends ActsOnProjectedPathCommand<GetPropertiesCommand>
    implements GetPropertiesCommand {

    /**
     */
    private static final long serialVersionUID = 1L;

    public ProjectedGetPropertiesCommand( GetPropertiesCommand delegate,
                                          Path projectedPath ) {
        super(delegate, projectedPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.cache.Cacheable#getCachePolicy()
     */
    public CachePolicy getCachePolicy() {
        return getOriginalCommand().getCachePolicy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.cache.Cacheable#getTimeLoaded()
     */
    public DateTime getTimeLoaded() {
        return getOriginalCommand().getTimeLoaded();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.cache.Cacheable#setCachePolicy(org.jboss.dna.spi.cache.CachePolicy)
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        getOriginalCommand().setCachePolicy(cachePolicy);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetPropertiesCommand#setProperty(org.jboss.dna.spi.graph.Property)
     */
    public void setProperty( Property property ) {
        getOriginalCommand().setProperty(property);
    }

}
