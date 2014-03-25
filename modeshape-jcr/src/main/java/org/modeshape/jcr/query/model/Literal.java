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
package org.modeshape.jcr.query.model;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A literal value used in a {@link Comparison} constraint.
 */
@Immutable
public class Literal implements StaticOperand {
    private static final long serialVersionUID = 1L;

    private final Object value;

    public Literal( Object value ) {
        CheckArg.isNotNull(value, "value");
        this.value = value;
    }

    /**
     * Get the literal value.
     * 
     * @return the value; never null
     */
    public final Object value() {
        return value;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return value().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Literal) {
            Literal that = (Literal)obj;
            return this.value.equals(that.value) || this.value.toString().equals(that.value.toString());
        }
        // Otherwise, check whether this literal's value is the same as the object ...
        return this.value.equals(obj);
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
