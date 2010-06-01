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
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * A {@link Path.Segment} implementation that represents an identifier segment.
 */
@Immutable
public class IdentifierPathSegment extends BasicPathSegment {

    private static final long serialVersionUID = 1L;

    public IdentifierPathSegment( Name name ) {
        super(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path.Segment#isIdentifier()
     */
    @Override
    public boolean isIdentifier() {
        return true;
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
    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(this.getName().getString(encoder)).append(']');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path.Segment#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder, org.modeshape.common.text.TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(this.getName().getString(namespaceRegistry, encoder, delimiterEncoder)).append(']');
        return sb.toString();
    }
}
