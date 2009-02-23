/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.jcip.annotations.Immutable;

import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;

/**
 * General purpose location type that supports a path and a property.
 * This class should never be directly instantiated by users of the DNA framework.
 * Instead, use @{link Location#create} to create the correct location.
 *
 * @see Location
 */
@Immutable
class PathPropertyLocation extends Location {

    private final Path path;
    private final Property idProperty;
    private final List<Property> idProperties;

    private final int hashCode;
    
    /**
     * Create a new location with a given path and identification property.
     * 
     * @param path the path
     * @param idProperty the identification property
     */
    PathPropertyLocation( Path path, Property idProperty ) {
		// The path could be null
		// CheckArg.isNotNull(path, "path");
		// If the properties are a null, a PathLocation should be used instead.
		CheckArg.isNotNull(idProperty, "idProperty");
		
	    this.path = path;
	    this.idProperty = idProperty;
	    this.idProperties = Collections.singletonList(idProperty);
	    
	    // Paths are immutable, Properties are immutable, the idProperties list 
	    // is wrapped in an unmodifiableList by the Location factory methods...
	    // ... so we can cache the hash code.
	    
	    hashCode = HashCode.compute(path, idProperties);
	}
	
    /**
     * {@inheritDoc}
     * 
     * @see Location#getPath()
     */
	@Override
	public final Path getPath() {
		return path;
	}

    /**
     * {@inheritDoc}
     * 
     * @see Location#getIdProperties()
     */	
	@Override
	public final List<Property> getIdProperties() {
		return idProperties;
	}

    /**
     * {@inheritDoc}
     * 
     * @see Location#hasIdProperties()
     */	
	@Override
	public final boolean hasIdProperties() {
		return true;
	}

    /**
     * {@inheritDoc}
     * 
     * @see Location#getIdProperty(Name)
     */	
	@Override
    public final Property getIdProperty( Name name ) {
        CheckArg.isNotNull(name, "name");
	    if (idProperty.getName().equals(name)) return idProperty;

	    return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getUuid()
     */	
	@Override
    public UUID getUuid() {
		if (DnaLexicon.UUID.equals(idProperty.getName())) {
            Object value = idProperty.getFirstValue();
            if (value instanceof UUID) return (UUID)value;
        }
        return null;
    }

    
    /**
     * {@inheritDoc}
     * 
     * @see Location#hashCode()
     */	
	@Override
	public int hashCode() {
		return hashCode;
	}	
	
    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */	
	@Override
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;
        
        // A new property with same name overrides the old property with the same name
        if (newIdProperty.getName().equals(idProperty.getName())) return Location.create(path, newIdProperty);
        
        List<Property> newIdProperties = new ArrayList<Property>(2);
        newIdProperties.add(idProperty);
        newIdProperties.add(newIdProperty);
                
        return Location.create(path, newIdProperties);
    }    
	
    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */	
	@Override
    public Location with( Path newPath ) {
        if (newPath == null && path == null) return this;
        if (path != null && (path.equals(newPath))) return this;
        
        return Location.create(newPath, idProperty);
    }	
	
    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
	@Override
    public Location with( UUID uuid ) {
        if (uuid == null) return this;
        
        // A new property with same name overrides the old property with the same name
        if (idProperty.getName().equals(DnaLexicon.UUID)) return Location.create(path, uuid);
        
        List<Property> newIdProperties = new ArrayList<Property>(2);
        newIdProperties.add(idProperty);
        newIdProperties.add(new BasicSingleValueProperty(DnaLexicon.UUID, uuid));
        
        return Location.create(path, newIdProperties);
    }	
}
