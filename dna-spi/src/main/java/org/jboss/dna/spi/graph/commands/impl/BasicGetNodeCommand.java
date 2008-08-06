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
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicGetNodeCommand extends BasicGetChildrenCommand implements GetNodeCommand {

    private static final long serialVersionUID = 5355669032301356873L;
    private final Map<Name, Property> properties = new HashMap<Name, Property>();

    /**
     * @param path
     */
    public BasicGetNodeCommand( Path path ) {
        super(path);

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
        List<Path.Segment> children = this.getChildren();
        if (children != null && children.size() > 0) {
            sb.append(" with ").append(children.size()).append(" children: ");
            sb.append(StringUtil.readableString(children));
        }
        return sb.toString();
    }

}
