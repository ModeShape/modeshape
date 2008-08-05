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
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicGetPropertiesCommand extends BasicGraphCommand implements GetPropertiesCommand {

    /**
     */
    private static final long serialVersionUID = -7816393217506909521L;
    private final Map<Name, Property> properties = new HashMap<Name, Property>();
    private final Path path;
    private CachePolicy cachePolicy;
    private DateTime timeLoaded;

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
    public void setProperty( Property property ) {
        if (property != null) {
            properties.put(property.getName(), property);
        }
    }

    public void setProperties( Map<Name, Property> properties ) {
        this.properties.clear();
        if (properties != null) this.properties.putAll(properties);
    }

    /**
     * Get the property values that were added to the command
     * 
     * @return the map of property name to values
     */
    public Iterable<Property> getPropertyIterator() {
        return this.properties.values();
    }

    public Map<Name, Property> getProperties() {
        return this.properties;
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
    public DateTime getTimeLoaded() {
        return timeLoaded;
    }

    /**
     * @param timeLoaded Sets timeLoaded to the specified value.
     */
    public void setTimeLoaded( DateTime timeLoaded ) {
        this.timeLoaded = timeLoaded;
    }

    /**
     * {@inheritDoc}
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        this.cachePolicy = cachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(" at ");
        sb.append(this.getPath());
        boolean firstProperty = true;
        for (Property property : this.getPropertyIterator()) {
            if (property.isEmpty()) continue;
            if (firstProperty) {
                sb.append(" { ");
                firstProperty = false;
            } else {
                sb.append("; ");
            }
            sb.append(property.getName());
            sb.append("=");
            if (property.isSingle()) {
                sb.append(StringUtil.readableString(property.getValues().next()));
            } else {
                sb.append(StringUtil.readableString(property.getValuesAsArray()));
            }
        }
        if (!firstProperty) {
            sb.append(" }");
        }
        return sb.toString();
    }
}
