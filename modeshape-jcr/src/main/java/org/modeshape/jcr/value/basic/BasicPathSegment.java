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
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;

/**
 * A basic implementation of {@link org.modeshape.jcr.value.Path.Segment}.
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

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public Name getName() {
        return this.name;
    }

    @Override
    public boolean hasIndex() {
        return this.index != Path.DEFAULT_INDEX;
    }

    @Override
    public boolean isParentReference() {
        return this.name.getLocalName().equals(Path.PARENT) && this.name.getNamespaceUri().length() == 0;
    }

    @Override
    public boolean isSelfReference() {
        return this.name.getLocalName().equals(Path.SELF) && this.name.getNamespaceUri().length() == 0;
    }

    @Override
    public boolean isIdentifier() {
        return false;
    }

    @Override
    public int compareTo( Path.Segment that ) {
        if (this == that) return 0;
        int diff = this.getName().compareTo(that.getName());
        if (diff != 0) return diff;
        return this.getIndex() - that.getIndex();
    }

    @Override
    public int hashCode() {
        return hc;
    }

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

    @Override
    public String toString() {
        if (this.hasIndex()) {
            return this.getName().toString() + "[" + this.getIndex() + "]";
        }
        return this.getName().toString();
    }

    @Override
    public String getUnencodedString() {
        return getString(Path.NO_OP_ENCODER);
    }

    @Override
    public String getString() {
        return getString(Path.DEFAULT_ENCODER);
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        String encodedName = this.getName().getString(encoder);
        if (this.hasIndex()) {
            return encodedName + "[" + this.getIndex() + "]";
        }
        return encodedName;
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        return getString(namespaceRegistry, Path.DEFAULT_ENCODER);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        return getString(namespaceRegistry, encoder, null);
    }

    @Override
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
