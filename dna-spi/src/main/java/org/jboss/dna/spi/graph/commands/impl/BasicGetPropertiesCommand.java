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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicGetPropertiesCommand extends BasicGraphCommand implements GetPropertiesCommand {

    /**
     */
    private static final long serialVersionUID = -7816393217506909521L;
    private final Map<Name, List<Object>> propertyValues = new HashMap<Name, List<Object>>();
    private final Path path;
    private CachePolicy cachePolicy;
    private long timeLoaded;

    /**
     * @param path the path to the node; may not be null
     */
    public BasicGetPropertiesCommand( Path path ) {
        super();
        assert path != null;
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Name propertyName, Object... values ) {
        setProperty(propertyValues, propertyName, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Name propertyName, Iterable<?> values ) {
        setProperty(propertyValues, propertyName, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Name propertyName, Iterator<?> values ) {
        setProperty(propertyValues, propertyName, values);
    }

    /**
     * Get the property values that were added to the command
     * 
     * @return the map of property name to values
     */
    public Map<Name, List<Object>> getPropertyValues() {
        return this.propertyValues;
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
