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
package org.modeshape.graph.property.basic;

import java.util.Arrays;
import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueComparators;

/**
 * An abstract {@link Property} implementation.
 */
@Immutable
public abstract class BasicProperty implements Property {
    private static final long serialVersionUID = 1L;

    private final Name name;

    /**
     * @param name
     */
    public BasicProperty( Name name ) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<?> getValues() {
        return iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getValuesAsArray() {
        if (size() == 0) return null;
        Object[] results = new Object[size()];
        Iterator<?> iter = iterator();
        int index = 0;
        while (iter.hasNext()) {
            Object value = iter.next();
            results[index++] = value;
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Property that ) {
        if (this == that) return 0;
        int diff = this.getName().compareTo(that.getName());
        if (diff != 0) return diff;
        diff = this.size() - that.size();
        if (diff != 0) return diff;
        Iterator<?> thisIter = iterator();
        Iterator<?> thatIter = that.iterator();
        while (thisIter.hasNext()) { // && thatIter.hasNext()
            Object thisValue = thisIter.next();
            Object thatValue = thatIter.next();
            diff = ValueComparators.OBJECT_COMPARATOR.compare(thisValue, thatValue);
            if (diff != 0) return diff;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof Property) {
            Property that = (Property)obj;
            if (!this.getName().equals(that.getName())) return false;
            if (this.size() != that.size()) return false;
            Iterator<?> thisIter = iterator();
            Iterator<?> thatIter = that.iterator();
            while (thisIter.hasNext()) { // && thatIter.hasNext()
                Object thisValue = thisIter.next();
                Object thatValue = thatIter.next();
                if (ValueComparators.OBJECT_COMPARATOR.compare(thisValue, thatValue) != 0) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return getString(null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        return getString(null, encoder, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        return getString(namespaceRegistry, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return getString(namespaceRegistry, encoder, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder, org.modeshape.common.text.TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        StringBuilder sb = new StringBuilder();
        sb.append(getName().getString(namespaceRegistry, encoder, delimiterEncoder));
        sb.append("=");
        if (isEmpty()) {
            sb.append("null");
        } else {
            if (isMultiple()) sb.append("[");
            boolean first = true;
            for (Object value : this) {
                if (first) first = false;
                else sb.append(",");
                sb.append('"');
                if (value instanceof Path) {
                    Path path = (Path)value;
                    sb.append(path.getString(namespaceRegistry, encoder, delimiterEncoder));
                } else if (value instanceof Name) {
                    Name name = (Name)value;
                    sb.append(name.getString(namespaceRegistry, encoder, delimiterEncoder));
                } else {
                    sb.append(value);
                }
                sb.append('"');
            }
            if (isMultiple()) sb.append("]");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" = ");
        if (isSingle()) {
            sb.append(getValues().next());
        } else if (isEmpty()) {
            sb.append("null");
        } else {
            sb.append(Arrays.asList(getValuesAsArray()));
        }
        return sb.toString();
    }
}
