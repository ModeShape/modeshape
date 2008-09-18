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

import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Path.Segment;

/**
 * The record of a source contributing only a single child to a node.
 * 
 * @author Randall Hauch
 */
@Immutable
public class OneChildContribution extends NonEmptyContribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private final Segment child;

    /**
     * Create a contribution of a single child from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param pathInSource the path in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param child the child contributed from the source; may not be null
     */
    public OneChildContribution( String sourceName,
                                 Path pathInSource,
                                 DateTime expirationTime,
                                 Segment child ) {
        super(sourceName, pathInSource, expirationTime);
        assert child != null;
        this.child = child;
        if (ContributionStatistics.RECORD) ContributionStatistics.record(0, 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getChildren()
     */
    @Override
    public Iterator<Segment> getChildren() {
        return new OneValueIterator<Segment>(child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getChildrenCount()
     */
    @Override
    public int getChildrenCount() {
        return 1;
    }

}
