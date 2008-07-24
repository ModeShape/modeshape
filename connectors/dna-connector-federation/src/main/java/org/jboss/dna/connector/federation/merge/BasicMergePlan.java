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

import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.contribution.EmptyContribution;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * This class represents the details about how information from different sources are merged into a single federated node.
 * <p>
 * A merge plan basically consists of the individual contribution from each source and the information about how these
 * contributions were merged into the single federated node.
 * </p>
 * <p>
 * Merge plans are designed to be {@link Serializable serializable}, as they are persisted on the federated node and deserialized
 * to assist in the management of the federated node.
 * </p>
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicMergePlan implements Serializable {

    /**
     * Define the earliest version of this class that is supported. The Java runtime, upon deserialization, compares the
     * serialized object's version to this, and if less than this version will throw a {@link InvalidClassException}. If, however,
     * the serialized object's version is compatible with this class, it will be deserialized successfully.
     * <p>
     * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/version.html#6678">Sun's documentation</a> describes
     * the following changes can be made without negatively affecting the deserialization of older versions:
     * <ul>
     * <li>Adding fields - When the class being reconstituted has a field that does not occur in the stream, that field in the
     * object will be initialized to the default value for its type. If class-specific initialization is needed, the class may
     * provide a readObject method that can initialize the field to nondefault values.</i>
     * <li>Adding classes - The stream will contain the type hierarchy of each object in the stream. Comparing this hierarchy in
     * the stream with the current class can detect additional classes. Since there is no information in the stream from which to
     * initialize the object, the class's fields will be initialized to the default values.</i>
     * <li>Removing classes - Comparing the class hierarchy in the stream with that of the current class can detect that a class
     * has been deleted. In this case, the fields and objects corresponding to that class are read from the stream. Primitive
     * fields are discarded, but the objects referenced by the deleted class are created, since they may be referred to later in
     * the stream. They will be garbage-collected when the stream is garbage-collected or reset.</i>
     * <li>Adding writeObject/readObject methods - If the version reading the stream has these methods then readObject is
     * expected, as usual, to read the required data written to the stream by the default serialization. It should call
     * defaultReadObject first before reading any optional data. The writeObject method is expected as usual to call
     * defaultWriteObject to write the required data and then may write optional data.</i>
     * <li>Removing writeObject/readObject methods - If the class reading the stream does not have these methods, the required
     * data will be read by default serialization, and the optional data will be discarded.</i>
     * <li>Adding java.io.Serializable - This is equivalent to adding types. There will be no values in the stream for this class
     * so its fields will be initialized to default values. The support for subclassing nonserializable classes requires that the
     * class's supertype have a no-arg constructor and the class itself will be initialized to default values. If the no-arg
     * constructor is not available, the InvalidClassException is thrown.</i>
     * <li>Changing the access to a field - The access modifiers public, package, protected, and private have no effect on the
     * ability of serialization to assign values to the fields.</i>
     * <li>Changing a field from static to nonstatic or transient to nontransient - When relying on default serialization to
     * compute the serializable fields, this change is equivalent to adding a field to the class. The new field will be written to
     * the stream but earlier classes will ignore the value since serialization will not assign values to static or transient
     * fields.</i>
     * </ul>
     * All other kinds of modifications should be avoided.
     * </p>
     */
    private static final long serialVersionUID = 1L;

    private final Map<String, Contribution> contributions = new HashMap<String, Contribution>();
    private Map<Name, Property> annotations = null;
    private DateTime expirationTimeInUtc;

    /**
     * Create this version
     */
    public BasicMergePlan() {
    }

    /**
     * Determine whether this merge plan has expired given the supplied current time. The {@link #getExpirationTimeInUtc()
     * expiration time} is the earliest time that any of the {@link #getContributionFrom(String) contributions}
     * {@link Contribution#getExpirationTimeInUtc()}.
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
     * Get the expiration time (in UTC) that is the earliest time that any of the {@link #getContributionFrom(String)
     * contributions} {@link Contribution#getExpirationTimeInUtc()}.
     * 
     * @return the expiration time in UTC, or null if there is no known expiration time
     */
    public DateTime getExpirationTimeInUtc() {
        return expirationTimeInUtc;
    }

    /**
     * Get the contribution from the source with the supplied name. Note that contributions always include sources that contribute
     * information and sources that contribute no information. If a source is not included in this list, its contributions are
     * <i>unknown</i>; that is, it is unknown whether that source does or does not contribute to the node.
     * 
     * @param sourceName the name of the source
     * @return the contribution, or null if the contribution of the source is unknown
     */
    public Contribution getContributionFrom( String sourceName ) {
        return contributions.get(sourceName);
    }

    /**
     * Return whether the named source was consulted for a contribution.
     * 
     * @param sourceName the name of the source
     * @return true if the source has some {@link Contribution contribution} (even if it is an {@link EmptyContribution})
     */
    public boolean isSource( String sourceName ) {
        return contributions.containsKey(sourceName);
    }

    /**
     * Get the names of the contributing sources.
     * 
     * @return the names of the sources that have some contribution
     */
    public Set<String> getNamesOfContributingSources() {
        return contributions.keySet();
    }

    /**
     * Add the supplied contribution, replacing and returning any previous contribution for the same source.
     * 
     * @param contribution the new contribution
     * @return the previous contribution for the source, or null if there was no previous contribution.
     */
    public Contribution addContribution( Contribution contribution ) {
        assert contribution != null;
        Contribution previous = contributions.put(contribution.getSourceName(), contribution);
        DateTime contributionExpirationInUtc = contribution.getExpirationTimeInUtc();
        if (expirationTimeInUtc == null || contributionExpirationInUtc.isBefore(expirationTimeInUtc)) {
            expirationTimeInUtc = contributionExpirationInUtc;
        }
        return previous;
    }

    /**
     * Get the plan annotation property with the given name. Plan annotations are custom properties that may be set by
     * MergeProcessor implementations to store custom properties on the plan. This method does nothing if the supplied name is
     * null
     * 
     * @param name the name of the annotation
     * @return the existing annotation, or null if there is no annotation with the supplied name
     * @see #setAnnotation(Property)
     */
    public Property getAnnotation( Name name ) {
        if (name == null) return null;
        if (this.annotations == null) return null;
        return this.annotations.get(name);
    }

    /**
     * Set the plan annotation property. This method replaces and returns any existing annotation property with the same name.
     * This method also returns immediately if the supplied annotation is null.
     * 
     * @param annotation the new annotation
     * @return the previous annotation property with the same name, or null if there was no previous annotation property for the
     *         name
     * @see #getAnnotation(Name)
     */
    public Property setAnnotation( Property annotation ) {
        if (annotation == null) return null;
        if (this.annotations == null) {
            this.annotations = new HashMap<Name, Property>();
        }
        return this.annotations.put(annotation.getName(), annotation);
    }

}
