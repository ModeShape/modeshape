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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.contribution.EmptyContribution;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;

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
@ThreadSafe
public abstract class MergePlan implements Serializable, Iterable<Contribution> {

    public static MergePlan create( Contribution... contributions ) {
        CheckArg.isNotNull(contributions, "contributions");
        switch (contributions.length) {
            case 0:
                throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text("contributions"));
            case 1:
                return new OneContributionMergePlan(contributions[0]);
            case 2:
                return new TwoContributionMergePlan(contributions[0], contributions[1]);
            case 3:
                return new ThreeContributionMergePlan(contributions[0], contributions[1], contributions[2]);
            case 4:
                return new FourContributionMergePlan(contributions[0], contributions[1], contributions[2], contributions[3]);
            case 5:
                return new FiveContributionMergePlan(contributions[0], contributions[1], contributions[2], contributions[3],
                                                     contributions[4]);
            default:
                return new MultipleContributionMergePlan(contributions);
        }
    }

    public static MergePlan create( Collection<Contribution> contributions ) {
        CheckArg.isNotNull(contributions, "contributions");
        Iterator<Contribution> iter = contributions.iterator();
        switch (contributions.size()) {
            case 0:
                throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text("contributions"));
            case 1:
                return new OneContributionMergePlan(iter.next());
            case 2:
                return new TwoContributionMergePlan(iter.next(), iter.next());
            case 3:
                return new ThreeContributionMergePlan(iter.next(), iter.next(), iter.next());
            case 4:
                return new FourContributionMergePlan(iter.next(), iter.next(), iter.next(), iter.next());
            case 5:
                return new FiveContributionMergePlan(iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
            default:
                return new MultipleContributionMergePlan(contributions);
        }
    }

    public static MergePlan addContribution( MergePlan plan,
                                             Contribution contribution ) {
        CheckArg.isNotNull(plan, "plan");
        CheckArg.isNotNull(contribution, "contribution");
        if (plan instanceof MultipleContributionMergePlan) {
            ((MultipleContributionMergePlan)plan).addContribution(contribution);
            return plan;
        }
        MergePlan newPlan = null;
        if (plan instanceof OneContributionMergePlan) {
            newPlan = new TwoContributionMergePlan(plan.iterator().next(), contribution);
        } else if (plan instanceof TwoContributionMergePlan) {
            Iterator<Contribution> iter = plan.iterator();
            newPlan = new ThreeContributionMergePlan(iter.next(), iter.next(), contribution);
        } else if (plan instanceof ThreeContributionMergePlan) {
            Iterator<Contribution> iter = plan.iterator();
            newPlan = new FourContributionMergePlan(iter.next(), iter.next(), iter.next(), contribution);
        } else if (plan instanceof FourContributionMergePlan) {
            Iterator<Contribution> iter = plan.iterator();
            newPlan = new FiveContributionMergePlan(iter.next(), iter.next(), iter.next(), iter.next(), contribution);
        } else {
            MultipleContributionMergePlan multiPlan = new MultipleContributionMergePlan();
            for (Contribution existingContribution : plan) {
                multiPlan.addContribution(existingContribution);
            }
            multiPlan.addContribution(contribution);
            newPlan = multiPlan;
        }
        newPlan.setAnnotations(plan.getAnnotations());
        return newPlan;
    }

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

    private final ReadWriteLock annotationLock = new ReentrantReadWriteLock();
    private DateTime expirationTimeInUtc;
    @GuardedBy( "annotationLock" )
    private Map<Name, Property> annotations = null;

    /**
     * Create an empty merge plan
     */
    protected MergePlan() {
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
        return utcTime.isAfter(getExpirationTimeInUtc());
    }

    /**
     * Get the expiration time (in UTC) that is the earliest time that any of the {@link #getContributionFrom(String)
     * contributions} {@link Contribution#getExpirationTimeInUtc()}.
     * 
     * @return the expiration time in UTC, or null if there is no known expiration time
     */
    public DateTime getExpirationTimeInUtc() {
        if (expirationTimeInUtc == null) {
            // This is computed regardless of a lock, since it's not expensive and idempotent
            DateTime earliest = null;
            for (Contribution contribution : this) {
                DateTime contributionTime = contribution.getExpirationTimeInUtc();
                if (earliest == null || (contributionTime != null && contributionTime.isBefore(earliest))) {
                    earliest = contributionTime;
                }
            }
            expirationTimeInUtc = earliest;
        }
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
    public abstract Contribution getContributionFrom( String sourceName );

    /**
     * Return whether the named source was consulted for a contribution.
     * 
     * @param sourceName the name of the source
     * @return true if the source has some {@link Contribution contribution} (even if it is an {@link EmptyContribution})
     */
    public abstract boolean isSource( String sourceName );

    public abstract int getContributionCount();

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
        try {
            annotationLock.readLock().lock();
            if (this.annotations == null) return null;
            return this.annotations.get(name);
        } finally {
            annotationLock.readLock().unlock();
        }
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
        try {
            annotationLock.writeLock().lock();
            if (this.annotations == null) {
                this.annotations = new HashMap<Name, Property>();
            }
            return this.annotations.put(annotation.getName(), annotation);
        } finally {
            annotationLock.writeLock().unlock();
        }
    }

    /**
     * Get the number of annotations.
     * 
     * @return the number of annotations
     */
    public int getAnnotationCount() {
        try {
            annotationLock.readLock().lock();
            if (this.annotations == null) return 0;
            return this.annotations.size();
        } finally {
            annotationLock.readLock().unlock();
        }
    }

    /**
     * Get the set of annotation {@link Name names}.
     * 
     * @return the unmodifiable set of names, or an empty set if there are no annotations
     */
    public Set<Name> getAnnotationNames() {
        try {
            annotationLock.readLock().lock();
            if (this.annotations == null) return Collections.emptySet();
            return Collections.unmodifiableSet(this.annotations.keySet());
        } finally {
            annotationLock.readLock().unlock();
        }
    }

    /**
     * Set the annotations. This
     * 
     * @param annotations
     */
    protected void setAnnotations( Map<Name, Property> annotations ) {
        try {
            annotationLock.writeLock().lock();
            this.annotations = annotations == null || annotations.isEmpty() ? null : annotations;
        } finally {
            annotationLock.writeLock().unlock();
        }
    }

    /**
     * Get a copy of the annotations.
     * 
     * @return a copy of annotations; never null
     */
    public Map<Name, Property> getAnnotations() {
        Map<Name, Property> result = null;
        try {
            annotationLock.writeLock().lock();
            if (this.annotations != null && !this.annotations.isEmpty()) {
                result = new HashMap<Name, Property>(this.annotations);
            } else {
                result = Collections.emptyMap();
            }
        } finally {
            annotationLock.writeLock().unlock();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Contribution contribution : this) {
            if (!first) {
                first = false;
                sb.append(", ");
            }
            sb.append(contribution);
        }
        sb.append(StringUtil.readableString(getAnnotations()));
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof MergePlan) {
            MergePlan that = (MergePlan)obj;
            if (this.getContributionCount() != that.getContributionCount()) return false;
            Iterator<Contribution> thisContribution = this.iterator();
            Iterator<Contribution> thatContribution = that.iterator();
            while (thisContribution.hasNext() && thatContribution.hasNext()) {
                if (!thisContribution.next().equals(thatContribution.next())) return false;
            }
            if (this.getAnnotationCount() != that.getAnnotationCount()) return false;
            if (!this.getAnnotations().equals(that.getAnnotations())) return false;
            return true;
        }
        return false;
    }

    protected boolean checkEachContributionIsFromDistinctSource() {
        Set<String> sourceNames = new HashSet<String>();
        for (Contribution contribution : this) {
            boolean added = sourceNames.add(contribution.getSourceName());
            if (!added) return false;
        }
        return true;
    }

}
