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

import java.util.Collection;
import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.DateTime;

/**
 * The changes that were made since some time.
 */
@Immutable
public class ChangeTimeline implements Iterable<Changes> {

    private final Collection<Changes> changes;
    private final DateTime after;
    private final DateTime until;

    public ChangeTimeline( Collection<Changes> changes,
                    DateTime after,
                    DateTime until ) {
        this.changes = changes;
        this.after = after;
        this.until = until;
    }

    /**
     * Get the timestamp after which all the changes occur.
     * 
     * @return the timestamp of the changes; never null
     */
    public DateTime after() {
        return after;
    }

    /**
     * Get the timestamp of the last change set.
     * 
     * @return the timestamp of the changes; never null
     */
    public DateTime until() {
        return until;
    }

    /**
     * Get the number of change sets.
     * 
     * @return the number of change sets
     */
    public int size() {
        return changes.size();
    }

    /**
     * Deterine if there were no changes durign this timeline.
     * 
     * @return true if this timeline is empty, or false if there is at least one change
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Changes> iterator() {
        return changes.iterator();
    }
}
