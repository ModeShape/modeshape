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
package org.jboss.dna.connector.federation.merge;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public interface FederatedNode {
    /**
     * Get the unique identifier for this federated node.
     * 
     * @return the UUID; never null
     */
    UUID getUuid();

    /**
     * Get the name of this node.
     * 
     * @return the node name; never null
     */
    Name getName();

    /**
     * Get the property with the given name.
     * 
     * @param propertyName the property name
     * @return the property, or null if the property does not exist
     * @throws IllegalArgumentException if the name is null
     */
    Property getProperty( Name propertyName );

    /**
     * Get the number of properties on this node. This value is technically an estimate, as it may not exactly match the number of
     * properties returned by {@link #getProperties()} or {@link #getPropertyNames()}.
     * 
     * @return the approximate number of properties.
     */
    int getPropertyCount();

    /**
     * Get the properties. This method returns a consistent set of properties at the moment this method is called, and is not
     * affected by additions or change to the properties. In other words, it is safe for concurrent operations and is not a
     * fail-fast iterator.
     * 
     * @return the properties on this node via an immutable iterator
     */
    Iterator<Property> getProperties();

    /**
     * Get the names of the properties for this node. This method returns a consistent set of names at the moment this method is
     * called, and is not affected by additions or change to the properties. In other words, it is safe for concurrent operations
     * and is not a fail-fast iterator.
     * 
     * @return the property names via an immutable iterator
     */
    Iterator<Name> getPropertyNames();

    /**
     * Set the property if it is not already set.
     * 
     * @param property the property
     * @return the existing property, or null if there was no property and the supplied property was set
     * @throws IllegalArgumentException if the property is null
     */
    Property setPropertyIfAbsent( Property property );

    /**
     * Set the supplied properties. This method will overwrite any existing properties with the new properties if they have the
     * same {@link Property#getName() property name}. This method ignores any null property references, and does nothing if there
     * are no properties supplied.
     * 
     * @param properties the properties that should be set
     */
    void setProperties( Property... properties );

    /**
     * Set the supplied properties. This method will overwrite any existing properties with the new properties if they have the
     * same {@link Property#getName() property name}. This method ignores any null property references, and does nothing if there
     * are no properties supplied.
     * 
     * @param properties the properties that should be set
     */
    void setProperties( Iterable<Property> properties );

    /**
     * Replace all existing properties with the supplied properties. This method ignores any null property references, and does
     * nothing if there are no properties supplied.
     * 
     * @param properties the properties that should be set
     */
    void setAllProperties( Property... properties );

    /**
     * Replace all existing properties with the supplied properties. This method ignores any null property references, and does
     * nothing if there are no properties supplied.
     * 
     * @param properties the properties that should replace the existing properties
     */
    void setAllProperties( Iterable<Property> properties );

    /**
     * Remove all properties, except for the {@link #getName() name} and {@link #getUuid() identifier}.
     */
    void removeAllProperties();

    /**
     * Get the sources that have contributed information to this node.
     * 
     * @return the names of the sources that have contributed content to this node.
     */
    Set<String> getContributingSources();

}
