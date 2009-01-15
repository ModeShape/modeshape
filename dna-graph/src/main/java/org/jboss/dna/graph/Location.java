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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;

/**
 * The location of a node, as specified by either its path, UUID, and/or identification properties.
 * 
 * @author Randall Hauch
 */
@Immutable
public class Location implements Iterable<Property> {

    private static final Iterator<Property> NO_ID_PROPERTIES_ITERATOR = new Iterator<Property>() {
        public boolean hasNext() {
            return false;
        }

        public Property next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    private final Path path;
    private final List<Property> idProperties;

    /**
     * Create a location defined by a path.
     * 
     * @param path the path
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public Location( Path path ) {
        CheckArg.isNotNull(path, "path");
        this.path = path;
        this.idProperties = null;
    }

    /**
     * Create a location defined by a UUID.
     * 
     * @param uuid the UUID
     * @throws IllegalArgumentException if <code>uuid</code> is null
     */
    public Location( UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        this.path = null;
        Property idProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by a path and an UUID.
     * 
     * @param path the path
     * @param uuid the UUID, or null if there is no UUID
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public Location( Path path,
                     UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        this.path = path;
        if (uuid != null) {
            Property idProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
            this.idProperties = Collections.singletonList(idProperty);
        } else {
            this.idProperties = null;
        }
    }

    /**
     * Create a location defined by a path and a single identification property.
     * 
     * @param path the path
     * @param idProperty the identification property
     * @throws IllegalArgumentException if <code>path</code> or <code>idProperty</code> is null
     */
    public Location( Path path,
                     Property idProperty ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(idProperty, "idProperty");
        this.path = path;
        this.idProperties = idProperty != null ? Collections.singletonList(idProperty) : null;
    }

    /**
     * Create a location defined by a path and multiple identification properties.
     * 
     * @param path the path
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Path path,
                     Property firstIdProperty,
                     Property... remainingIdProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        this.path = path;
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            idProperties.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idProperties);
    }

    /**
     * Create a location defined by a path and an iterator over identification properties.
     * 
     * @param path the path
     * @param idProperties the iterator over the identification properties
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Path path,
                     Iterable<Property> idProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(idProperties, "idProperties");
        this.path = path;
        List<Property> idPropertiesList = new ArrayList<Property>();
        for (Property property : idProperties) {
            idPropertiesList.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idPropertiesList);
    }

    /**
     * Create a location defined by a single identification property.
     * 
     * @param idProperty the identification property
     * @throws IllegalArgumentException if <code>idProperty</code> is null
     */
    public Location( Property idProperty ) {
        CheckArg.isNotNull(idProperty, "idProperty");
        this.path = null;
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by multiple identification properties.
     * 
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Property firstIdProperty,
                     Property... remainingIdProperties ) {
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        this.path = null;
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            idProperties.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idProperties);
    }

    /**
     * Create a location defined by a path and an iterator over identification properties.
     * 
     * @param idProperties the iterator over the identification properties
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Iterable<Property> idProperties ) {
        CheckArg.isNotNull(idProperties, "idProperties");
        this.path = null;
        List<Property> idPropertiesList = new ArrayList<Property>();
        for (Property property : idProperties) {
            idPropertiesList.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idPropertiesList);
    }

    /**
     * Create a location defined by multiple identification properties.
     * 
     * @param idProperties the identification properties
     * @throws IllegalArgumentException if <code>idProperties</code> is null or empty
     */
    public Location( List<Property> idProperties ) {
        CheckArg.isNotEmpty(idProperties, "idProperties");
        this.path = null;
        this.idProperties = idProperties;
    }

    /**
     * Create a location defined by a path and multiple identification properties.
     * 
     * @param path the path
     * @param idProperties the identification properties
     * @throws IllegalArgumentException if <code>path</code> is null, or if <code>idProperties</code> is empty
     */
    protected Location( Path path,
                        List<Property> idProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotEmpty(idProperties, "idProperties");
        this.path = path;
        this.idProperties = idProperties;
    }

    /**
     * Create a location from another but adding the supplied identification property. The new identification property will
     * replace any existing identification property with the same name on the original.
     * 
     * @param original the original location
     * @param newIdProperty the new identification property
     * @throws IllegalArgumentException if <code>original</code> is null
     */
    protected Location( Location original,
                        Property newIdProperty ) {
        CheckArg.isNotNull(original, "original");
        this.path = original.getPath();
        if (original.hasIdProperties()) {
            List<Property> originalIdProperties = original.getIdProperties();
            if (newIdProperty == null) {
                this.idProperties = original.idProperties;
            } else {
                List<Property> idProperties = new ArrayList<Property>(originalIdProperties.size() + 1);
                for (Property property : originalIdProperties) {
                    if (!newIdProperty.getName().equals(property.getName())) idProperties.add(property);
                }
                idProperties.add(newIdProperty);
                this.idProperties = Collections.unmodifiableList(idProperties);
            }
        } else {
            this.idProperties = Collections.singletonList(newIdProperty);
        }
    }

    /**
     * Create a location from another but adding the supplied identification property. The new identification property will
     * replace any existing identification property with the same name on the original.
     * 
     * @param original the original location
     * @param newPath the new path for the location
     * @throws IllegalArgumentException if <code>original</code> is null
     */
    protected Location( Location original,
                        Path newPath ) {
        CheckArg.isNotNull(original, "original");
        this.path = newPath != null ? newPath : original.getPath();
        this.idProperties = original.idProperties;
    }

    /**
     * Get the path that (at least in part) defines this location.
     * 
     * @return the path, or null if this location is not defined with a path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Return whether this location is defined (at least in part) by a path.
     * 
     * @return true if a {@link #getPath() path} helps define this location
     */
    public boolean hasPath() {
        return path != null;
    }

    /**
     * Get the identification properties that (at least in part) define this location.
     * 
     * @return the identification properties, or null if this location is not defined with identification properties
     */
    public List<Property> getIdProperties() {
        return idProperties;
    }

    /**
     * Return whether this location is defined (at least in part) with identification properties.
     * 
     * @return true if a {@link #getIdProperties() identification properties} help define this location
     */
    public boolean hasIdProperties() {
        return idProperties != null && idProperties.size() != 0;
    }

    /**
     * Get the identification property with the supplied name, if there is such a property.
     * 
     * @param name the name of the identification property
     * @return the identification property with the supplied name, or null if there is no such property (or if there
     *         {@link #hasIdProperties() are no identification properties}
     */
    public Property getIdProperty( Name name ) {
        CheckArg.isNotNull(name, "name");
        if (idProperties != null) {
            for (Property property : idProperties) {
                if (property.getName().equals(name)) return property;
            }
        }
        return null;
    }

    /**
     * Get the first UUID that is in one of the {@link #getIdProperties() identification properties}.
     * 
     * @return the UUID for this location, or null if there is no such identification property
     */
    public UUID getUuid() {
        Property property = getIdProperty(DnaLexicon.UUID);
        if (property != null && !property.isEmpty()) {
            Object value = property.getFirstValue();
            if (value instanceof UUID) return (UUID)value;
        }
        return null;
    }

    /**
     * Compare this location to the supplied location, and determine whether the two locations represent the same logical
     * location. One location is considered the same as another location when one location is a superset of the other. For
     * example, consider the following locations:
     * <ul>
     * <li>location A is defined with a "<code>/x/y</code>" path</li>
     * <li>location B is defined with an identification property {id=3}</li>
     * <li>location C is defined with a "<code>/x/y/z</code>"</li>
     * <li>location D is defined with a "<code>/x/y/z</code>" path and an identification property {id=3}</li>
     * </ul>
     * Locations C and D would be considered the same, and B and D would also be considered the same. None of the other
     * combinations would be considered the same.
     * <p>
     * Note that passing a null location as a parameter will always return false.
     * </p>
     * 
     * @param other the other location to compare
     * @return true if the two locations represent the same location, or false otherwise
     */
    public boolean isSame( Location other ) {
        return isSame(other, true);
    }

    /**
     * Compare this location to the supplied location, and determine whether the two locations represent the same logical
     * location. One location is considered the same as another location when one location is a superset of the other. For
     * example, consider the following locations:
     * <ul>
     * <li>location A is defined with a "<code>/x/y</code>" path</li>
     * <li>location B is defined with an identification property {id=3}</li>
     * <li>location C is defined with a "<code>/x/y/z</code>"</li>
     * <li>location D is defined with a "<code>/x/y/z</code>" path and an identification property {id=3}</li>
     * </ul>
     * Locations C and D would be considered the same, and B and D would also be considered the same. None of the other
     * combinations would be considered the same.
     * <p>
     * Note that passing a null location as a parameter will always return false.
     * </p>
     * 
     * @param other the other location to compare
     * @param requireSameNameSiblingIndexes true if the paths must have equivalent {@link Path.Segment#getIndex()
     *        same-name-sibling indexes}, or false if the same-name-siblings may be different
     * @return true if the two locations represent the same location, or false otherwise
     */
    public boolean isSame( Location other,
                           boolean requireSameNameSiblingIndexes ) {
        if (other != null) {
            if (this.hasPath() && other.hasPath()) {
                // Paths on both, so the paths MUST match
                if (requireSameNameSiblingIndexes) {
                    if (!this.getPath().equals(other.getPath())) return false;
                } else {
                    Path thisPath = this.getPath();
                    Path thatPath = other.getPath();
                    if (thisPath.isRoot()) return thatPath.isRoot();
                    if (thatPath.isRoot()) return thisPath.isRoot();
                    // The parents must match ...
                    if (!thisPath.hasSameAncestor(thatPath)) return false;
                    // And the names of the last segments must match ...
                    if (!thisPath.getLastSegment().getName().equals(thatPath.getLastSegment().getName())) return false;
                }

                // And the identification properties must match only if they exist on both
                if (this.hasIdProperties() && other.hasIdProperties()) {
                    return this.getIdProperties().containsAll(other.getIdProperties());
                }
                return true;
            }
            // Path only in one, so the identification properties MUST match
            if (!other.hasIdProperties()) return false;
            return this.getIdProperties().containsAll(other.getIdProperties());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Property> iterator() {
        return idProperties != null ? idProperties.iterator() : NO_ID_PROPERTIES_ITERATOR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(path, idProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof Location) {
            Location that = (Location)obj;
            if (this.hasPath()) {
                if (!this.getPath().equals(that.getPath())) return false;
            } else {
                if (that.hasPath()) return false;
            }
            if (this.hasIdProperties()) {
                if (!this.getIdProperties().equals(that.getIdProperties())) return false;
            } else {
                if (that.hasIdProperties()) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Get the string form of the location.
     * 
     * @return the string
     * @see #getString(TextEncoder)
     * @see #getString(NamespaceRegistry)
     * @see #getString(NamespaceRegistry, TextEncoder)
     * @see #getString(NamespaceRegistry, TextEncoder, TextEncoder)
     */
    public String getString() {
        return getString(null, null, null);
    }

    /**
     * Get the encoded string form of the location, using the supplied encoder to encode characters in each of the location's path
     * and properties.
     * 
     * @param encoder the encoder to use, or null if the default encoder should be used
     * @return the encoded string
     * @see #getString()
     * @see #getString(NamespaceRegistry)
     * @see #getString(NamespaceRegistry, TextEncoder)
     * @see #getString(NamespaceRegistry, TextEncoder, TextEncoder)
     */
    public String getString( TextEncoder encoder ) {
        return getString(null, encoder, null);
    }

    /**
     * Get the encoded string form of the location, using the supplied encoder to encode characters in each of the location's path
     * and properties.
     * 
     * @param namespaceRegistry the namespace registry to use for getting the string form of the path and properties, or null if
     *        no namespace registry should be used
     * @return the encoded string
     * @see #getString()
     * @see #getString(TextEncoder)
     * @see #getString(NamespaceRegistry, TextEncoder)
     * @see #getString(NamespaceRegistry, TextEncoder, TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return getString(namespaceRegistry, null, null);
    }

    /**
     * Get the encoded string form of the location, using the supplied encoder to encode characters in each of the location's path
     * and properties.
     * 
     * @param namespaceRegistry the namespace registry to use for getting the string form of the path and properties, or null if
     *        no namespace registry should be used
     * @param encoder the encoder to use, or null if the default encoder should be used
     * @return the encoded string
     * @see #getString()
     * @see #getString(TextEncoder)
     * @see #getString(NamespaceRegistry)
     * @see #getString(NamespaceRegistry, TextEncoder, TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return getString(namespaceRegistry, encoder, null);
    }

    /**
     * Get the encoded string form of the location, using the supplied encoder to encode characters in each of the location's path
     * and properties.
     * 
     * @param namespaceRegistry the namespace registry to use for getting the string form of the path and properties, or null if
     *        no namespace registry should be used
     * @param encoder the encoder to use, or null if the default encoder should be used
     * @param delimiterEncoder the encoder to use for encoding the delimiters in paths, names, and properties, or null if the
     *        standard delimiters should be used
     * @return the encoded string
     * @see #getString()
     * @see #getString(TextEncoder)
     * @see #getString(NamespaceRegistry)
     * @see #getString(NamespaceRegistry, TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        if (this.hasPath()) {
            sb.append(this.getPath().getString(namespaceRegistry, encoder, delimiterEncoder));
            if (this.hasIdProperties()) sb.append(" && ");
        }
        if (this.hasIdProperties()) {
            sb.append("[");
            boolean first = true;
            for (Property idProperty : this.getIdProperties()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(idProperty.getString(namespaceRegistry, encoder, delimiterEncoder));
            }
            sb.append("]");
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        if (this.hasPath()) {
            sb.append(this.getPath());
            if (this.hasIdProperties()) sb.append(" && ");
        }
        if (this.hasIdProperties()) {
            sb.append("[");
            boolean first = true;
            for (Property idProperty : this.getIdProperties()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(idProperty);
            }
            sb.append("]");
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Create a copy of this location that adds the supplied identification property. The new identification property will replace
     * any existing identification property with the same name on the original.
     * 
     * @param newIdProperty the new identification property, which may be null
     * @return the new location, or this location if the new identification property is null or empty
     */
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;
        return new Location(this, newIdProperty);
    }

    /**
     * Create a copy of this location that uses the supplied path.
     * 
     * @param newPath the new path for the location
     * @return the new location, or this location if the path is equal to this location's path
     */
    public Location with( Path newPath ) {
        if (newPath == null) return this;
        if (!this.path.equals(newPath)) return new Location(this, newPath);
        return this;
    }

    /**
     * Create a copy of this location that adds the supplied UUID as an identification property. The new identification property
     * will replace any existing identification property with the same name on the original.
     * 
     * @param uuid the new UUID, which may be null
     * @return the new location, or this location if the new identification property is null or empty
     */
    public Location with( UUID uuid ) {
        if (uuid == null) return this;
        Property newProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
        if (this.hasIdProperties()) {
            Property existing = this.getIdProperty(DnaLexicon.UUID);
            if (existing != null && existing.equals(newProperty)) return this;
        }
        return new Location(this, newProperty);
    }

}
