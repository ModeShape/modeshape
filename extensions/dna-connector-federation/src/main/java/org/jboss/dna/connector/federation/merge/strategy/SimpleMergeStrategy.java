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
import org.jboss.dna.graph.properties.IoException;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.UuidFactory;
import org.jboss.dna.graph.properties.ValueComparators;
import org.jboss.dna.graph.properties.Path.Segment;

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
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        // Prepare the federated node ...
        List<Segment> children = federatedNode.getChildren();
        children.clear();
        Map<Name, Integer> childNames = new HashMap<Name, Integer>();
        Map<Name, Property> properties = federatedNode.getPropertiesByName();
        properties.clear();
        UUID uuid = null;
        UuidFactory uuidFactory = null;
        final boolean removeDuplicateProperties = isRemoveDuplicateProperties();
        // Iterate over the set of contributions (in order) ...
        for (Contribution contribution : contributions) {
            // If the contribution is a placeholder contribution, then the children should be merged into other children ...
            if (contribution.isPlaceholder()) {
                // Iterate over the children and add only if there is not already one ...
                Iterator<Segment> childIterator = contribution.getChildren();
                while (childIterator.hasNext()) {
                    Segment child = childIterator.next();
                    if (!childNames.containsKey(child.getName())) {
                        childNames.put(child.getName(), 1);
                        children.add(pathFactory.createSegment(child.getName()));
                    }
                }

            } else {
                // Copy the children ...
                Iterator<Segment> childIterator = contribution.getChildren();
                while (childIterator.hasNext()) {
                    Segment child = childIterator.next();
                    int index = Path.NO_INDEX;
                    Integer previous = childNames.put(child.getName(), 1);
                    if (previous != null) {
                        int previousValue = previous.intValue();
                        // Correct the index in the child name map ...
                        childNames.put(child.getName(), ++previousValue);
                        index = previousValue;
                    }
                    children.add(pathFactory.createSegment(child.getName(), index));
                }

                // Copy the properties ...
                Iterator<Property> propertyIterator = contribution.getProperties();
                while (propertyIterator.hasNext()) {
                    Property property = propertyIterator.next();
                    // Skip the "uuid" property on all root nodes ...
                    if (federatedNode.getPath().isRoot() && property.getName().getLocalName().equals("uuid")) continue;
                    Property existing = properties.put(property.getName(), property);
                    if (existing != null) {
                        // There's already an existing property, so we need to merge them ...
                        Property merged = merge(existing, property, context.getPropertyFactory(), removeDuplicateProperties);
                        properties.put(property.getName(), merged);
                    }

                    if (uuid == null && property.getName().getLocalName().equals("uuid") && property.isSingle()) {
                        if (uuidFactory == null) uuidFactory = context.getValueFactories().getUuidFactory();
                        try {
                            uuid = uuidFactory.create(property.getValues().next());
                        } catch (IoException e) {
                            // Ignore conversion exceptions
                            assert uuid == null;
                        }
                    }
                }
            }
        }
        // If we found a single "uuid" property whose value is a valid UUID ..
        if (uuid != null) {
            // then set the UUID on the federated node ...
            federatedNode.setUuid(uuid);
        }
        // Set the UUID as a property ...
        Property uuidProperty = context.getPropertyFactory().create(DnaLexicon.UUID, federatedNode.getUuid());
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
}
