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
package org.jboss.dna.connector.federation.merge.strategy;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFormatException;

/**
 * A merge strategy that is optimized for merging when there is a single contribution.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class OneContributionMergeStrategy implements MergeStrategy {

    /**
     * {@inheritDoc}
     * <p>
     * This method only uses the one and only one non-null {@link Contribution} in the <code>contributions</code>.
     * </p>
     * 
     * @see org.jboss.dna.connector.federation.merge.strategy.MergeStrategy#merge(org.jboss.dna.connector.federation.merge.FederatedNode,
     *      java.util.List, org.jboss.dna.graph.ExecutionContext)
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
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Location location = federatedNode.getActualLocationOfNode();

        // Copy the children ...
        Iterator<Location> childIterator = contribution.getChildren();
        while (childIterator.hasNext()) {
            Location child = translateChildFromSourceToRepository(pathFactory, location, childIterator.next());
            federatedNode.addChild(child);
        }

        // Copy the properties ...
        Property uuidProperty = null;
        Property dnaUuidProperty = null;
        Iterator<Property> propertyIterator = contribution.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            federatedNode.addProperty(property);
            if (property.isSingle()) {
                if (property.getName().equals(DnaLexicon.UUID) && hasUuidValue(context, property)) {
                    dnaUuidProperty = property;
                } else if (property.getName().getLocalName().equals("uuid") && hasUuidValue(context, property)) {
                    uuidProperty = property;
                }
            }
        }
        if (dnaUuidProperty != null) uuidProperty = dnaUuidProperty; // use "dna:uuid" if there is one

        // Look for the UUID property on the properties, and update the federated node ...
        if (uuidProperty != null && !uuidProperty.isEmpty()) {
            UUID uuid = context.getValueFactories().getUuidFactory().create(uuidProperty.getFirstValue());
            uuidProperty = context.getPropertyFactory().create(DnaLexicon.UUID, uuid); // Use the "dna:uuid" name
            federatedNode.setActualLocationOfNode(federatedNode.at().with(uuidProperty));
        } else {
            // Make sure there's a UUID for an identification property ...
            if (location.getUuid() == null) {
                location = location.with(UUID.randomUUID());
            }
            // Set the UUID as a property (it wasn't set already) ...
            uuidProperty = location.getIdProperty(DnaLexicon.UUID);
            assert uuidProperty != null; // there should be one!
            federatedNode.addProperty(uuidProperty);
            federatedNode.setActualLocationOfNode(location);
        }

        // Assign the merge plan ...
        MergePlan mergePlan = MergePlan.create(contributions);
        federatedNode.setMergePlan(mergePlan);
    }

    private boolean hasUuidValue( ExecutionContext context,
                                  Property property ) {
        try {
            context.getValueFactories().getUuidFactory().create(property.getFirstValue());
            return true;
        } catch (ValueFormatException e) {
            return false;
        }
    }

    /**
     * Utility method to translate the list of locations of the children so that the locations all are correctly relative to
     * parent location of the federated node.
     * 
     * @param factory the path factory
     * @param parent the parent of the child
     * @param childInSource the child to be translated, with a source-specific location
     * @return the list of locations of each child
     */
    protected Location translateChildFromSourceToRepository( PathFactory factory,
                                                             Location parent,
                                                             Location childInSource ) {
        // Convert the locations of the children (relative to the source) to be relative to this node
        Path parentPath = parent.getPath();
        if (parentPath == null) return childInSource;
        Path newPath = factory.create(parentPath, childInSource.getPath().getLastSegment());
        return childInSource.with(newPath);
    }
}
