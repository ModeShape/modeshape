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

import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;

/**
 * A {@link Reference} implementation that uses a single {@link UUID} as the pointer.
 */
@Immutable
public class StringReference implements Reference {

    /**
     */
    private static final long serialVersionUID = 2299467578161645109L;
    private/*final*/String id;
    private/*final*/boolean isWeak;

    public StringReference( String id ) {
        this.id = id;
        this.isWeak = false;
    }

    public StringReference( String id,
                            boolean weak ) {
        this.id = id;
        this.isWeak = weak;
    }

    @Override
    public boolean isWeak() {
        return isWeak;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isForeign() {
        return false;
    }

    @Override
    public String getString() {
        return this.id;
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        return encoder.encode(getString());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo( Reference that ) {
        if (this == that) return 0;
        if (this.isWeak()) {
            if (!that.isWeak()) return -1;
        } else {
            if (that.isWeak()) return 1;
        }
        if (that instanceof StringReference) {
            return this.id.compareTo(((StringReference)that).getString());
        }
        return this.getString().compareTo(that.getString());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Reference) {
            Reference that = (Reference)obj;
            return this.isWeak() == that.isWeak() && this.getString().equals(that.getString());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return this.id;
    }

}
