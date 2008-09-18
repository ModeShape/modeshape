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

import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.GetChildrenCommand;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.Path.Segment;

/**
 * @author Randall Hauch
 */
public class ProjectedGetChildrenCommand extends ActsOnProjectedPathCommand<GetChildrenCommand> implements GetChildrenCommand {

    /**
     */
    private static final long serialVersionUID = 1L;

    public ProjectedGetChildrenCommand( GetChildrenCommand delegate,
                                        Path projectedPath ) {
        super(delegate, projectedPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.GetChildrenCommand#addChild(org.jboss.dna.graph.properties.Path.Segment,
     *      org.jboss.dna.graph.properties.Property[])
     */
    public void addChild( Segment nameOfChild,
                          Property... identityProperties ) {
        getOriginalCommand().addChild(nameOfChild, identityProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.GetChildrenCommand#setNoChildren()
     */
    public void setNoChildren() {
        getOriginalCommand().setNoChildren();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#getCachePolicy()
     */
    public CachePolicy getCachePolicy() {
        return getOriginalCommand().getCachePolicy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#getTimeLoaded()
     */
    public DateTime getTimeLoaded() {
        return getOriginalCommand().getTimeLoaded();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.cache.Cacheable#setCachePolicy(org.jboss.dna.graph.cache.CachePolicy)
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        getOriginalCommand().setCachePolicy(cachePolicy);
    }

}
