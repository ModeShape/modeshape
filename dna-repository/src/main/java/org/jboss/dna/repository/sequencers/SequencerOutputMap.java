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
package org.jboss.dna.repository.sequencers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.sequencers.SequencerOutput;

/**
 * A basic {@link SequencerOutput} that records all information in-memory and which organizes the properties by
 * {@link Path node paths} and provides access to the nodes in a natural path-order.
 * @author Randall Hauch
 */
@NotThreadSafe
public class SequencerOutputMap implements SequencerOutput, Iterable<SequencerOutputMap.Entry> {

    private static final String JCR_PRIMARY_TYPE_PROPERTY_NAME = "jcr:primaryType";
    private static final String JCR_NAME_PROPERTY_NAME = "jcr:name";

    private final Map<Path, List<PropertyValue>> data;
    private boolean valuesSorted = true;
    private final PathFactory pathFactory;

    public SequencerOutputMap( PathFactory pathFactory ) {
        ArgCheck.isNotNull(pathFactory, "pathFactory");
        this.data = new HashMap<Path, List<PropertyValue>>();
        this.pathFactory = pathFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( String nodePath, String property, Object... values ) {
        property = property.trim();
        if (JCR_NAME_PROPERTY_NAME.equals(property)) return; // ignore the "jcr:name" property
        nodePath = nodePath.trim();
        if (nodePath.endsWith("/")) nodePath = nodePath.replaceFirst("/+$", "");

        // Find or create the entry for this node ...
        Path path = pathFactory.create(nodePath);
        List<PropertyValue> properties = this.data.get(path);
        if (properties == null) {
            if (values == null || values.length == 0) return; // do nothing
            properties = new ArrayList<PropertyValue>();
            this.data.put(path, properties);
        }
        if (values == null || values.length == 0) {
            properties.remove(new PropertyValue(property, null));
        } else {
            Object propValue = values.length == 1 ? values[0] : values;
            PropertyValue value = new PropertyValue(property, propValue);
            properties.add(value);
            valuesSorted = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setReference( String nodePath, String property, String... paths ) {
        if (paths == null || paths.length == 0) {
            setProperty(nodePath, property, (Object[])null);
        } else if (paths.length == 1) {
            setProperty(nodePath, property, pathFactory.create(paths[0]));
        } else {
            Path[] pathsArray = new Path[paths.length];
            for (int i = 0; i != paths.length; ++i) {
                pathsArray[i] = pathFactory.create(paths[i]);
            }
            setProperty(nodePath, property, (Object[])pathsArray);
        }
    }

    /**
     * Return the number of node entries in this map.
     * @return the number of entries
     */
    public int size() {
        return this.data.size();
    }

    /**
     * Return whether there are no entries
     * @return true if this container is empty, or false otherwise
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    protected List<PropertyValue> removeProperties( Path nodePath ) {
        return this.data.remove(nodePath);
    }

    /**
     * Get the properties for the node given by the supplied path.
     * @param nodePath the path to the node
     * @return the property values, or null if there are none
     */
    protected List<PropertyValue> getProperties( Path nodePath ) {
        return data.get(nodePath);
    }

    /**
     * Return the entries in this output in an order with shorter paths first.
     * <p>
     * {@inheritDoc}
     */
    public Iterator<Entry> iterator() {
        LinkedList<Path> paths = new LinkedList<Path>(data.keySet());
        Collections.sort(paths);
        sortValues();
        return new EntryIterator(paths.iterator());
    }

    protected void sortValues() {
        if (!valuesSorted) {
            for (List<PropertyValue> values : this.data.values()) {
                if (values.size() > 1) Collections.sort(values);
            }
            valuesSorted = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return StringUtil.readableString(this.data);
    }

    /**
     * A property name and value pair. PropertyValue instances have a natural order where the <code>jcr:primaryType</code> is
     * first, followed by all other properties in ascending lexicographical order according to the {@link #getName() name}.
     * @author Randall Hauch
     */
    @Immutable
    public class PropertyValue implements Comparable<PropertyValue> {

        private final String name;
        private final Object value;

        protected PropertyValue( String propertyName, Object value ) {
            this.name = propertyName;
            this.value = value;
        }

        /**
         * Get the property name.
         * @return the property name; never null
         */
        public String getName() {
            return this.name;
        }

        /**
         * Get the property value, which is either a single value or an array of values.
         * @return the property value
         */
        public Object getValue() {
            return this.value;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo( PropertyValue that ) {
            if (this == that) return 0;
            if (this.name.equals(JCR_PRIMARY_TYPE_PROPERTY_NAME)) return -1;
            if (that.name.equals(JCR_PRIMARY_TYPE_PROPERTY_NAME)) return 1;
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
            return "[" + this.name + "=" + StringUtil.readableString(value) + "]";
        }
    }

    /**
     * An entry in a SequencerOutputMap, which contains the path of the node and the {@link #getPropertyValues() property values}
     * on the node.
     * @author Randall Hauch
     */
    @Immutable
    public class Entry {

        private final Path path;
        private final String primaryType;
        private final List<PropertyValue> properties;

        protected Entry( Path path, List<PropertyValue> properties ) {
            assert path != null;
            assert properties != null;
            this.path = path;
            this.properties = properties;
            if (this.properties.size() > 0 && this.properties.get(0).getName().equals("jcr:primaryType")) {
                PropertyValue primaryTypeProperty = this.properties.remove(0);
                this.primaryType = primaryTypeProperty.getValue().toString();
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
         * @return the primary type, or null
         */
        public String getPrimaryTypeValue() {
            return this.primaryType;
        }

        /**
         * Get the property values, which may be empty
         * @return value
         */
        public List<PropertyValue> getPropertyValues() {
            return getProperties(path);
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
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Entry next() {
            this.last = iter.next();
            return new Entry(last, getProperties(last));
        }

        /**
         * {@inheritDoc}
         */
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
