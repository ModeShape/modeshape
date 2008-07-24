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

import java.util.HashSet;
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
public class BasicFederatedNode implements FederatedNode {

    private final UUID uuid;

    // private Name name;

    /**
     * Create a new federated node instance.
     * 
     * @param uuid the UUID; may not be null
     */
    public BasicFederatedNode( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return null;
    }

    /**
     * @param name Sets name to the specified value; may not be null
     */
    public void setName( Name name ) {
        assert name != null;
        // this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getContributingSources() {
        Set<String> names = new HashSet<String>();
        return names;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Property> getProperties() {
        // Compute merged properties ...
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Property getProperty( Name propertyName ) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getPropertyCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Name> getPropertyNames() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAllProperties() {
    }

    /**
     * {@inheritDoc}
     */
    public void setAllProperties( Property... properties ) {
    }

    /**
     * {@inheritDoc}
     */
    public void setAllProperties( Iterable<Property> properties ) {
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties( Property... properties ) {
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties( Iterable<Property> properties ) {
    }

    /**
     * {@inheritDoc}
     */
    public Property setPropertyIfAbsent( Property property ) {
        return null;
    }

}
