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
package org.modeshape.schematic.internal.schema;

import org.modeshape.schematic.SchemaLibrary;
import org.modeshape.schematic.document.JsonSchema.Type;
import org.modeshape.schematic.document.Path;

public final class ValidationTypeMismatchProblem extends ValidationProblem implements SchemaLibrary.MismatchedTypeProblem {
    private final Object actualValue;
    private final Object convertedValue;
    private final Type actualType;
    private final Type expectedType;

    public ValidationTypeMismatchProblem( SchemaLibrary.ProblemType type,
                                          Path path,
                                          Object actualValue,
                                          Type actualType,
                                          Type expectedType,
                                          Object convertedValue,
                                          String reason,
                                          Throwable cause ) {
        super(type, path, reason, cause);
        this.actualValue = actualValue;
        this.convertedValue = convertedValue;
        this.actualType = actualType;
        this.expectedType = expectedType;
    }

    @Override
    public Object getActualValue() {
        return actualValue;
    }

    @Override
    public Object getConvertedValue() {
        return convertedValue;
    }

    @Override
    public Type getActualType() {
        return actualType;
    }

    @Override
    public Type getExpectedType() {
        return expectedType;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
