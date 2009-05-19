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
package org.jboss.dna.graph.observe;

import org.jboss.dna.graph.property.DateTime;

/**
 * Interface used to poll for the changes that have occurred during a specified time period.
 */
public interface ChangeFeed {

    /**
     * Get the changes that were made since the supplied timestamp.
     * 
     * @param timestamp the timestamp after which all changes should be returned
     * @return the iterator over the changes that occurred after the supplied timestamp.
     */
    ChangeTimeline changesSince( DateTime timestamp );

    /**
     * Get the changes that were made since the supplied timestamp.
     * 
     * @param beginning the timestamp at the beginning of the timeline (exclusive), or null if the earliest timestamp should be
     *        used
     * @param end the timestamp at the end of the timeline (inclusive), or null if the current timestamp should be used
     * @return the iterator over the changes that occurred after the supplied timestamp.
     */
    ChangeTimeline changesBetween( DateTime beginning,
                                   DateTime end );

}
