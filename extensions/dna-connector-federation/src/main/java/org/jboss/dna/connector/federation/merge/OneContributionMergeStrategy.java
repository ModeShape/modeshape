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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.IoException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.UuidFactory;
import org.jboss.dna.spi.graph.Path.Segment;

/**
 * A merge strategy that is optimized for merging when there is a single contribution.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class OneContributionMergeStrategy implements MergeStrategy {

    public static final boolean DEFAULT_REUSE_UUID_FROM_CONTRIBUTION = true;

    private boolean useUuidFromContribution = DEFAULT_REUSE_UUID_FROM_CONTRIBUTION;

    /**
     * @return reuseUuidFromContribution
     */
    public boolean isContributionUuidUsedForFederatedNode() {
        return useUuidFromContribution;
    }

    /**
     * @param useUuidFromContribution Sets useUuidFromContribution to the specified value.
     */
    public void setContributionUuidUsedForFederatedNode( boolean useUuidFromContribution ) {
        this.useUuidFromContribution = useUuidFromContribution;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method only uses the one and only one non-null {@link Contribution} in the <code>contributions</code>.
     * </p>
     * 
     * @see org.jboss.dna.connector.federation.merge.MergeStrategy#merge(org.jboss.dna.connector.federation.merge.FederatedNode,
     *      java.util.List, org.jboss.dna.spi.ExecutionContext)
     */
    public void merge( FederatedNode federatedNode,
                       List<Contribution> contributions,
                       ExecutionContext context ) {
        assert federatedNode != null;
        assert context != null;
        assert contributions != null;
        assert contributions.size() > 0;
        Contribution contribution = contributions.get(0);
        assert contribution != null;
        final boolean findUuid = isContributionUuidUsedForFederatedNode();
        // Copy the children ...
        List<Segment> children = federatedNode.getChildren();
        children.clear();
        Iterator<Segment> childIterator = contribution.getChildren();
        while (childIterator.hasNext()) {
            Segment child = childIterator.next();
            children.add(child);
        }
        // Copy the properties ...
        Map<Name, Property> properties = federatedNode.getPropertiesByName();
        properties.clear();
        UUID uuid = null;
        UuidFactory uuidFactory = null;
        Iterator<Property> propertyIterator = contribution.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            properties.put(property.getName(), property);
            if (findUuid && uuid == null && property.getName().getLocalName().equals("uuid") && property.isSingle()) {
                if (uuidFactory == null) uuidFactory = context.getValueFactories().getUuidFactory();
                try {
                    uuid = uuidFactory.create(property.getValues().next());
                } catch (IoException e) {
                    // Ignore conversion exceptions
                }
            }
        }
        // If we found a single "uuid" property whose value is a valid UUID ..
        if (uuid != null) {
            // then set the UUID on the federated node ...
            federatedNode.setUuid(uuid);
        }
        // Assign the merge plan ...
        MergePlan mergePlan = MergePlan.create(contributions);
        federatedNode.setMergePlan(mergePlan);
    }

}
