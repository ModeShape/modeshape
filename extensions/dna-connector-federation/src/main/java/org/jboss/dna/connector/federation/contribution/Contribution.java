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
package org.jboss.dna.connector.federation.contribution;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.JodaDateTime;

/**
 * The contribution of a source to the information for a single federated node. Users of this interface should treat contributions
 * as generally being immutable, since some implementation will be immutable and will return immutable {@link #getProperties()
 * properties} and {@link #getChildren() children} containers. Thus, rather than make changes to an existing contribution, a new
 * contribution is created to replace the previous contribution.
 * 
 * @author Randall Hauch
 */
@Immutable
public abstract class Contribution implements Serializable {

    /**
     * Create an empty contribution from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @return the contribution
     */
    public static Contribution create( String sourceName,
                                       DateTime expirationTime ) {
        return new EmptyContribution(sourceName, expirationTime);
    }

    /**
     * Create a contribution of a single property from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the location in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param property the property from the source; may not be null
     * @return the contribution
     */
    public static Contribution create( String sourceName,
                                       Location locationInSource,
                                       DateTime expirationTime,
                                       Property property ) {
        if (property == null) {
            return new EmptyContribution(sourceName, expirationTime);
        }
        return new OnePropertyContribution(sourceName, locationInSource, expirationTime, property);
    }

    /**
     * Create a contribution of a single child from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param child the child from the source; may not be null or empty
     * @return the contribution
     */
    public static Contribution create( String sourceName,
                                       Location locationInSource,
                                       DateTime expirationTime,
                                       Location child ) {
        if (child == null) {
            return new EmptyContribution(sourceName, expirationTime);
        }
        return new OneChildContribution(sourceName, locationInSource, expirationTime, child);
    }

    /**
     * Create a contribution of a single child from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param child1 the first child from the source; may not be null or empty
     * @param child2 the second child from the source; may not be null or empty
     * @return the contribution
     */
    public static Contribution create( String sourceName,
                                       Location locationInSource,
                                       DateTime expirationTime,
                                       Location child1,
                                       Location child2 ) {
        if (child1 != null) {
            if (child2 != null) {
                return new TwoChildContribution(sourceName, locationInSource, expirationTime, child1, child2);
            }
            return new OneChildContribution(sourceName, locationInSource, expirationTime, child1);
        }
        if (child2 != null) {
            return new OneChildContribution(sourceName, locationInSource, expirationTime, child2);
        }
        return new EmptyContribution(sourceName, expirationTime);
    }

    /**
     * Create a contribution of the supplied properties and children from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param properties the properties from the source; may not be null
     * @param children the children from the source; may not be null or empty
     * @return the contribution
     */
    public static Contribution create( String sourceName,
                                       Location locationInSource,
                                       DateTime expirationTime,
                                       Collection<Property> properties,
                                       List<Location> children ) {
        if (properties == null || properties.isEmpty()) {
            // There are no properties ...
            if (children == null || children.isEmpty()) {
                return new EmptyContribution(sourceName, expirationTime);
            }
            if (children.size() == 1) {
                return new OneChildContribution(sourceName, locationInSource, expirationTime, children.iterator().next());
            }
            if (children.size() == 2) {
                Iterator<Location> iter = children.iterator();
                return new TwoChildContribution(sourceName, locationInSource, expirationTime, iter.next(), iter.next());
            }
            return new MultiChildContribution(sourceName, locationInSource, expirationTime, children);
        }
        // There are some properties ...
        if (children == null || children.isEmpty()) {
            // There are no children ...
            if (properties.size() == 1) {
                return new OnePropertyContribution(sourceName, locationInSource, expirationTime, properties.iterator().next());
            }
            if (properties.size() == 2) {
                Iterator<Property> iter = properties.iterator();
                return new TwoPropertyContribution(sourceName, locationInSource, expirationTime, iter.next(), iter.next());
            }
            if (properties.size() == 3) {
                Iterator<Property> iter = properties.iterator();
                return new ThreePropertyContribution(sourceName, locationInSource, expirationTime, iter.next(), iter.next(),
                                                     iter.next());
            }
            return new MultiPropertyContribution(sourceName, locationInSource, expirationTime, properties);
        }
        // There are some properties AND some children ...
        return new NodeContribution(sourceName, locationInSource, expirationTime, properties, children);
    }

    /**
     * Create a placeholder contribution of a single child from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param child the child from the source; may not be null or empty
     * @return the contribution
     */
    public static Contribution createPlaceholder( String sourceName,
                                                  Location locationInSource,
                                                  DateTime expirationTime,
                                                  Location child ) {
        if (child == null) {
            return new EmptyContribution(sourceName, expirationTime);
        }
        return new PlaceholderContribution(sourceName, locationInSource, expirationTime, Collections.singletonList(child));
    }

    /**
     * Create a placeholder contribution of the supplied properties and children from the named source.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param children the children from the source; may not be null or empty
     * @return the contribution
     */
    public static Contribution createPlaceholder( String sourceName,
                                                  Location locationInSource,
                                                  DateTime expirationTime,
                                                  List<Location> children ) {
        if (children == null || children.isEmpty()) {
            return new EmptyContribution(sourceName, expirationTime);
        }
        return new PlaceholderContribution(sourceName, locationInSource, expirationTime, children);
    }

