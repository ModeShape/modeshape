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
package org.modeshape.schematic.document;

import org.modeshape.schematic.annotation.Immutable;

/**
 * A {@link Bson.Type#JAVASCRIPT_WITH_SCOPE JavaScript code with scope} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
public final class CodeWithScope extends Code {

    private final Document scope;

    public CodeWithScope( String code,
                          Document scope ) {
        super(code);
        this.scope = scope;
        assert this.scope != null;
    }

    public Document getScope() {
        return scope;
    }

    @Override
    public int hashCode() {
        return scope.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CodeWithScope) {
            CodeWithScope that = (CodeWithScope)obj;
            return this.getCode().equals(that.getCode()) && this.getScope().equals(that.getScope());
        }
        return false;
    }

    @Override
    public String toString() {
        return "CodeWithScope (" + getCode() + ':' + getScope() + ')';
    }

}
