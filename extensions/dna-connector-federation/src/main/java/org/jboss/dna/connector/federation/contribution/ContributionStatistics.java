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
package org.jboss.dna.connector.federation.contribution;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCode;

/**
 * Simple utility class to record the distribution of contributions.
 * 
 * @author Randall Hauch
 */
public class ContributionStatistics {

    /**
     * This should only be enabled when attempting to accumulate the distribution, and should <i>never</i> be enabled in a
     * release.
     */
    /*package*/static final boolean RECORD = false;

    private static final ConcurrentMap<Stats, AtomicLong> DATA = new ConcurrentHashMap<Stats, AtomicLong>();

    /**
     * Record a {@link Contribution} was created for the supplied number of properties and children. {@link #RECORD} should be
     * checked <i>before</i> this method is called.
     * 
     * @param propertyCount the number of properties
     * @param childrenCount the number of children
     */
    /*package*/static void record( int propertyCount,
                                    int childrenCount ) {
        Stats key = new Stats(propertyCount, childrenCount);
        AtomicLong existing = DATA.putIfAbsent(key, new AtomicLong(1l));
        if (existing != null) existing.incrementAndGet();
    }

    public boolean isRecording() {
        return RECORD;
    }

    /**
     * Get a copy of the raw statistics data.
     * 
     * @return a copy of the data; never null
     */
    public static Map<Stats, AtomicLong> getData() {
        return new HashMap<Stats, AtomicLong>(DATA);
    }

    /**
     * Get the N most populous combinations of properties and children counts.
     * 
     * @param n the maximum number of data objects to find and return in the raw data; must be positive
     * @return the list of N (or fewer)
     */
    public static List<Data> getTop( int n ) {
        ArgCheck.isPositive(n, "n");
        LinkedList<Data> results = new LinkedList<Data>();
        for (Map.Entry<Stats, AtomicLong> entry : DATA.entrySet()) {
            long value = entry.getValue().get();
            if (results.size() >= n) {
                Data last = results.getLast();
                if (value <= last.getInstanceCount()) continue;
                // The new count is larger than the smallest, so pop the smallest and add the newest ...
                results.removeLast();
            }
            results.add(new Data(entry.getKey(), value));
            Collections.sort(results);
        }
        return results;
    }

    public static final class Data implements Comparable<Data> {
        private final int propertyCount;
        private final int childrenCount;
        private final long instanceCount;

        protected Data( Stats stats,
                        long instanceCount ) {
            this.propertyCount = stats.getPropertyCount();
            this.childrenCount = stats.getChildrenCount();
            this.instanceCount = instanceCount;
        }

        /**
         * @return childrenCount
         */
        public int getChildrenCount() {
            return childrenCount;
        }

        /**
         * @return propertyCount
         */
        public int getPropertyCount() {
            return propertyCount;
        }

        /**
         * @return instanceCount
         */
        public long getInstanceCount() {
            return instanceCount;
        }

        /**
         * {@inheritDoc} This orders the values in the opposite order, so that those with larger numbers of instances appear
         * first.
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( Data that ) {
            long diff = that.getInstanceCount() - this.getInstanceCount(); // backwards
            return diff < 0l ? -1 : diff > 0 ? 1 : 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return HashCode.compute(this.propertyCount, this.childrenCount, this.instanceCount);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Data) {
                Data that = (Data)obj;
                if (this.propertyCount != that.propertyCount) return false;
                if (this.childrenCount != that.childrenCount) return false;
                if (this.instanceCount != that.instanceCount) return false;
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
            return "(# props=" + this.getPropertyCount() + ", # children=" + this.getChildrenCount() + ") => "
                   + this.getInstanceCount();
        }
    }

    public static final class Stats {
        private final int propertyCount;
        private final int childrenCount;

        protected Stats( int propertyCount,
                         int childrenCount ) {
            this.propertyCount = propertyCount;
            this.childrenCount = childrenCount;
        }

        /**
         * @return childrenCount
         */
        public int getChildrenCount() {
            return childrenCount;
        }

        /**
         * @return propertyCount
         */
        public int getPropertyCount() {
            return propertyCount;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return HashCode.compute(this.propertyCount, this.childrenCount);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Stats) {
                Stats that = (Stats)obj;
                if (this.propertyCount != that.propertyCount) return false;
                if (this.childrenCount != that.childrenCount) return false;
                return true;
            }
            return false;
        }
    }

}
