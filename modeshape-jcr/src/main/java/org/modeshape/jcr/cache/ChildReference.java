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
package org.modeshape.jcr.cache;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.basic.BasicPathSegment;

/**
 * An immutable reference to a child node.
 */
@Immutable
public class ChildReference implements Comparable<ChildReference> {

    private final NodeKey key;
    private final Segment segment;

    public ChildReference( NodeKey key,
                           Name name,
                           int snsIndex ) {
        this.key = key;
        this.segment = new BasicPathSegment(name, snsIndex);
    }

    public ChildReference( NodeKey key,
                           Segment segment ) {
        this.key = key;
        this.segment = segment;
    }

    /**
     * @return key
     */
    public NodeKey getKey() {
        return key;
    }

    /**
     * @return name
     */
    public Name getName() {
        return segment.getName();
    }

    /**
     * @return snsIndex
     */
    public int getSnsIndex() {
        return segment.getIndex();
    }

    public Segment getSegment() {
        return segment;
    }

    @Override
    public int compareTo( ChildReference that ) {
        if (that == this) return 0;
        int diff = this.segment.compareTo(that.segment);
        if (diff != 0) return diff;
        return this.key.compareTo(that.key);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildReference) {
            ChildReference that = (ChildReference)obj;
            return this.segment.equals(that.segment) && this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return this.segment.getString() + " (key=" + key + ")";
    }

    public String getString( NamespaceRegistry registry ) {
        return this.segment.getString(registry);
    }

    public ChildReference with( int snsIndex ) {
        return snsIndex == segment.getIndex() ? this : new ChildReference(key, segment.getName(), snsIndex);
    }

    public ChildReference with( Name name,
                                int snsIndex ) {
        if (snsIndex == segment.getIndex() && segment.getName().equals(name)) return this;
        return new ChildReference(key, name, snsIndex);
    }

}
