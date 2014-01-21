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
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;

/**
 * A {@link org.modeshape.jcr.value.Path.Segment} implementation that represents an identifier segment.
 */
@Immutable
public class IdentifierPathSegment extends BasicPathSegment {

    private static final long serialVersionUID = 1L;

    public IdentifierPathSegment( Name name ) {
        super(name);
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
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
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(this.getName().getString(encoder)).append(']');
        return sb.toString();
    }

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
