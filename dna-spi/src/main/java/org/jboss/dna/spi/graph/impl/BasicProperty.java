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
package org.jboss.dna.spi.graph.impl;

import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * @author Randall Hauch
 */
@Immutable
public abstract class BasicProperty implements Property {

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
    public int compareTo( Property o ) {
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
                if (thisValue == null) {
                    if (thatValue != null) return false;
                    // else both are null
                } else {
                    if (!thisValue.equals(thatValue)) return false;
                }
            }
            return true;
        }
        return false;
    }
}
