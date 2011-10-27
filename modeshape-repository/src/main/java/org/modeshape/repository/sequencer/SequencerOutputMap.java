/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.repository.sequencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.sequencer.SequencerOutput;

/**
 * A basic {@link SequencerOutput} that records all information in-memory and which organizes the properties by {@link Path node
 * paths} and provides access to the nodes in a natural path-order.
 */
@NotThreadSafe
public class SequencerOutputMap implements SequencerOutput, Iterable<Path> {

    private final Map<Path, List<PropertyValue>> overridingValues;
    private final Map<Path, List<PropertyValue>> addedValues;
    private final ValueFactories factories;

    public SequencerOutputMap( ValueFactories factories ) {
        CheckArg.isNotNull(factories, "factories");
        this.overridingValues = new LinkedHashMap<Path, List<PropertyValue>>();
        this.addedValues = new LinkedHashMap<Path, List<PropertyValue>>();
        this.factories = factories;
    }

    ValueFactories getFactories() {
        return this.factories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        CheckArg.isNotNull(nodePath, "nodePath");
        CheckArg.isNotNull(propertyName, "property");

        // Find or create the entry for this node ...
        List<PropertyValue> properties = this.overridingValues.get(nodePath);
        if (properties == null) {
            if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) return; // do nothing
            properties = new ArrayList<PropertyValue>();
            this.overridingValues.put(nodePath, properties);
        }
        if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) {
            properties.remove(new PropertyValue(propertyName, null));
        } else {
            Object propValue = values.length == 1 ? values[0] : values;
            PropertyValue value = new PropertyValue(propertyName, propValue);
            properties.add(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addValues( Path nodePath,
                           Name propertyName,
                           Object... values ) {
        CheckArg.isNotNull(nodePath, "nodePath");
        CheckArg.isNotNull(propertyName, "property");

        // Find or create the entry for this node ...
        List<PropertyValue> properties = this.addedValues.get(nodePath);
        if (properties == null) {
            if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) return; // do nothing
            properties = new ArrayList<PropertyValue>();
            this.addedValues.put(nodePath, properties);
        }
        if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) {
            properties.remove(new PropertyValue(propertyName, null));
        } else {
            Object propValue = values.length == 1 ? values[0] : values;
            PropertyValue value = new PropertyValue(propertyName, propValue);
            properties.add(value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated As of ModeShape 2.0, the preferred approach is to use {@link #setProperty(Path, Name, Object...)}, which
     *             properly addresses the session having different namespace mappings. This method depends on the namespace
     *             mappings for the given URIs in the name components of the nodePath and propertyName to be mapped in the
     *             NamespaceRegistry of the ModeShapeEngine's (or JcrEngine's) ExecutionContext.
     */
    @Override
    @Deprecated
    public void setProperty( String nodePath,
                             String property,
                             Object... values ) {
        CheckArg.isNotEmpty(nodePath, "nodePath");
        CheckArg.isNotEmpty(property, "property");
        Path path = this.factories.getPathFactory().create(nodePath);
        Name propertyName = this.factories.getNameFactory().create(property);
        setProperty(path, propertyName, values);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated As of ModeShape 2.0, the preferred approach is to use {@link #setProperty(Path, Name, Object...)}, which
     *             properly addresses the session having different namespace mappings. This method depends on the namespace
     *             mappings for the given URIs in the name components of the nodePath and propertyName to be mapped in the
     *             NamespaceRegistry of the ModeShapeEngine's (or JcrEngine's) ExecutionContext.
     */
    @Override
    @Deprecated
    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        PathFactory pathFactory = this.factories.getPathFactory();
        Path path = pathFactory.create(nodePath);
        Name name = this.factories.getNameFactory().create(propertyName);
        Object[] values = null;
        if (paths != null && paths.length != 0) {
            values = new Path[paths.length];
            for (int i = 0, len = paths.length; i != len; ++i) {
                String pathValue = paths[i];
                values[i] = pathFactory.create(pathValue);
            }
        }
        setProperty(path, name, values);
    }

    /**
     * Return the number of node entries in this map.
     * 
     * @return the number of entries
     */
    public int size() {
        return this.overridingValues.size() + this.addedValues.size();
    }

    /**
     * Return whether there are no entries
     * 
     * @return true if this container is empty, or false otherwise
     */
    public boolean isEmpty() {
        return this.overridingValues.isEmpty() && this.addedValues.isEmpty();
    }

    protected List<PropertyValue> removeProperties( Path nodePath ) {
        return this.overridingValues.remove(nodePath);
    }

    /**
     * Get the overriding property values for the node given by the supplied path.
     * 
     * @param nodePath the path to the node
     * @return the property values, or null if there are none
     */
    protected List<PropertyValue> getOverridingValues( Path nodePath ) {
        return overridingValues.get(nodePath);
    }

    /**
     * Get the added property values for the node given by the supplied path.
     * 
     * @param nodePath the path to the node
     * @return the property values, or null if there are none
     * @see #addValues(Path, Name, Object...)
     */
    protected List<PropertyValue> getAddedValues( Path nodePath ) {
        return addedValues.get(nodePath);
    }

    /**
     * Return the entries in this output in an order with shorter paths first.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        // De-duplicate paths, since we don't have a requirement that a path can't be set and the added
        Set<Path> allPaths = new HashSet<Path>(addedValues.size() + overridingValues.size());

        allPaths.addAll(addedValues.keySet());
        allPaths.addAll(overridingValues.keySet());

        List<Path> sortedPaths = new ArrayList<Path>(allPaths);
        Collections.sort(sortedPaths);

        return sortedPaths.iterator();
    }

    // protected void sortValues() {
    // if (!valuesSorted) {
    // for (List<PropertyValue> values : this.overridingValues.values()) {
    // if (values.size() > 1) Collections.sort(values);
    // }
    // valuesSorted = true;
    // }
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ValueFactory<String> strings = factories.getStringFactory();
        for (Map.Entry<Path, List<PropertyValue>> entry : this.overridingValues.entrySet()) {
            sb.append(strings.create(entry.getKey())).append(" = ");
            List<PropertyValue> values = entry.getValue();
            if (values.size() == 1) {
                sb.append(strings.create(values.get(0)));
            } else {
                boolean first = true;
                sb.append('[');
                for (PropertyValue value : values) {
                    if (first) first = false;
                    else sb.append(", ");
                    sb.append(strings.create(value));
                }
                sb.append(']');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * A property name and value pair. PropertyValue instances have a natural order where the <code>jcr:primaryType</code> is
     * first, followed by all other properties in ascending lexicographical order according to the {@link #getName() name}.
     * 
     * @author Randall Hauch
     */
    @Immutable
    public class PropertyValue implements Comparable<PropertyValue> {

        private final Name name;
        private final Object value;

        protected PropertyValue( Name propertyName,
                                 Object value ) {
            this.name = propertyName;
            this.value = value;
        }

        /**
         * Get the property name.
         * 
         * @return the property name; never null
         */
        public Name getName() {
            return this.name;
        }

        /**
         * Get the property value, which is either a single value or an array of values.
         * 
         * @return the property value
         */
        public Object getValue() {
            return this.value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo( PropertyValue that ) {
            if (this == that) return 0;
            if (this.name.equals(JcrLexicon.PRIMARY_TYPE)) return -1;
            if (that.name.equals(JcrLexicon.PRIMARY_TYPE)) return 1;
            return this.name.compareTo(that.name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PropertyValue) {
                PropertyValue that = (PropertyValue)obj;
                if (!this.getName().equals(that.getName())) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[" + this.name + "=" + value + "]";
        }
    }

    /**
     * An entry in a SequencerOutputMap, which contains the path of the node and the {@link #getPropertyValues() property values}
     * on the node.
     * 
     * @author Randall Hauch
     */
    @Immutable
    public class Entry {

        private final Path path;
        private final Name primaryType;
        private final List<PropertyValue> properties;

        protected Entry( Path path,
                         List<PropertyValue> properties ) {
            assert path != null;
            assert properties != null;
            this.path = path;
            this.properties = properties;
            if (this.properties.size() > 0 && this.properties.get(0).getName().equals("jcr:primaryType")) {
                PropertyValue primaryTypeProperty = this.properties.remove(0);
                this.primaryType = getFactories().getNameFactory().create(primaryTypeProperty.getValue());
            } else {
                this.primaryType = null;
            }
        }

        /**
         * @return path
         */
        public Path getPath() {
            return this.path;
        }

        /**
         * Get the primary type specified for this node, or null if the type was not specified
         * 
         * @return the primary type, or null
         */
        public Name getPrimaryTypeValue() {
            return this.primaryType;
        }

        /**
         * Get the property values, which may be empty
         * 
         * @return value
         */
        public List<PropertyValue> getPropertyValues() {
            return getOverridingValues(path);
        }
    }

    protected class EntryIterator implements Iterator<Entry> {

        private Path last;
        private final Iterator<Path> iter;

        protected EntryIterator( Iterator<Path> iter ) {
            this.iter = iter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry next() {
            this.last = iter.next();
            return new Entry(last, getOverridingValues(last));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            if (last == null) throw new IllegalStateException();
            try {
                removeProperties(last);
            } finally {
                last = null;
            }
        }
    }

}