    /**
     * This is the first version of this class. See the documentation of BasicMergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    protected static final Iterator<Property> EMPTY_PROPERTY_ITERATOR = new EmptyIterator<Property>();
    protected static final Iterator<Location> EMPTY_CHILDREN_ITERATOR = new EmptyIterator<Location>();

    private final String sourceName;
    private DateTime expirationTimeInUtc;

    /**
     * Create a contribution for the source with the supplied name and path.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     */
    protected Contribution( String sourceName,
                            DateTime expirationTime ) {
        assert sourceName != null && sourceName.trim().length() != 0;
        assert expirationTime == null || expirationTime.equals(expirationTime.toUtcTimeZone());
        this.sourceName = sourceName;
        this.expirationTimeInUtc = expirationTime;
    }

    /**
     * Get the name of the source that made this contribution.
     * 
     * @return the name of the contributing source
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * Get the source-specific location of this information.
     * 
     * @return the location as known to the source, or null for {@link EmptyContribution}
     */
    public abstract Location getLocationInSource();

    /**
     * Determine whether this contribution has expired given the supplied current time.
     * 
     * @param utcTime the current time expressed in UTC; may not be null
     * @return true if at least one contribution has expired, or false otherwise
     */
    public boolean isExpired( DateTime utcTime ) {
        assert utcTime != null;
        assert utcTime.toUtcTimeZone().equals(utcTime); // check that it is passed UTC time
        return !expirationTimeInUtc.isAfter(utcTime);
    }

    /**
     * Get the expiration time, already in UTC.
     * 
     * @return the expiration time in UTC
     */
    public DateTime getExpirationTimeInUtc() {
        return this.expirationTimeInUtc;
    }

    /**
     * Get the properties that are in this contribution. This resulting iterator does not support {@link Iterator#remove()
     * removal}.
     * 
     * @return the properties; never null
     */
    public Iterator<Property> getProperties() {
        return EMPTY_PROPERTY_ITERATOR;
    }

    /**
     * Get the number of properties that are in this contribution.
     * 
     * @return the number of properties
     */
    public int getPropertyCount() {
        return 0;
    }

    /**
     * Get the contributed property with the supplied name.
     * 
     * @param name the name of the property
     * @return the contributed property that matches the name, or null if no such property is in the contribution
     */
    public Property getProperty( Name name ) {
        return null;
    }

    /**
     * Get the children that make up this contribution. This resulting iterator does not support {@link Iterator#remove() removal}
     * .
     * 
     * @return the children; never null
     */
    public Iterator<Location> getChildren() {
        return EMPTY_CHILDREN_ITERATOR;
    }

    /**
     * Get the number of children that make up this contribution.
     * 
     * @return the number of children
     */
    public int getChildrenCount() {
        return 0;
    }

    /**
     * Return whether this contribution is an empty contribution.
     * 
     * @return true if this contribution is empty, or false otherwise
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * Determine whether this contribution is considered a placeholder necessary solely because the same source has contributions
     * at or below the children.
     * 
     * @return true if a placeholder contribution, or false otherwise
     */
    public boolean isPlaceholder() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the hash code of the {@link #getSourceName() source name}, and is compatible with the
     * implementation of {@link #equals(Object)}.
     * </p>
     */
    @Override
    public int hashCode() {
        return this.sourceName.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Contribution from \"");
        sb.append(getSourceName());
        if (isExpired(new JodaDateTime().toUtcTimeZone())) {
            sb.append("\": expired ");
        } else {
            sb.append("\": expires ");
        }
        sb.append(getExpirationTimeInUtc().getString());
        if (getPropertyCount() != 0) {
            sb.append(" { ");
            boolean first = true;
            Iterator<Property> propIter = getProperties();
            while (propIter.hasNext()) {
                if (!first) sb.append(", ");
                else first = false;
                sb.append(propIter.next());
            }
            sb.append(" }");
        }
        if (getChildrenCount() != 0) {
            sb.append("< ");
            boolean first = true;
            Iterator<Location> childIter = getChildren();
            while (childIter.hasNext()) {
                if (!first) sb.append(", ");
                else first = false;
                sb.append(childIter.next());
            }
            sb.append(" >");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation only compares the {@link #getSourceName() source name}.
     * </p>
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Contribution) {
            Contribution that = (Contribution)obj;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            return true;
        }
        return false;
    }

    protected static class ImmutableIterator<T> implements Iterator<T> {
        private final Iterator<T> iter;

        protected ImmutableIterator( Iterator<T> iter ) {
            this.iter = iter;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            return iter.next();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected static class EmptyIterator<T> implements Iterator<T> {

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    protected static class OneValueIterator<T> implements Iterator<T> {

        private final T value;
        private boolean next = true;

        protected OneValueIterator( T value ) {
            assert value != null;
            this.value = value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return next;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            if (next) {
                next = false;
                return value;
            }
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    protected static class TwoValueIterator<T> implements Iterator<T> {

        private final T value1;
        private final T value2;
        private int next = 2;

        protected TwoValueIterator( T value1,
                                    T value2 ) {
            this.value1 = value1;
            this.value2 = value2;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return next > 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            if (next == 2) {
                next = 1;
                return value1;
            }
            if (next == 1) {
                next = 0;
                return value2;
            }
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected static class ThreeValueIterator<T> implements Iterator<T> {

        private final T value1;
        private final T value2;
        private final T value3;
        private int next = 3;

        protected ThreeValueIterator( T value1,
                                      T value2,
                                      T value3 ) {
            this.value1 = value1;
            this.value2 = value2;
            this.value3 = value3;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return next > 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            if (next == 3) {
                next = 2;
                return value1;
            }
            if (next == 2) {
                next = 1;
                return value2;
            }
            if (next == 1) {
                next = 0;
                return value3;
            }
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
