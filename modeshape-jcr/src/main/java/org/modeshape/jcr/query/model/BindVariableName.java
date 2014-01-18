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
 * A value bound to a variable name used in a {@link Comparison} constraint.
 */
@Immutable
public class BindVariableName implements StaticOperand, javax.jcr.query.qom.BindVariableValue {

    private static final long serialVersionUID = 1L;

    private final String variableName;

    public BindVariableName( String variableName ) {
        CheckArg.isNotEmpty(variableName, "variableName");
        this.variableName = variableName;
    }

    @Override
    public final String getBindVariableName() {
        return variableName;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof BindVariableName) {
            BindVariableName that = (BindVariableName)obj;
            return this.variableName.equals(that.variableName);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
