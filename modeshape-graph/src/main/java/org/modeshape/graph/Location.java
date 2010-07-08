/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * The location of a node, as specified by either its path, UUID, and/or identification properties. Hash codes are not implemented
 * in this base class to allow immutable subclasses to calculate and cache the hash code during object construction.
 */
@Immutable
public abstract class Location implements Iterable<Property>, Comparable<Location>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Comparator<Location> COMPARATOR = new Comparator<Location>() {
        /**
         * {@inheritDoc}
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare( Location o1,
                            Location o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };

    /**
     * Get a {@link Comparator} that can be used to compare two Location objects. Note that Location implements {@link Comparable}
     * .
     * 
     * @return the comparator; never null
     */
    public static final Comparator<Location> comparator() {
        return COMPARATOR;
    }

    /**
     * Simple shared iterator instance that is used when there are no properties.
     */
    protected static final Iterator<Property> NO_ID_PROPERTIES_ITERATOR = new Iterator<Property>() {
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

    /**
     * Create a location defined by a path.
     * 
     * @param path the path
     * @return a new <code>Location</code> with the given path and no identification properties
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public static Location create( Path path ) {
        CheckArg.isNotNull(path, "path");
        UUID id = identifierFor(path);
        if (id != null) return new LocationWithUuid(id);
        return new LocationWithPath(path);
    }

    protected static UUID identifierFor( Path identifierPath ) {
        if (!identifierPath.isIdentifier()) return null;
        // Get the identifier segment ...
        Segment segment = identifierPath.getLastSegment();
        assert segment.isIdentifier();
        String id = segment.getName().getLocalName();
        try {
            // The local part of the segment's name should be the identifier, though it may not be a UUID ...
            return UUID.fromString(id);
        } catch (IllegalArgumentException err) {
            String pathStr = "[" + id + "]";
            throw new IllegalArgumentException(GraphI18n.identifierPathContainedUnsupportedIdentifierFormat.text(pathStr));
        }
    }

    /**
     * Create a location defined by a UUID.
     * 
     * @param uuid the UUID
     * @return a new <code>Location</code> with no path and a single identification property with the name
     *         {@link ModeShapeLexicon#UUID} and the given <code>uuid</code> for a value.
     * @throws IllegalArgumentException if <code>uuid</code> is null
     */
    public static Location create( UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        return new LocationWithUuid(uuid);
    }

    /**
     * Create a location defined by a path and an UUID.
     * 
     * @param path the path
     * @param uuid the UUID, or null if there is no UUID
     * @return a new <code>Location</code> with the given path (if any) and a single identification property with the name
     *         {@link ModeShapeLexicon#UUID} and the given <code>uuid</code> (if it is present) for a value.
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public static Location create( Path path,
                                   UUID uuid ) {
        if (path == null) return create(uuid);
        if (uuid == null) return create(path);
        UUID id = identifierFor(path);
        if (id != null) {
            if (!id.equals(uuid)) {
                String pathStr = "[" + id + "]";
                throw new IllegalArgumentException(GraphI18n.identifierPathDoesNotMatchSuppliedUuid.text(pathStr, uuid));
            }
            return new LocationWithUuid(id);
        }
        return new LocationWithPathAndUuid(path, uuid);
    }

    /**
     * Create a location defined by a path and a single identification property.
     * 
     * @param path the path
     * @param idProperty the identification property
     * @return a new <code>Location</code> with the given path and identification property (if it is present).
     * @throws IllegalArgumentException if <code>path</code> or <code>idProperty</code> is null
     */
    public static Location create( Path path,
                                   Property idProperty ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(idProperty, "idProperty");
        if (ModeShapeLexicon.UUID.equals(idProperty.getName()) && idProperty.isSingle()) {
            Object uuid = idProperty.getFirstValue();
            assert uuid instanceof UUID;
            UUID id = identifierFor(path);
            if (id != null) {
                if (!id.equals(uuid)) {
                    String pathStr = "[" + id + "]";
                    throw new IllegalArgumentException(GraphI18n.identifierPathDoesNotMatchSuppliedUuid.text(pathStr, uuid));
                }
                return new LocationWithUuid(id);
            }
            return new LocationWithPathAndUuid(path, (UUID)uuid);
        }
        return new LocationWithPathAndProperty(path, idProperty);
    }

    /**
     * Create a location defined by a path and multiple identification properties.
     * 
     * @param path the path
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @return a new <code>Location</code> with the given path and identification properties.
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public static Location create( Path path,
                                   Property firstIdProperty,
                                   Property... remainingIdProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        Set<Name> names = new HashSet<Name>();
        names.add(firstIdProperty.getName());
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            if (property == null) continue;
            if (names.add(property.getName())) idProperties.add(property);
        }
        if (idProperties.isEmpty()) return new LocationWithPath(path);
        if (idProperties.size() == 1) {
            Property property = idProperties.get(0);
            if (property.isSingle()
                && (property.getName().equals(JcrLexicon.UUID) || property.getName().equals(ModeShapeLexicon.UUID))) {
                Object value = property.getFirstValue();
                if (value instanceof UUID) {
                    return new LocationWithPathAndUuid(path, (UUID)value);
                }
            }
            return new LocationWithPathAndProperty(path, idProperties.get(0));
        }
        return new LocationWithPathAndProperties(path, idProperties);
    }

    /**
     * Create a location defined by a path and an iterator over identification properties.
     * 
     * @param path the path
     * @param idProperties the iterator over the identification properties
     * @return a new <code>Location</code> with the given path and identification properties
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public static Location create( Path path,
                                   Iterable<Property> idProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(idProperties, "idProperties");
        List<Property> idPropertiesList = new ArrayList<Property>();
        Set<Name> names = new HashSet<Name>();
        for (Property property : idProperties) {
            if (property == null) continue;
            if (names.add(property.getName())) idPropertiesList.add(property);
        }
        if (idPropertiesList.isEmpty()) return new LocationWithPath(path);
        if (idPropertiesList.size() == 1) {
            Property property = idPropertiesList.get(0);
            if (property.isSingle()
                && (property.getName().equals(JcrLexicon.UUID) || property.getName().equals(ModeShapeLexicon.UUID))) {
                Object value = property.getFirstValue();
                if (value instanceof UUID) {
                    return new LocationWithPathAndUuid(path, (UUID)value);
                }
            }
            return new LocationWithPathAndProperty(path, idPropertiesList.get(0));
        }
        return new LocationWithPathAndProperties(path, idPropertiesList);
    }

    /**
     * Create a location defined by a single identification property.
     * 
     * @param idProperty the identification property
     * @return a new <code>Location</code> with no path and the given identification property.
     * @throws IllegalArgumentException if <code>idProperty</code> is null
     */
    public static Location create( Property idProperty ) {
        CheckArg.isNotNull(idProperty, "idProperty");
        if (ModeShapeLexicon.UUID.equals(idProperty.getName()) && idProperty.isSingle()) {
            Object uuid = idProperty.getFirstValue();
            assert uuid instanceof UUID;
            return new LocationWithUuid((UUID)uuid);
        }
        return new LocationWithProperty(idProperty);
    }

    /**
     * Create a location defined by multiple identification properties.
     * 
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @return a new <code>Location</code> with no path and the given and identification properties.
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public static Location create( Property firstIdProperty,
                                   Property... remainingIdProperties ) {
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        if (remainingIdProperties.length == 0) return create(firstIdProperty);
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        Set<Name> names = new HashSet<Name>();
        names.add(firstIdProperty.getName());
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            if (names.add(property.getName())) idProperties.add(property);
        }
        return new LocationWithProperties(idProperties);
    }

    /**
     * Create a location defined by a path and an iterator over identification properties.
     * 
     * @param idProperties the iterator over the identification properties
     * @return a new <code>Location</code> with no path and the given identification properties.
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public static Location create( Iterable<Property> idProperties ) {
        CheckArg.isNotNull(idProperties, "idProperties");
        List<Property> idPropertiesList = new ArrayList<Property>();
        Set<Name> names = new HashSet<Name>();
        for (Property property : idProperties) {
            if (names.add(property.getName())) idPropertiesList.add(property);
        }
        return create(idPropertiesList);
    }

    /**
     * Create a location defined by multiple identification properties. This method does not check whether the identification
     * properties are duplicated.
     * 
     * @param idProperties the identification properties
     * @return a new <code>Location</code> with no path and the given identification properties.
     * @throws IllegalArgumentException if <code>idProperties</code> is null or empty
     */
    public static Location create( List<Property> idProperties ) {
        CheckArg.isNotEmpty(idProperties, "idProperties");
        return new LocationWithProperties(idProperties);
    }

    /**
     * Get the path that (at least in part) defines this location.
     * 
     * @return the path, or null if this location is not defined with a path
     */
    public abstract Path getPath();

    /**
     * Return whether this location is defined (at least in part) by a path.
     * 
     * @return true if a {@link #getPath() path} helps define this location
     */
    public boolean hasPath() {
        return getPath() != null;
    }

    /**
     * Get the identification properties that (at least in part) define this location.
     * 
     * @return the identification properties, or null if this location is not defined with identification properties
     */
    public abstract List<Property> getIdProperties();

    /**
     * Return whether this location is defined (at least in part) with identification properties.
     * 
     * @return true if a {@link #getIdProperties() identification properties} help define this location
     */
    public boolean hasIdProperties() {
        return getIdProperties() != null && getIdProperties().size() != 0;
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
        if (getIdProperties() != null) {
            for (Property property : getIdProperties()) {
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
        Property property = getIdProperty(ModeShapeLexicon.UUID);
        if (property != null && !property.isEmpty()) {
            Object value = property.getFirstValue();
            if (value instanceof UUID) return (UUID)value;
        }
        return null;
    }

    /**
     * Determine whether this location has the same {@link #getPath() path} and {@link #getIdProperties() identification
     * properties}: if one location has a path, then both must have the same path; likewise, if one location has ID properties,
     * then both must have the same ID properties.
     * <p>
     * This is different than the behavior of {@link #equals(Object)}, which attempts to determine whether two locations are
     * <i>equivalent</i>. Two location objects are equivalent if they share the same path and/or ID properties: if both locations
     * have a path, they must have the same path; if both locations have ID properties, these properties must match.
     * </p>
     * 
     * @param that the other location to be compared
     * @return true if they are the same, or false otherwise (or if the supplied location is null)
     * @see #equals(Object)
     */
    public boolean isSame( Location that ) {
        if (that == null) return false;
        if (this.hasPath()) {
            if (!this.getPath().equals(that.getPath())) return false;
        } else if (that.hasPath()) {
            // this has no path, but that does
            return false;
        }
        if (this.hasIdProperties()) {
            if (that.hasIdProperties()) return this.getIdProperties().equals(that.getIdProperties());
            return false;
        }
        return (!that.hasIdProperties());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Property> iterator() {
        return getIdProperties() != null ? getIdProperties().iterator() : NO_ID_PROPERTIES_ITERATOR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(getPath(), getIdProperties());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Two location objects are equal (or equivalent) if they share the same path and/or ID properties: if both locations have a
     * path, they must have the same path; if both locations have ID properties, these properties must match.
     * </p>
     * <p>
     * To determine whether two location objects represent the same location, use {@link #isSame(Location)}: if one location has a
     * path, then both must have the same path; likewise, if one location has ID properties, then both must have the same ID
     * properties.
     * </p>
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @see #isSame(Location)
     */
    @Override
    public boolean equals( Object obj ) {
        return equals(obj, true);
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
     * @param obj the other location to compare
     * @param requireSameNameSiblingIndexes true if the paths must have equivalent {@link Path.Segment#getIndex()
     *        same-name-sibling indexes}, or false if the same-name-siblings may be different
     * @return true if the two locations represent the same location, or false otherwise
     */
    public boolean equals( Object obj,
                           boolean requireSameNameSiblingIndexes ) {
        if (obj instanceof Location) {
            Location that = (Location)obj;

            // if both have same path they are equal
            if (requireSameNameSiblingIndexes) {
                if (this.hasPath() && that.hasPath()) return (this.getPath().equals(that.getPath()));
            } else {
                Path thisPath = this.getPath();
                Path thatPath = that.getPath();
                if (thisPath.isRoot()) return thatPath.isRoot();
                if (thatPath.isRoot()) return thisPath.isRoot();
                // The parents must match ...
                if (!thisPath.hasSameAncestor(thatPath)) return false;
                // And the names of the last segments must match ...
                if (!thisPath.getLastSegment().getName().equals(thatPath.getLastSegment().getName())) return false;
            }

            // one or both is/are missing path so check properties instead
            if (this.hasIdProperties()) return (this.getIdProperties().equals(that.getIdProperties()));
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Location that ) {
        if (this == that) return 0;
        if (this.hasPath() && that.hasPath()) {
            return this.getPath().compareTo(that.getPath());
        }
        UUID thisUuid = this.getUuid();
        UUID thatUuid = that.getUuid();
        if (thisUuid != null && thatUuid != null) {
            return thisUuid.compareTo(thatUuid);
        }
        return this.hashCode() - that.hashCode();
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
        boolean hasPath = this.hasPath();
        if (hasPath) {
            sb.append(this.getPath().getString(namespaceRegistry, encoder, delimiterEncoder));
        }
        if (this.hasIdProperties()) {
            if (hasPath) sb.append(" && ");
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
        boolean hasPath = this.hasPath();
        boolean hasProps = this.hasIdProperties();
        if (hasPath) {
            if (hasProps) {
                sb.append("<");
            }
            sb.append(this.getPath());
        }
        if (hasProps) {
            if (hasPath) sb.append(" && ");
            sb.append("[");
            boolean first = true;
            for (Property idProperty : this.getIdProperties()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(idProperty);
            }
            sb.append("]");
            if (hasPath) {
                sb.append(">");
            }
        }
        return sb.toString();
    }

    /**
     * Create a copy of this location that adds the supplied identification property. The new identification property will replace
     * any existing identification property with the same name on the original.
     * 
     * @param newIdProperty the new identification property, which may be null
     * @return the new location, or this location if the new identification property is null or empty
     */
    public abstract Location with( Property newIdProperty );

    /**
     * Create a copy of this location that uses the supplied path.
     * 
     * @param newPath the new path for the location
     * @return the new location, or this location if the path is equal to this location's path
     */
    public abstract Location with( Path newPath );

    /**
     * Create a copy of this location that adds the supplied UUID as an identification property. The new identification property
     * will replace any existing identification property with the same name on the original.
     * 
     * @param uuid the new UUID, which may be null
     * @return the new location, or this location if the new identification property is null or empty
     */
    public abstract Location with( UUID uuid );

}
