/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private final boolean weak;
    private final boolean foreign;
    private final boolean simple;

    protected NodeKeyReference( NodeKey key,
                                boolean weak,
                                boolean foreign ) {
        this(key, weak, foreign, false);
    }

    protected NodeKeyReference( NodeKey key,
                                boolean weak,
                                boolean foreign,
                                boolean simple ) {
        this.key = key;
        this.weak = weak;
        this.foreign = foreign;
        this.simple = simple;
    }

    @Override
    public boolean isForeign() {
        return foreign;
    }

    @Override
    public boolean isSimple() {
        return simple;
    }

    /**
     * @return uuid
     */
    public NodeKey getNodeKey() {
        return this.key;
    }

    @Override
    public String getString() {
        return foreign ? this.key.toString() : this.key.getIdentifier();
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) {
            encoder = Path.DEFAULT_ENCODER;
        }
        return encoder.encode(getString());
    }

    @Override
    public boolean isWeak() {
        return weak;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo( Reference that ) {
        if (this == that) {
            return 0;
        }
        if (this.isWeak()) {
            if (!that.isWeak()) {
                return -1;
            }
        } else {
            if (that.isWeak()) {
                return 1;
            }
        }
        if (that instanceof NodeKeyReference) {
            NodeKeyReference thatNodeKeyReference = (NodeKeyReference)that;
            if (this.foreign && !thatNodeKeyReference.foreign) {
                return 1;
            }
            if (!this.foreign && thatNodeKeyReference.foreign) {
                return -1;
            }
            if (this.simple && !thatNodeKeyReference.simple) {
                return 1;
            }
            if (!this.simple && thatNodeKeyReference.simple) {
                return -1;
            }
            return this.key.compareTo(thatNodeKeyReference.getNodeKey());
        }
        return this.getString().compareTo(that.getString());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NodeKeyReference) {
            NodeKeyReference that = (NodeKeyReference)obj;
            return this.isWeak() == that.isWeak() && this.key.equals(that.getNodeKey()) && foreign == that.foreign && simple == that.simple;
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
