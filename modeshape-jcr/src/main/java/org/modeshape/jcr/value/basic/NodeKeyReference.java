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
package org.modeshape.jcr.value.basic;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;

/**
 * A {@link Reference} implementation that uses a single {@link NodeKey} as the pointer.
 */
@Immutable
public class NodeKeyReference implements Reference {

    private static final long serialVersionUID = 2299467578161645109L;

    private final NodeKey key;
    private final boolean isWeak;

    private boolean isNodeForeign;

    public NodeKeyReference( NodeKey key,
                             boolean weak ) {
        this.key = key;
        this.isWeak = weak;
    }

    public void setNodeForeign( boolean nodeForeign ) {
        isNodeForeign = nodeForeign;
    }

    /**
     * @return uuid
     */
    public NodeKey getNodeKey() {
        return this.key;
    }

    @Override
    public String getString() {
        return isNodeForeign ? this.key.toString() : this.key.getIdentifier();
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        return encoder.encode(getString());
    }

    @Override
    public boolean isWeak() {
        return isWeak;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo( Reference that ) {
        if (this == that) return 0;
        if (this.isWeak()) {
            if (!that.isWeak()) return -1;
        } else {
            if (that.isWeak()) return 1;
        }
        if (that instanceof NodeKeyReference) {
            return this.key.compareTo(((NodeKeyReference)that).getNodeKey());
        }
        return this.getString().compareTo(that.getString());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeKeyReference) {
            NodeKeyReference that = (NodeKeyReference)obj;
            return this.isWeak() == that.isWeak() && this.key.equals(that.getNodeKey());
        }
        if (obj instanceof Reference) {
            Reference that = (Reference)obj;
            return this.isWeak() == that.isWeak() && this.getString().equals(that.getString());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return this.key.toString();
    }

}
