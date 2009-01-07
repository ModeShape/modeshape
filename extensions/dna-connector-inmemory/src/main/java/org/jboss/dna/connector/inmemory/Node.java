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
package org.jboss.dna.connector.inmemory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class Node {

    private final UUID uuid;
    private Node parent;
    private Path.Segment name;
    private final Map<Name, Property> properties = new HashMap<Name, Property>();
    private final List<Node> children = new LinkedList<Node>();

    public Node( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
    }

    /**
     * @return uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return name
     */
    public Path.Segment getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    protected void setName( Path.Segment name ) {
        this.name = name;
    }

    /**
     * @return parent
     */
    public Node getParent() {
        return parent;
    }

    /**
     * @param parent Sets parent to the specified value.
     */
    protected void setParent( Node parent ) {
        this.parent = parent;
    }

    /**
     * @return children
     */
    protected List<Node> getChildren() {
        return children;
    }

    /**
     * @return properties
     */
    protected Map<Name, Property> getProperties() {
        return properties;
    }

    public Node setProperty( Property property ) {
        if (property != null) {
            this.properties.put(property.getName(), property);
        }
        return this;
    }

    public Node setProperty( ExecutionContext context,
                             String name,
                             Object... values ) {
        PropertyFactory propertyFactory = context.getPropertyFactory();
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return setProperty(propertyFactory.create(propertyName, values));
    }

    public Property getProperty( ExecutionContext context,
                                 String name ) {
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return getProperty(propertyName);
    }

    public Property getProperty( Name name ) {
        return this.properties.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Node) {
            Node that = (Node)obj;
            if (!this.getUuid().equals(that.getUuid())) return false;
            return true;
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
        if (this.name == null) {
            sb.append("");
        } else {
            sb.append(this.name);
        }
        sb.append(" (").append(uuid).append(")");
        return sb.toString();
    }
}
