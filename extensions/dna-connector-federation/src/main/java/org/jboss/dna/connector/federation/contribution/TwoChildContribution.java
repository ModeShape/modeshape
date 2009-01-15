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
package org.jboss.dna.connector.federation.contribution;

import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.DateTime;

/**
 * The record of a source contributing only two children to a node.
 * 
 * @author Randall Hauch
 */
@Immutable
public class TwoChildContribution extends NonEmptyContribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private final Location child1;
    private final Location child2;

    /**
     * Create a contribution of two children from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param child1 the first child contributed from the source; may not be null
     * @param child2 the second child contributed from the source; may not be null
     */
    public TwoChildContribution( String sourceName,
                                 Location locationInSource,
                                 DateTime expirationTime,
                                 Location child1,
                                 Location child2 ) {
        super(sourceName, locationInSource, expirationTime);
        assert child1 != null;
        assert child2 != null;
        this.child1 = child1;
        this.child2 = child2;
        if (ContributionStatistics.RECORD) ContributionStatistics.record(0, 2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getChildren()
     */
    @Override
    public Iterator<Location> getChildren() {
        return new TwoValueIterator<Location>(child1, child2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getChildrenCount()
     */
    @Override
    public int getChildrenCount() {
        return 2;
    }

}
