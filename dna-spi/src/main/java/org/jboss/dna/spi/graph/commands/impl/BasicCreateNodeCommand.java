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

import java.util.Collection;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicCreateNodeCommand extends BasicGraphCommand implements CreateNodeCommand {

    /**
     */
    private static final long serialVersionUID = -5452285887178397354L;
    private final Path path;
    private final Collection<Property> properties;
    private final NodeConflictBehavior conflictBehavior;

    /**
     * @param path the path to the node; may not be null
     * @param properties the properties of the node; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     */
    public BasicCreateNodeCommand( Path path,
                                   Collection<Property> properties,
                                   NodeConflictBehavior conflictBehavior ) {
        super();
        assert path != null;
        assert properties != null;
        assert conflictBehavior != null;
        this.properties = properties;
        this.path = path;
        this.conflictBehavior = conflictBehavior;
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
    public Iterable<Property> getPropertyIterator() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    public NodeConflictBehavior getConflictBehavior() {
        return this.conflictBehavior;
    }

    public void setProperty( Property property ) {
        if (!properties.isEmpty()) {
            for (Property existing : this.properties) {
                if (existing.getName().equals(property.getName())) {
                    this.properties.remove(existing);
                    break;
                }
            }
        }
        this.properties.add(property);
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( CreateNodeCommand that ) {
        if (this == that) return 0;
        return this.getPath().compareTo(that.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CreateNodeCommand) {
            CreateNodeCommand that = (CreateNodeCommand)obj;
            return this.path.equals(that.getPath());
        }
        return false;
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
