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
package org.infinispan.schematic.internal.schema;

import org.infinispan.schematic.SchemaLibrary.Problem;
import org.infinispan.schematic.SchemaLibrary.ProblemType;
import org.infinispan.schematic.document.Path;

public class ValidationProblem implements Problem {
    private final ProblemType type;
    private final Path path;
    private final String reason;
    private final Throwable cause;

    public ValidationProblem( ProblemType type,
                              Path path,
                              String reason,
                              Throwable cause ) {
        this.type = type;
        this.path = path;
        this.reason = reason;
        this.cause = cause;
    }

    @Override
    public ProblemType getType() {
        return type;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "" + type + " at " + path + ": " + reason;
    }
}
