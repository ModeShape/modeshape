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

import java.util.Iterator;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;

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
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#setChild(org.jboss.dna.spi.graph.Name)
     */
    public void setChild( Name nameOfChild ) {
        getOriginalCommand().setChild(nameOfChild);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#setChildren(java.util.Iterator)
     */
    public void setChildren( Iterator<Segment> namesOfChildren ) {
        getOriginalCommand().setChildren(namesOfChildren);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#setChildren(java.lang.Iterable)
     */
    public void setChildren( Iterable<Segment> namesOfChildren ) {
        getOriginalCommand().setChildren(namesOfChildren);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#setChildren(org.jboss.dna.spi.graph.Path.Segment[])
     */
    public void setChildren( Segment... namesOfChildren ) {
        getOriginalCommand().setChildren(namesOfChildren);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#setNoChildren()
     */
    public void setNoChildren() {
        getOriginalCommand().setNoChildren();
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
    public long getTimeLoaded() {
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

}
