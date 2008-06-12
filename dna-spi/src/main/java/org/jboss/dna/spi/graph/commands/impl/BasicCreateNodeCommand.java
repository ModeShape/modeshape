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
package org.jboss.dna.spi.graph.commands.impl;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicCreateNodeCommand extends BasicGraphCommand implements CreateNodeCommand {

    /**
     */
    private static final long serialVersionUID = -5452285887178397354L;
    private final Path path;
    private final List<Property> properties;
    private CachePolicy cachePolicy;
    private long timeLoaded;

    /**
     * @param path the path to the node; may not be null
     * @param properties the properties of the node; may not be null
     */
    public BasicCreateNodeCommand( Path path,
                                   List<Property> properties ) {
        super();
        assert path != null;
        assert properties != null;
        this.properties = properties;
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<Property> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeLoaded() {
        return timeLoaded;
    }

    /**
     * @param timeLoaded Sets timeLoaded to the specified value.
     */
    public void setTimeLoaded( long timeLoaded ) {
        this.timeLoaded = timeLoaded;
    }

    /**
     * {@inheritDoc}
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        this.cachePolicy = cachePolicy;
    }

}
