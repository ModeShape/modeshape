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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueComparators;

/**
 * This merge strategy simply merges all of the contributions' properties and combines the children according to the order of the
 * contributions. No children are merged, and all properties are used (except if they are deemed to be duplicates of the property
 * in other contributions).
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class SimpleMergeStrategy implements MergeStrategy {

    private boolean removeDuplicateProperties = true;

    /**
     * @return removeDuplicateProperties
     */
    public boolean isRemoveDuplicateProperties() {
        return removeDuplicateProperties;
    }

    /**
     * @param removeDuplicateProperties Sets removeDuplicateProperties to the specified value.
     */
    public void setRemoveDuplicateProperties( boolean removeDuplicateProperties ) {
        this.removeDuplicateProperties = removeDuplicateProperties;
    }

    /**
     * {@inheritDoc}
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

        final Location location = federatedNode.getActualLocationOfNode();
        final boolean isRoot = location.hasPath() && location.getPath().isRoot();
        final boolean removeDuplicateProperties = isRemoveDuplicateProperties();
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        final Map<Name, Integer> childNames = new HashMap<Name, Integer>();
        final Map<Name, Property> properties = federatedNode.getPropertiesByName();
        properties.clear();

        // Record the different ID properties from the federated node and (later) from each contribution
        final Map<Name, Property> idProperties = new HashMap<Name, Property>();
        for (Property idProperty : location) {
            idProperties.put(idProperty.getName(), idProperty);
        }

        // Iterate over each of the contributions (in order) ...
        for (Contribution contribution : contributions) {
            if (contribution.isEmpty()) continue;
            // If the contribution is a placeholder contribution, then the children should be merged into other children ...
            if (contribution.isPlaceholder()) {
                // Iterate over the children and add only if there is not already one ...
                Iterator<Location> childIterator = contribution.getChildren();
                while (childIterator.hasNext()) {
                    Location child = childIterator.next();
                    Name childName = child.getPath().getLastSegment().getName();
                    if (!childNames.containsKey(childName)) {
                        childNames.put(childName, 1);
                        Path pathToChild = pathFactory.create(location.getPath(), childName);
                        federatedNode.addChild(Location.create(pathToChild));
                    }
                }
            } else {
                // Get the identification properties for each contribution ...
                Location contributionLocation = contribution.getLocationInSource();
                for (Property idProperty : contributionLocation) {
                    // Record the property ...
                    Property existing = properties.put(idProperty.getName(), idProperty);
                    if (existing != null) {
                        // There's already an existing property, so we need to merge them ...
                        Property merged = merge(existing, idProperty, context.getPropertyFactory(), removeDuplicateProperties);
                        properties.put(merged.getName(), merged);
                    }
                }

                // Accumulate the children ...
                Iterator<Location> childIterator = contribution.getChildren();
                while (childIterator.hasNext()) {
                    Location child = childIterator.next();
                    Name childName = child.getPath().getLastSegment().getName();
                    int index = Path.NO_INDEX;
                    Integer previous = childNames.put(childName, 1);
                    if (previous != null) {
                        int previousValue = previous.intValue();
                        // Correct the index in the child name map ...
                        childNames.put(childName, ++previousValue);
                        index = previousValue;
                    }
                    Path pathToChild = pathFactory.create(location.getPath(), childName, index);
                    federatedNode.addChild(Location.create(pathToChild));
                }

                // Add in the properties ...
                Iterator<Property> propertyIter = contribution.getProperties();
                while (propertyIter.hasNext()) {
                    Property property = propertyIter.next();
                    // Skip the "uuid" property on all root nodes ...
                    if (isRoot && property.getName().getLocalName().equals("uuid")) continue;

                    // Record the property ...
                    Property existing = properties.put(property.getName(), property);
                    if (existing != null) {
                        // There's already an existing property, so we need to merge them ...
                        Property merged = merge(existing, property, context.getPropertyFactory(), removeDuplicateProperties);
                        properties.put(property.getName(), merged);
                    }
                }
            }
        }

        if (idProperties.size() != 0) {
            // Update the location based upon the merged ID properties ...
            Location newLocation = location;
            for (Property idProperty : idProperties.values()) {
                newLocation = newLocation.with(idProperty);
            }
            federatedNode.setActualLocationOfNode(newLocation);
        } else {
            Location newLocation = location.with(UUID.randomUUID());
            federatedNode.setActualLocationOfNode(newLocation);
        }

        // Look for the UUID property on the location, and update the federated node ...
        Property uuidProperty = federatedNode.getActualLocationOfNode().getIdProperty(DnaLexicon.UUID);
        assert uuidProperty != null;
        properties.put(uuidProperty.getName(), uuidProperty);

        // Assign the merge plan ...
        MergePlan mergePlan = MergePlan.create(contributions);
        federatedNode.setMergePlan(mergePlan);
    }

    /**
     * Merge the values from the two properties with the same name, returning a new property with the newly merged values.
     * <p>
     * The current algorithm merges the values by concatenating the values from <code>property1</code> and <code>property2</code>,
     * and if <code>removeDuplicates</code> is true any values in <code>property2</code> that are identical to values found in
     * <code>property1</code> are skipped.
     * </p>
     * 
     * @param property1 the first property; may not be null, and must have the same {@link Property#getName() name} as
     *        <code>property2</code>
     * @param property2 the second property; may not be null, and must have the same {@link Property#getName() name} as
     *        <code>property1</code>
     * @param factory the property factory, used to create the result
     * @param removeDuplicates true if this method removes any values in the second property that duplicate values found in the
     *        first property.
     * @return the property that contains the same {@link Property#getName() name} as the input properties, but with values that
     *         are merged from both of the input properties
     */
    protected Property merge( Property property1,
                              Property property2,
                              PropertyFactory factory,
                              boolean removeDuplicates ) {
        assert property1 != null;
        assert property2 != null;
        assert property1.getName().equals(property2.getName());
        if (property1.isEmpty()) return property2;
        if (property2.isEmpty()) return property1;

        // If they are both single-valued, then we can use a more efficient algorithm ...
        if (property1.isSingle() && property2.isSingle()) {
            Object value1 = property1.getValues().next();
            Object value2 = property2.getValues().next();
            if (removeDuplicates && ValueComparators.OBJECT_COMPARATOR.compare(value1, value2) == 0) return property1;
            return factory.create(property1.getName(), new Object[] {value1, value2});
        }

        // One or both properties are multi-valued, so use an algorithm that works with in all cases ...
        if (!removeDuplicates) {
            Iterator<?> valueIterator = new DualIterator(property1.getValues(), property2.getValues());
            return factory.create(property1.getName(), valueIterator);
        }

        // First copy all the values from property 1 ...
        Object[] values = new Object[property1.size() + property2.size()];
        int index = 0;
        for (Object property1Value : property1) {
            values[index++] = property1Value;
        }
        assert index == property1.size();
        // Now add any values of property2 that don't match a value in property1 ...
        for (Object property2Value : property2) {
            // Brute force, go through the values of property1 and compare ...
            boolean matched = false;
            for (Object property1Value : property1) {
                if (ValueComparators.OBJECT_COMPARATOR.compare(property1Value, property2Value) == 0) {
                    // The values are the same ...
                    matched = true;
                    break;
                }
            }
            if (!matched) values[index++] = property2Value;
        }
        if (index != values.length) {
            Object[] newValues = new Object[index];
            System.arraycopy(values, 0, newValues, 0, index);
            values = newValues;
        }
        return factory.create(property1.getName(), values);
    }

    protected static class DualIterator implements Iterator<Object> {

        private final Iterator<?>[] iterators;
        private Iterator<?> current;
        private int index = 0;

        protected DualIterator( Iterator<?>... iterators ) {
            assert iterators != null;
            assert iterators.length > 0;
            this.iterators = iterators;
            this.current = this.iterators[0];
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            if (this.current != null) return this.current.hasNext();
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            while (this.current != null) {
                if (this.current.hasNext()) return this.current.next();
                // Get the next iterator ...
                if (++this.index < iterators.length) {
                    this.current = this.iterators[this.index];
                } else {
                    this.current = null;
                }
            }
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected static class Child {
        private final Location location;
        private final boolean placeholder;

        protected Child( Location location,
                         boolean placeholder ) {
            assert location != null;
            this.location = location;
            this.placeholder = placeholder;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return location.hasIdProperties() ? location.getIdProperties().hashCode() : location.getPath()
                                                                                                .getLastSegment()
                                                                                                .getName()
                                                                                                .hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Child) {
                Child that = (Child)obj;
                if (this.placeholder && that.placeholder) {
                    // If both are placeholders, then compare just the name ...
                    assert this.location.hasPath();
                    assert that.location.hasPath();
                    Name thisName = this.location.getPath().getLastSegment().getName();
                    Name thatName = that.location.getPath().getLastSegment().getName();
                    return thisName.equals(thatName);
                }
                if (location.hasIdProperties() && that.location.hasIdProperties()) {
                    List<Property> thisIds = location.getIdProperties();
                    List<Property> thatIds = that.location.getIdProperties();
                    if (thisIds.size() != thatIds.size()) return false;
                    return thisIds.containsAll(thatIds);
                }
                // One or both do not have identification properties, so delegate to the locations...
                return this.location.equals(that.location);
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
            return location.toString();
        }
    }
}
