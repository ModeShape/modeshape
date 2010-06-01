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

import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * A basic implementation of {@link Path.Segment}.
 */
@Immutable
public class BasicPathSegment implements Path.Segment {

    /**
     */
    private static final long serialVersionUID = 4367349287846075157L;
    private final Name name;
    private final int index;
    private final int hc;

    /**
     * @param name the segment name
     * @throws IllegalArgumentException if the name is null or if the index is invalid
     */
    public BasicPathSegment( Name name ) {
        assert name != null;
        this.name = name;
        this.index = Path.DEFAULT_INDEX;
        hc = HashCode.compute(name, index);
    }

    /**
     * @param name the segment name
     * @param index the segment index
     * @throws IllegalArgumentException if the name is null or if the index is invalid
     */
    public BasicPathSegment( Name name,
                             int index ) {
        assert name != null;
        assert index >= Path.DEFAULT_INDEX;
        this.name = name;
        this.index = (this.isSelfReference() || this.isParentReference()) ? Path.DEFAULT_INDEX : index;
        hc = HashCode.compute(name, index);
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasIndex() {
        return this.index != Path.DEFAULT_INDEX;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isParentReference() {
        return this.name.getLocalName().equals(Path.PARENT) && this.name.getNamespaceUri().length() == 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelfReference() {
        return this.name.getLocalName().equals(Path.SELF) && this.name.getNamespaceUri().length() == 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path.Segment#isIdentifier()
     */
    public boolean isIdentifier() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Path.Segment that ) {
        if (this == that) return 0;
        int diff = this.getName().compareTo(that.getName());
        if (diff != 0) return diff;
        return this.getIndex() - that.getIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return hc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path.Segment) {
            Path.Segment that = (Path.Segment)obj;
            if (!this.getName().equals(that.getName())) return false;
            return Math.abs(getIndex()) == Math.abs(that.getIndex());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this.hasIndex()) {
            return this.getName().toString() + "[" + this.getIndex() + "]";
        }
        return this.getName().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getUnencodedString() {
        return getString(Path.NO_OP_ENCODER);
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return getString(Path.DEFAULT_ENCODER);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        String encodedName = this.getName().getString(encoder);
        if (this.hasIndex()) {
            return encodedName + "[" + this.getIndex() + "]";
        }
        return encodedName;
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        return getString(namespaceRegistry, Path.DEFAULT_ENCODER);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        return getString(namespaceRegistry, encoder, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path.Segment#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder, org.modeshape.common.text.TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        String encodedName = this.getName().getString(namespaceRegistry, encoder, delimiterEncoder);
        if (this.hasIndex()) {
            return encodedName + "[" + this.getIndex() + "]";
        }
        return encodedName;
    }
}
