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
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;

/**
 * The record of a source contributing only properties to a node.
 * 
 * @author Randall Hauch
 */
@Immutable
public class ThreePropertyContribution extends NonEmptyContribution {

    /**
     * This is the first version of this class. See the documentation of MergePlan.serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private final Property property1;
    private final Property property2;
    private final Property property3;

    /**
     * Create a contribution of node properties from the source with the supplied name.
     * 
     * @param sourceName the name of the source, which may not be null or blank
     * @param locationInSource the location in the source for this contributed information; may not be null
     * @param expirationTime the time (in UTC) after which this contribution should be considered expired, or null if there is no
     *        expiration time
     * @param property1 the first property from the source; may not be null
     * @param property2 the second property from the source; may not be null
     * @param property3 the third property from the source; may not be null
     */
    public ThreePropertyContribution( String sourceName,
                                      Location locationInSource,
                                      DateTime expirationTime,
                                      Property property1,
                                      Property property2,
                                      Property property3 ) {
        super(sourceName, locationInSource, expirationTime);
        assert property1 != null;
        assert property1.isEmpty() == false;
        assert property2 != null;
        assert property2.isEmpty() == false;
        assert property3 != null;
        assert property3.isEmpty() == false;
        this.property1 = property1;
        this.property2 = property2;
        this.property3 = property3;
        if (ContributionStatistics.RECORD) ContributionStatistics.record(3, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getProperties()
     */
    @Override
    public Iterator<Property> getProperties() {
        return new ThreeValueIterator<Property>(property1, property2, property3);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getPropertyCount()
     */
    @Override
    public int getPropertyCount() {
        return 3;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.contribution.Contribution#getProperty(org.jboss.dna.graph.property.Name)
     */
    @Override
    public Property getProperty( Name name ) {
        if (this.property1.getName().equals(name)) return property1;
        if (this.property2.getName().equals(name)) return property2;
        if (this.property3.getName().equals(name)) return property3;
        return null;
    }

}
